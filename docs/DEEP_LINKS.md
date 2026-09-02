# Deep Links Reference

Everything this project knows about the deep-link formats for Uber, Uber Eats, Bolt (rides), Bolt
Food, and Glovo, gathered while debugging the "Bolt opens with no destination set" issue (see PRs
#63, #79, #80, #81) and while adding Glovo as a third food delivery option (see the Comparison
configuration section in Settings, and `ComparisonConfigRepository`). Keep this updated as new
evidence comes in - it's the source of truth for the next person (or the next AI session) working
on any of these integrations.

## Confidence legend

Every format below is tagged so it's clear how much to trust it:

- ✅ **OFFICIAL** - published by the provider's own developer docs or SDK source. Safe to rely on.
- 🔎 **VERIFIED HOST** - not published anywhere, but confirmed by inspecting the real app's own
  shipped `AndroidManifest.xml` (an `autoVerify="true"` intent-filter proves the app really
  listens on that scheme/host). The *host* is trustworthy; the *query parameters* below it are
  still a guess unless separately marked otherwise.
- ❓ **UNVERIFIED GUESS** - not confirmed by any source. Needs on-device testing before trusting it.

---

## 1. Uber (Rides) — ✅ OFFICIAL

**Source**: [developer.uber.com/docs/deep-linking](https://developer.uber.com/docs/deep-linking),
[.../riders/ride-requests/tutorials/deep-links/introduction](https://developer.uber.com/docs/riders/ride-requests/tutorials/deep-links/introduction),
and the official SDK source: [`RideRequestDeeplink.java`](https://github.com/uber/rides-android-sdk/blob/main/rides-android/src/main/java/com/uber/sdk/android/rides/RideRequestDeeplink.java)
(`AUTHORITY = "riderequest"`).

**Native scheme**:
```
uber://riderequest
  ?action=setPickup
  &client_id=<CLIENT_ID>
  &pickup[latitude]=<LAT>&pickup[longitude]=<LNG>
  &pickup[nickname]=<NAME>&pickup[formatted_address]=<ADDRESS>
  &dropoff[latitude]=<LAT>&dropoff[longitude]=<LNG>
  &dropoff[nickname]=<NAME>&dropoff[formatted_address]=<ADDRESS>
  &product_id=<PRODUCT_ID>          (optional; only takes effect if pickup is also set)
```
- **Host is `riderequest`**, not blank. `pickup=my_location` is a documented shorthand for "use
  device location" instead of explicit lat/long.
- Latitude and longitude are documented as **required** for each location; you must *also* supply
  either `nickname` or `formatted_address` for it to actually populate in-app.
- `client_id` is documented as required for the native scheme (used for install/signup
  attribution) though the app appears to tolerate its absence in practice.

**Universal/web link**: same query params on `https://m.uber.com/ul/` (e.g.
`https://m.uber.com/ul/?action=setPickup&pickup[latitude]=...&client_id=...`). Falls back to the
mobile web / sign-up flow (`https://m.uber.com/?client_id=<CLIENT_ID>`) if the app isn't installed.

**⚠️ Gap vs. this app's current implementation** - `createUberDeepLink()` in `MainViewModel.kt`
currently builds `uber://?action=setPickup&pickup[formatted_address]=...&dropoff[formatted_address]=...`:
it omits the `riderequest` host, omits `client_id`, and never sends lat/long (only the formatted
address text) even though the docs list lat/long as required. This hasn't been reported as broken
by the user, so the Uber app may tolerate the missing host/lat-long in practice, but it doesn't
match the documented contract - worth fixing to the documented format in a follow-up, and testing
on-device before trusting it blindly (same lesson as the Bolt saga).

There's also a documented `applyPromo` action at
[developer.uber.com/docs/rides/deep-linking-action-applypromo](https://developer.uber.com/docs/rides/deep-linking-action-applypromo),
same dual native/web pattern, with `client_id` + `promo_code`.

---

## 2. Uber Eats — ❓ mostly UNOFFICIAL (no published deep-link docs)

**Source**: no official documentation found. `developer.uber.com/docs/eats/*` only covers the
Eats Marketplace API (order management / POS integration for restaurant partners), not consumer
deep links. There is no published `ubereats://` scheme spec.

**Package name**: `com.ubercab.eats` (confirmed Play Store listing package).

**Observed (unofficial) website patterns** - not guaranteed stable, no lat/long or place param
confirmed to work via deep link:
```
https://www.ubereats.com/search?q=<QUERY>          (search page)
https://www.ubereats.com/store/<slug>/<store-uuid>  (a specific restaurant's page)
```
Store UUIDs are only obtainable by browsing the live site; there's no public lookup API for them,
so "open the same restaurant across apps" isn't currently feasible without one.

**What this app implements** (`FoodDeepLinks.createUberEatsSearchLink()`): the search URL above,
targeted at the app's package explicitly so it opens in-app rather than a browser tab. Whether
Uber Eats actually pre-fills the search box from `q` hasn't been confirmed on-device.

---

## 3. Bolt (Rides) — `ee.mtakso.client` — 🔎 VERIFIED HOST, ❓ query params still guessed

**Source**: the app's own shipped `AndroidManifest.xml` (`versionName="CA.210.0"`,
`versionCode="4180"`), shared during this task. No official Bolt developer documentation exists
for the rider-facing app (only driver/fleet APIs are public).

**`DeeplinkActivity` (`ee.mtakso.client.newbase.deeplink.DeeplinkActivity`) intent filters**:

| Scheme(s) | Host | Path | `autoVerify` | Purpose |
|---|---|---|---|---|
| `bolt`, `taxify` | `action` | — | ✅ **true** | **The real deep-link entry point.** |
| `https` | `scooters.taxify.eu` | — | ✅ true | Scooter-specific web links |
| `boltprelive`, `bolt`, `taxify` | `action` | (NFC only) | — | NFC tag discovery |
| `https` | `scooters.taxify.eu` | `/qr` | — | NFC QR variant |
| `bolt`, `taxify`, `geo` | *(none - catches all hosts)* | — | — | **Generic catch-all** - opens the app but reaches no host-specific routing |
| `https`, `http` | `maps.google.com` | — | ✅ true | Receiving a shared Google Maps location |

**`AttributionDeeplinkActivity` intent filter** (separate activity, install attribution only):

| Scheme(s) | Host | Path prefix | `autoVerify` |
|---|---|---|---|
| `http`, `https` | `bolt.sng.link` | `/A`, `/B`, `/E`, `/F` | ✅ true |

**Key findings**:
- `bolt://action` (and `taxify://action`) is the **only** verified, specifically-routed host.
  Every other host - including the `bolt://ride` this app used from PR #63 through #81's earlier
  commits - only matches the generic catch-all filter, which opens the app but hits no
  host-specific logic. This is the root cause of the "Bolt opens, destination never sets" bug.
- **`bolt.eu` is not declared anywhere in this manifest** - not as a scheme+host, not
  `autoVerify`, not at all. `https://bolt.eu/ride/...` (this app's old HTTPS fallback) can never
  resolve inside the Bolt app itself; it always falls through to a browser tab.
- The manifest confirms *routing*, not the *query-param contract* `DeeplinkActivity` reads once
  it's on the `action` host - that part is still genuinely unknown.

**What this app implements** (`MainViewModel.createBoltDeepLink()` in `MainViewModel.kt`):
```
bolt://action?pickup_lat=<LAT>&pickup_lng=<LNG>&destination_lat=<LAT>&destination_lng=<LNG>
```
This corrects the host (🔎 verified) but the param names (`pickup_lat`, `destination_lat`, ...)
are ❓ **still an unverified guess**.

**Candidates to test on-device** (`BoltDeepLinkCandidates.kt`, exercised via the in-app
**Bolt Link Lab** at Settings → Diagnostics):

| id | Format | Confidence |
|---|---|---|
| 1 | `bolt://action?pickup_lat=..&pickup_lng=..&destination_lat=..&destination_lng=..` | 🔎 host verified, params guessed (current app behavior) |
| 2 | `bolt://action?pickup_lat=..&pickup_lng=..&dropoff_lat=..&dropoff_lng=..` | ❓ |
| 3 | `bolt://action?action_type=order_ride&pickup_lat=..&...` | ❓ |
| 4 | `bolt://action?type=ride&pickup_lat=..&...` | ❓ |
| 5 | `bolt://action?pickup[lat]=..&dropoff[lat]=..` (bracket-style) | ❓ |
| 6 | `bolt://action?pickup=<lat>,<lng>&dropoff=<lat>,<lng>` (comma-joined) | ❓ |
| 7 | `bolt://ride?pickup_lat=..&destination_lat=..` | Known non-functional (`ride` host isn't routed) - kept only as a baseline for comparison |

Coordinates are formatted with exactly 6 decimal places (`String.format(Locale.US, "%.6f", ...)`),
period as the decimal separator - this detail predates the host fix and has never been in
question.

**Package name**: `ee.mtakso.client`.

---

## 4. Bolt Food — `com.bolt.deliveryclient` — 🔎 VERIFIED HOST for `food.bolt.eu`

**Source**: the app's own shipped `AndroidManifest.xml` (`versionName="1.117.0"`,
`versionCode="3254809"`), shared during this task. No official Bolt Food developer documentation
exists (only third-party scraping services show up when searching for one).

**`MainActivity` (`com.bolt.deliveryclient.MainActivity`) intent filters**:

| Scheme(s) | Host | Path | `autoVerify` | Purpose |
|---|---|---|---|---|
| `boltfood`, `exp+boltfood` | *(none)* | — | — | Basic custom-scheme open (no path routing declared) |
| `boltfood` | *(none)* | — | ✅ true | Same scheme, separately declared verified |
| `https` | `boltfood.onelink.me` | — | ✅ true | AppsFlyer OneLink attribution |
| `http`, `https` | `boltfood.sng.link` | `/A`, `/B`, `/E`, `/F` | ✅ true | Singular attribution |
| `https` | `food.bolt.eu` | `/` (any path), `/search`, `/*/search`, `/*/p/*`, `/*/*/p/*`, `/*/*/*/p/*`, `/*/hc/*`, `/*/*/hc/*`, `/dine-in` | ✅ true | **The real web deep-link surface** - search, product/restaurant pages (`/p/`), help center (`/hc/`), dine-in |

**Key findings**:
- `https://food.bolt.eu/...` is a genuinely verified App Link with a declared `/search` path and
  `/p/` (product/restaurant) path pattern - much stronger footing than anything in the Bolt rides
  manifest ever had. This is the one link in this whole document that's closest to "provider
  confirmed the surface exists," short of them publishing param docs.
- The `/p/` path pattern (`/.*/p/.*`, nested under locale/city segments) strongly suggests
  restaurant/product pages are addressed by ID within that path, not a query param - i.e. a real
  Bolt Food restaurant URL likely looks like `https://food.bolt.eu/en-us/6/p/12345-restaurant-name`
  rather than `?restaurant_id=12345`. This app doesn't currently attempt restaurant-specific
  linking (see "same restaurant" discussion below), only search.
- The bare `boltfood://` scheme has no host/path constraint declared, so there's no way to tell
  from the manifest alone whether it accepts any deep-link parameters at all versus just opening
  the app.

**What this app implements** (`FoodDeepLinks.createBoltFoodSearchLink()` in `FoodDeepLinks.kt`):
```
https://food.bolt.eu/search?q=<QUERY>
```
This matches the verified `/search` path (🔎), but whether the `q` param is actually read is ❓
still unconfirmed on-device.

**Package name**: `com.bolt.deliveryclient`.

---

## 5. Glovo — `com.glovo` — ❓ UNVERIFIED GUESS

**Source**: no official documentation found (same situation as Uber Eats). No shipped
`AndroidManifest.xml` was available to inspect for this one either (unlike Bolt/Bolt Food, where a
real manifest was shared during those tasks) - both the network fetches attempted while researching
this and a manifest teardown were unavailable in that session's environment, so *nothing* below the
package name is host/param-verified. This is the least-confident entry in this document; treat it
as a starting point for on-device testing, not a trustworthy format.

**Package name**: `com.glovo` (confirmed via web search against the Play Store listing for "Glovo:
Food & Grocery Delivery" - note this is distinct from the separate courier/partner apps
`com.logistics.rider.glovo` and `com.deliveryhero.glovopartner`, and from `com.glovoapp23`, which
came up while researching this but does not appear to be the current consumer app's id).

**Why it's shakier than Uber Eats/Bolt Food**: both of those have a flat, locale-independent
`https://<host>/search?q=<query>` page. Glovo's web app is locale/city-scoped instead
(`https://glovoapp.com/<lang>/<country>/...`, e.g. `.../en/es/map/cities`) - there's no confirmed
flat search URL. A `links.glovoapp.com` domain exists (likely a dynamic-link/attribution host,
similar to Bolt's `*.sng.link` App Links) but its host/path contract wasn't reachable to inspect.

**What this app implements** (`FoodDeepLinks.createSearchLink()` for `FoodDeliveryProvider.GLOVO`
in `FoodDeepLinks.kt`):
```
https://glovoapp.com/search/?query=<QUERY>
```
This guesses that, launched from inside the already-installed app via an explicit-package intent
(same trick used for the other two providers), Glovo's own in-app session already knows the user's
city/locale, so the URL's lack of a locale/country path segment may not matter the way it would for
a plain browser visit - but this is **unconfirmed**. If the app doesn't handle this path at all, the
explicit-package intent throws and `MainActivity.openLinkWithAppFallback()` falls back to a browser
tab (same behavior as the other two providers), so the worst case is "opens a browser to a page that
doesn't resolve either" rather than a crash.

---

## Where this lives in the codebase

| Concern | File |
|---|---|
| Uber ride link builder | `app/src/main/java/org/neteinstein/compareapp/ui/screens/MainViewModel.kt` (`createUberDeepLink`) |
| Bolt ride link builder | same file (`createBoltDeepLink`, `createBoltDeepLinkWeb`) |
| Bolt ride candidate formats (for on-device testing) | `app/src/main/java/org/neteinstein/compareapp/utils/BoltDeepLinkCandidates.kt` |
| Bolt Link Lab UI (Settings → Diagnostics, tap the title 10 times to reveal) | `app/src/main/java/org/neteinstein/compareapp/ui/screens/BoltLinkLabScreen.kt` / `BoltLinkLabViewModel.kt` |
| Food providers (package names, display names) | `app/src/main/java/org/neteinstein/compareapp/utils/FoodDeliveryProvider.kt` |
| Food search link builders (Uber Eats / Bolt Food / Glovo) | `app/src/main/java/org/neteinstein/compareapp/utils/FoodDeepLinks.kt` |
| Which 2 of 3 food providers are active (Settings → Comparison configuration) | `app/src/main/java/org/neteinstein/compareapp/data/repository/ComparisonConfigRepository.kt` / `ComparisonConfigRepositoryImpl.kt` |
| Launch mechanics (native → web/browser fallback, explicit package targeting) | `app/src/main/java/org/neteinstein/compareapp/MainActivity.kt` (`launchBoltWithFallback`, `openBoltWebLink`, `openLinkWithAppFallback`, `openFoodSearch`) |

## Open items / suggested next steps

1. **Bolt rides**: use the Bolt Link Lab on a real device with Bolt installed to find which of the
   `bolt://action?...` param candidates actually sets the destination, then promote it into
   `createBoltDeepLink()` and delete the rest.
2. **Uber rides**: bring `createUberDeepLink()` in line with the documented format above
   (`riderequest` host, lat/long, `client_id`) and confirm on-device that it still works - it may
   already be silently relying on Uber's app being lenient about the missing host.
3. **Bolt Food**: confirm on-device whether `https://food.bolt.eu/search?q=...` actually pre-fills
   the search box; if not, try encoding the query differently or explore whether the `/p/` path
   pattern can be used for a specific restaurant once/if a restaurant-ID lookup becomes available.
4. **Uber Eats**: no verified path exists; the search link is the best available guess. Revisit if
   Uber ever publishes Eats deep-link docs.
5. **Glovo**: highest priority follow-up of the three food providers - get a device with Glovo
   installed (or the shipped APK's `AndroidManifest.xml`) and confirm whether `com.glovo` declares
   any App Link host at all, and whether `glovoapp.com/search/?query=` (or any URL) actually opens
   the app rather than falling back to browser. Right now this is a guess with no host verification,
   unlike Bolt Food's confirmed `/search` App Link.
6. **"Same restaurant" across food providers**: not implemented. None of the three platforms expose
   a shared restaurant identifier or a public search API, so matching would require an extra
   search-and-compare step (fuzzy match by name + location) rather than a direct deep link.
