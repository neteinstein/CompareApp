# Deployment Guide - Google Play Store

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Initial Play Console Setup](#initial-play-console-setup)
4. [Keystore Management](#keystore-management)
5. [Service Account Setup](#service-account-setup)
6. [GitHub Secrets Configuration](#github-secrets-configuration)
7. [Deployment Process](#deployment-process)
8. [Release Tracks](#release-tracks)
9. [Staged Rollouts](#staged-rollouts)
10. [Troubleshooting](#troubleshooting)
11. [Post-Deployment](#post-deployment)
12. [Best Practices](#best-practices)

## Overview

This guide provides step-by-step instructions for deploying CompareApp to the Google Play Store. The deployment process is automated via GitHub Actions, but requires initial setup of credentials and configurations.

## Prerequisites

### Required Accounts

- ✅ Google Play Console account ([$25 one-time fee](https://play.google.com/console/signup))
- ✅ Google Cloud Platform account (free tier available)
- ✅ GitHub account with repository access
- ✅ Android keystore file (for signing APKs)

### Required Tools

- ✅ Java JDK 17+ (for keystore creation)
- ✅ Git (for version control)
- ✅ Text editor (for editing secrets)

### Knowledge Requirements

- Basic understanding of Android app deployment
- Familiarity with GitHub Actions
- Basic command-line skills

## Initial Play Console Setup

### Step 1: Create Play Console Account

1. Go to [Google Play Console](https://play.google.com/console/signup)
2. Sign in with your Google account
3. Accept the Developer Agreement
4. Pay the $25 registration fee
5. Complete your account details

### Step 2: Create a New App

1. Click **"Create app"** in Play Console
2. Fill in app details:
   - **App name**: CompareApp (or your preferred name)
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free
3. Review declarations:
   - ✅ Accept Play Developer Program Policies
   - ✅ Confirm US export laws compliance
4. Click **"Create app"**

### Step 3: Complete Store Listing

Navigate to **"Store presence" → "Main store listing"** and fill in:

#### App Details

**Short description** (80 characters max):
```
Compare Uber and Bolt prices side-by-side instantly
```

**Full description** (4000 characters max):
```
CompareApp makes choosing the best ride-sharing service quick and easy. Simply enter your pickup and dropoff locations, tap Compare, and both Uber and Bolt will open side-by-side so you can see prices and features at a glance.

FEATURES
• Split Screen Comparison - View Uber and Bolt simultaneously
• Smart Location Entry - Type addresses or use current location
• Instant Price Comparison - See both apps with one tap
• Modern Design - Clean, intuitive Material Design interface

HOW IT WORKS
1. Enter your pickup location
2. Enter your dropoff location
3. Tap the Compare button
4. Compare prices and choose the best option

REQUIREMENTS
• Android 7.0 or higher
• Uber app installed
• Bolt app installed

Save time and money by comparing ride-sharing options before you book!
```

**App icon**:
- Size: 512 x 512 px
- Format: PNG (32-bit)
- No transparency

**Feature graphic**:
- Size: 1024 x 500 px
- Format: PNG or JPEG

**Screenshots** (minimum 2, up to 8):
- Size: 16:9 or 9:16 aspect ratio
- Min dimension: 320 px
- Max dimension: 3840 px
- Format: PNG or JPEG

#### Categorization

- **App category**: Tools
- **Tags**: travel, transportation, price comparison

#### Contact Details

- **Email**: your-email@example.com
- **Phone**: (optional)
- **Website**: https://github.com/neteinstein/CompareUberVsBoltPriceApp

### Step 4: Content Rating

1. Navigate to **"Policy" → "App content"**
2. Click **"Start questionnaire"** under Content rating
3. Enter your email address
4. Select category: **Utility, Productivity, Communication, or Other**
5. Answer questionnaire questions:
   - Violence: No
   - Sexual content: No
   - Profanity: No
   - Controlled substances: No
   - Gambling: No
   - User-generated content: No
6. Save and submit

### Step 5: Privacy Policy

1. Create a privacy policy (required for apps that access location data)
2. Host it on a public URL (GitHub Pages, your website, etc.)
3. In Play Console: **"Policy" → "Privacy Policy"**
4. Enter your privacy policy URL

**Sample Privacy Policy Content**:
```markdown
# Privacy Policy for CompareApp

Last updated: [Date]

## Information Collection
CompareApp does not collect, store, or transmit any personal information. 
All location data is processed locally on your device and is only used to 
create deep links to Uber and Bolt apps.

## Location Data
The app may request location permission to auto-fill your pickup location. 
This data is never stored or transmitted to our servers.

## Third-Party Apps
When you use CompareApp to open Uber or Bolt, you are subject to those 
apps' respective privacy policies.

## Contact
For questions about this privacy policy, contact: your-email@example.com
```

### Step 6: Data Safety

Navigate to **"Policy" → "Data safety"** and declare:

**Data collection**:
- Location: Yes (approximate and precise)
  - Purpose: App functionality
  - Collection method: Required
  - Not shared with third parties
  - Not collected (only used, not stored)

**Security practices**:
- Data is encrypted in transit: Yes
- Users can request data deletion: N/A (no data stored)
- Committed to Google Play Families Policy: No

## Keystore Management

### Creating a New Keystore

**Important**: Keep this file secure and backed up. If you lose it, you cannot update your app.

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias compareapp \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Prompts**:
- **Keystore password**: Choose a strong password (save it!)
- **Key password**: Choose a strong password (can be same as keystore)
- **First and last name**: Your name or company name
- **Organizational unit**: Your team/department
- **Organization**: Your company
- **City**: Your city
- **State**: Your state/province
- **Country code**: Two-letter country code (e.g., US)

**Example**:
```
Enter keystore password: MySecurePassword123!
Re-enter new password: MySecurePassword123!
What is your first and last name?
  [Unknown]:  John Doe
What is the name of your organizational unit?
  [Unknown]:  Development
What is the name of your organization?
  [Unknown]:  CompareApp Inc
What is the name of your City or Locality?
  [Unknown]:  San Francisco
What is the name of your State or Province?
  [Unknown]:  California
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=John Doe, OU=Development, O=CompareApp Inc, L=San Francisco, ST=California, C=US correct?
  [no]:  yes

Enter key password for <compareapp>
  (RETURN if same as keystore password):
```

**Output**: `release.keystore` file

### Keystore Security Best Practices

1. **Backup**: Store keystore in multiple secure locations
   - Encrypted cloud storage (Google Drive with encryption)
   - Password manager (1Password, LastPass)
   - Hardware security key
   - Encrypted USB drive in safe

2. **Never**:
   - Commit keystore to Git
   - Email keystore
   - Store keystore unencrypted
   - Share keystore password in plain text

3. **Password Management**:
   - Use a password manager
   - Choose strong, unique passwords
   - Never reuse passwords
   - Document passwords securely

### Encoding Keystore for GitHub

**Linux/macOS**:
```bash
base64 release.keystore | tr -d '\n' > keystore.base64.txt
cat keystore.base64.txt
```

**Windows PowerShell**:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -FilePath keystore.base64.txt -NoNewline
Get-Content keystore.base64.txt
```

**Windows Command Prompt**:
```cmd
certutil -encode release.keystore keystore.base64.txt
```
(Then manually remove header/footer lines)

**Output**: Long base64 string (copy this for GitHub secret)

## Service Account Setup

Service accounts allow GitHub Actions to publish to Play Store without manual intervention.

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **"Select a project"** → **"New Project"**
3. **Project name**: CompareApp-PlayStore
4. Click **"Create"**
5. Wait for project creation (30-60 seconds)

### Step 2: Enable Play Developer API

1. In Cloud Console, go to **"APIs & Services" → "Library"**
2. Search for **"Google Play Android Developer API"**
3. Click on it
4. Click **"Enable"**
5. Wait for API to enable (10-30 seconds)

### Step 3: Create Service Account

1. Go to **"IAM & Admin" → "Service Accounts"**
2. Click **"Create Service Account"**
3. **Service account name**: `compareapp-publisher`
4. **Service account ID**: `compareapp-publisher` (auto-generated)
5. **Description**: "GitHub Actions service account for Play Store publishing"
6. Click **"Create and Continue"**
7. **Role**: Skip this step (permissions granted in Play Console)
8. Click **"Continue"** then **"Done"**

### Step 4: Create Service Account Key

1. Find your service account in the list
2. Click on it to view details
3. Go to **"Keys"** tab
4. Click **"Add Key" → "Create new key"**
5. **Key type**: JSON
6. Click **"Create"**
7. JSON file downloads automatically
8. **Save this file securely** (you'll need it for GitHub)

**JSON file structure**:
```json
{
  "type": "service_account",
  "project_id": "compareapp-playstore",
  "private_key_id": "abc123...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "compareapp-publisher@compareapp-playstore.iam.gserviceaccount.com",
  "client_id": "123456789...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  ...
}
```

### Step 5: Link Service Account to Play Console

1. Open [Google Play Console](https://play.google.com/console/)
2. Go to **"Setup" → "API access"**
3. Click **"Link a project"**
4. Select your Cloud project: **CompareApp-PlayStore**
5. Click **"Link"**
6. Under "Service accounts", find `compareapp-publisher`
7. Click **"Grant access"**
8. **Account permissions**:
   - ✅ View app information and download bulk reports (read-only)
   - ✅ Create, edit, and delete draft apps
9. **App permissions**:
   - Select **"CompareApp"**
   - ✅ Create and edit releases to testing tracks
   - ✅ Release apps to testing tracks
   - ✅ Release apps to production
10. Click **"Invite user"**
11. Click **"Send invite"**

**Verification**: Service account should show "Active" status

## GitHub Secrets Configuration

### Accessing GitHub Secrets

1. Go to your repository on GitHub
2. Click **"Settings"** tab
3. Navigate to **"Secrets and variables" → "Actions"**
4. Click **"New repository secret"**

### Required Secrets

Add each of these secrets:

#### 1. KEYSTORE_FILE

- **Name**: `KEYSTORE_FILE`
- **Value**: Contents of `keystore.base64.txt` (the base64-encoded keystore)
- **Example**: `MIIJKgIBAzCCCOYGCSqGSIb3DQE...` (very long string)

**Steps**:
1. Open `keystore.base64.txt` in a text editor
2. Copy the **entire** contents (may be 2000+ characters)
3. Paste into GitHub secret value field
4. Click **"Add secret"**

#### 2. KEYSTORE_PASSWORD

- **Name**: `KEYSTORE_PASSWORD`
- **Value**: The password you used when creating the keystore
- **Example**: `MySecurePassword123!`

#### 3. KEY_ALIAS

- **Name**: `KEY_ALIAS`
- **Value**: The alias you used when creating the keystore
- **Example**: `compareapp`

#### 4. KEY_PASSWORD

- **Name**: `KEY_PASSWORD`
- **Value**: The key password (often same as keystore password)
- **Example**: `MySecurePassword123!`

#### 5. PLAY_STORE_SERVICE_ACCOUNT_JSON

- **Name**: `PLAY_STORE_SERVICE_ACCOUNT_JSON`
- **Value**: **Entire contents** of the service account JSON file

**Steps**:
1. Open the downloaded service account JSON file
2. Copy the **entire file** contents (including `{` and `}`)
3. Paste into GitHub secret value field
4. Click **"Add secret"**

**Important**: 
- Must be valid JSON
- Include all curly braces
- No extra whitespace at start/end
- Should be ~2500 characters

### Verify Secrets

After adding all secrets, you should see:

```
KEYSTORE_FILE               Updated now by you
KEYSTORE_PASSWORD           Updated now by you
KEY_ALIAS                   Updated now by you
KEY_PASSWORD                Updated now by you
PLAY_STORE_SERVICE_ACCOUNT_JSON   Updated now by you
```

## Deployment Process

### First Release (Manual Upload Required)

**Google Play requires the first APK to be manually uploaded.**

#### Step 1: Build Signed APK Locally

```bash
# Set environment variables
export KEYSTORE_FILE="/path/to/release.keystore"
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="compareapp"
export KEY_PASSWORD="your_key_password"

# Build release APK
./gradlew assembleRelease

# APK location:
# app/build/outputs/apk/release/app-release.apk
```

#### Step 2: Manual Upload to Play Console

1. Go to Play Console → **"Release" → "Testing" → "Internal testing"**
2. Click **"Create new release"**
3. Upload APK:
   - Click **"Upload"**
   - Select `app-release.apk`
   - Wait for upload to complete
4. **Release name**: Version 1.0 (Build 1)
5. **Release notes** (optional):
   ```
   Initial release
   - Compare Uber and Bolt prices side-by-side
   - Modern Material Design interface
   - Smart location entry
   ```
6. Click **"Review release"**
7. Click **"Start rollout to Internal testing"**
8. Click **"Rollout"**

**First release is now live on internal track!**

### Subsequent Releases (Automated)

After the first manual upload, all subsequent releases use GitHub Actions.

#### Step 1: Update Version

Edit `app/build.gradle`:

```gradle
android {
    defaultConfig {
        versionCode 2  // Increment by 1
        versionName "1.1"  // Update as needed
    }
}
```

Commit and push:
```bash
git add app/build.gradle
git commit -m "Bump version to 1.1 (build 2)"
git push
```

#### Step 2: Trigger Deployment Workflow

1. Go to GitHub repository → **"Actions"** tab
2. Select **"Deploy to Play Store"** workflow
3. Click **"Run workflow"**
4. Configure inputs:
   - **track**: Select release track (internal/alpha/beta/production)
   - **inAppUpdatePriority**: 0-5 (default: 2)
   - **userFraction**: 0.0-1.0 (default: 1.0)
5. Click **"Run workflow"**

#### Step 3: Monitor Workflow

1. Click on the running workflow
2. Watch progress in real-time
3. Check for errors in log output

**Successful deployment log**:
```
✓ APK built and signed
✓ APK verified
✓ Uploaded to Play Store (internal track)
✓ Release status: completed
✓ User fraction: 100%
```

#### Step 4: Verify in Play Console

1. Go to Play Console
2. Navigate to selected track (internal/alpha/beta/production)
3. Verify new version is listed
4. Check release status

## Release Tracks

### Internal Testing

**Purpose**: Small team testing (up to 100 testers)

**Use Cases**:
- Initial testing
- QA team validation
- Quick iterations

**Deployment Time**: Minutes to hours

**How to Access**:
1. Add testers via email in Play Console
2. Testers get email with opt-in link
3. Testers can install from Play Store

### Alpha Testing

**Purpose**: Broader testing (custom tester list)

**Use Cases**:
- Early adopters
- Beta testers
- Feedback gathering

**Deployment Time**: Hours

**How to Access**: Same as internal testing

### Beta Testing

**Purpose**: Public or closed testing

**Use Cases**:
- Pre-release testing
- Soft launch in specific countries
- Gather public feedback

**Options**:
- **Closed**: Email list (up to 10,000 testers)
- **Open**: Anyone with link can join

**Deployment Time**: Hours

### Production

**Purpose**: Public release to all users

**Use Cases**:
- Official launch
- General availability

**Deployment Time**: Hours to days (review may be required)

**Staged Rollout**: Recommended (10% → 25% → 50% → 100%)

### Track Promotion

Promote releases between tracks in Play Console:

```
Internal → Alpha → Beta → Production
   ↓         ↓       ↓         ↓
 Minutes   Hours   Hours    Hours-Days
```

**How to Promote**:
1. Go to source track (e.g., Beta)
2. Click **"Promote release"**
3. Select target track (e.g., Production)
4. Configure rollout
5. Click **"Promote"**

## Staged Rollouts

### What is a Staged Rollout?

Release app to a percentage of users, gradually increasing over time.

### Why Use Staged Rollouts?

- ✅ Catch critical bugs before full release
- ✅ Monitor crash rates with limited impact
- ✅ Gather feedback from real users
- ✅ Reduce risk of widespread issues

### Recommended Rollout Schedule

**Production releases**:

| Day | User Fraction | Users (example) | Action |
|-----|---------------|-----------------|--------|
| 1   | 0.1 (10%)     | 100 users       | Deploy, monitor closely |
| 2-3 | 0.25 (25%)    | 250 users       | Check crash rates |
| 4-5 | 0.5 (50%)     | 500 users       | Review feedback |
| 6-7 | 1.0 (100%)    | All users       | Full rollout |

**Pause between stages if**:
- Crash rate > 1%
- ANR rate > 0.5%
- Negative reviews spike
- Critical bug reported

### Configure Staged Rollout in GitHub

```yaml
# Run workflow with:
track: production
userFraction: 0.1  # 10%
```

### Increase Rollout Percentage

**Option 1: Via GitHub Actions**

Re-run workflow with higher percentage:
```yaml
track: production
userFraction: 0.5  # 50%
```

**Option 2: Via Play Console**

1. Go to **"Release" → "Production"**
2. Click **"Manage rollout"**
3. Select **"Increase rollout"**
4. Choose new percentage
5. Click **"Update rollout"**

### Halt Rollout

If critical issue found:

**Via Play Console**:
1. Go to production track
2. Click **"Manage rollout"**
3. Click **"Halt rollout"**
4. New users won't get update (existing users keep it)

**Fix and Re-release**:
1. Increment version code
2. Fix bug
3. Deploy new version
4. Resume rollout

## Troubleshooting

### Build Failures

#### Error: "Keystore not found"

**Cause**: `KEYSTORE_FILE` secret not set or invalid

**Solution**:
1. Verify secret is set in GitHub
2. Re-encode keystore:
   ```bash
   base64 release.keystore | tr -d '\n'
   ```
3. Update GitHub secret
4. Re-run workflow

#### Error: "Incorrect keystore password"

**Cause**: Wrong `KEYSTORE_PASSWORD` or `KEY_PASSWORD`

**Solution**:
1. Test password locally:
   ```bash
   keytool -list -v -keystore release.keystore
   ```
2. Update GitHub secret with correct password
3. Re-run workflow

### Upload Failures

#### Error: "APK already exists"

**Cause**: Version code not incremented

**Solution**:
```gradle
// app/build.gradle
versionCode 3  // Increment
```

#### Error: "Invalid service account"

**Cause**: Service account JSON incorrect or expired

**Solution**:
1. Download new service account JSON from Cloud Console
2. Update `PLAY_STORE_SERVICE_ACCOUNT_JSON` secret
3. Re-run workflow

#### Error: "Insufficient permissions"

**Cause**: Service account lacks required permissions

**Solution**:
1. Go to Play Console → API access
2. Click on service account
3. Verify permissions:
   - ✅ Create and edit releases
   - ✅ Release to production
4. Save changes
5. Wait 5-10 minutes for propagation
6. Re-run workflow

#### Error: "Package name mismatch"

**Cause**: Package name in workflow doesn't match Play Console

**Solution**:
Verify in workflow file:
```yaml
packageName: org.neteinstein.compareapp  # Must match Play Console
```

### Play Console Issues

#### App Not Appearing in Store

**Causes**:
- App not published to production
- Store listing incomplete
- Content rating incomplete

**Solution**:
1. Complete all setup steps (store listing, content rating, etc.)
2. Publish to production track
3. Wait 2-24 hours for indexing

#### Release Under Review

**What it means**: Google is reviewing your app

**Timeline**: Usually 1-7 days

**Actions**:
- Wait patiently
- Don't upload new versions during review
- Check for emails from Google Play

## Post-Deployment

### Monitor App Performance

#### Play Console Vitals

Check: **"Quality" → "Android vitals"**

**Key Metrics**:
- **Crash rate**: Should be < 1%
- **ANR rate**: Should be < 0.5%
- **Excessive wakeups**: Monitor battery usage

#### Crash Reports

Check: **"Quality" → "Crashes & ANRs"**

**Actions**:
- Review crash stack traces
- Prioritize by affected users
- Fix high-impact crashes first

#### User Reviews

Check: **"Quality" → "Reviews"**

**Best Practices**:
- Respond to reviews (boosts rating)
- Address common complaints
- Thank positive reviewers

### Release Notes

#### GitHub Releases - Automated

GitHub releases are **automatically generated** with dynamic release notes based on merged pull requests and commits. When you push to the `main` branch:

1. The release workflow automatically creates a GitHub release
2. Release notes are generated from commits since the last release
3. Changes are categorized by labels (features, bug fixes, documentation, etc.)

To customize how changes are categorized, edit `.github/release.yml`.

#### Play Store Release Notes (What's New) - Manual

**Important:** Play Store release notes are **NOT automated** and must be manually updated before each deployment.

Before deploying to Play Store:

1. Review the [CHANGELOG.md](../CHANGELOG.md) file in the repository root
2. Check recent [GitHub Releases](https://github.com/neteinstein/CompareUberVsBoltPriceApp/releases) for changes
3. Update `distribution/whatsnew/en-US.txt` with user-facing changes
4. Ensure the content is under 500 characters (Play Store limit)
5. Optionally add translations in other language files (e.g., `es-ES.txt`, `fr-FR.txt`)

Example `distribution/whatsnew/en-US.txt`:

```
Version 1.1
• Added support for current location
• Improved geocoding accuracy
• Fixed split screen issues
• Performance improvements
```

**Character limit**: 500 per language

#### Maintaining the CHANGELOG

Keep the [CHANGELOG.md](../CHANGELOG.md) file updated with notable changes:

1. Add entries to the `[Unreleased]` section as you make changes
2. When releasing a new version, move unreleased changes to a new version section
3. Follow the [Keep a Changelog](https://keepachangelog.com/) format
4. Use categories: Added, Changed, Deprecated, Removed, Fixed, Security

### App Store Optimization (ASO)

**Improve Discoverability**:

1. **Keywords**: Add relevant keywords to description
   - "ride sharing", "price comparison", "uber", "bolt"
2. **Screenshots**: Show key features
3. **Feature graphic**: Eye-catching design
4. **Icon**: Professional, recognizable
5. **Ratings**: Encourage happy users to rate

## Best Practices

### Version Management

**Versioning Strategy**:
```
versionCode: Integer that increments each release (1, 2, 3...)
versionName: Semantic version (1.0, 1.1, 2.0...)
```

**When to Increment**:
- `versionCode`: **Every** release (required by Play Store)
- `versionName`: Major/minor/patch changes (user-facing)

### Release Checklist

Before deploying to production:

- [ ] All tests passing
- [ ] Lint checks pass
- [ ] Version code incremented
- [ ] Release notes written
- [ ] Tested on internal track
- [ ] No critical bugs
- [ ] Crash rate < 1%
- [ ] Review feedback from beta testers

### Security

**Protect Sensitive Data**:
- ✅ Never commit keystore
- ✅ Never commit passwords
- ✅ Rotate service account keys yearly
- ✅ Use GitHub secret scanning
- ✅ Review workflow logs for leaks

**Keystore Backup**:
- Store in 3+ locations
- Use encryption
- Document password securely
- Test restore process

### Communication

**User Communication**:
- In-app changelog for major updates
- Respond to Play Store reviews
- Social media announcements
- Email newsletter (if applicable)

**Team Communication**:
- Document deployment dates
- Share release notes
- Coordinate with support team
- Monitor metrics together

## Emergency Procedures

### Critical Bug in Production

1. **Halt Rollout** (if staged):
   - Play Console → Manage rollout → Halt

2. **Assess Impact**:
   - Check crash reports
   - Review user feedback
   - Estimate affected users

3. **Fix**:
   - Create hotfix branch
   - Fix bug
   - Test thoroughly
   - Increment version code

4. **Deploy Hotfix**:
   ```bash
   # Bump version
   versionCode 4
   versionName "1.1.1"
   
   # Commit
   git commit -m "Hotfix: Critical bug fix"
   git push
   
   # Deploy via GitHub Actions
   # track: production
   # userFraction: 1.0 (override previous)
   ```

5. **Monitor**:
   - Watch crash rate
   - Check user reviews
   - Verify fix effectiveness

### Lost Keystore

**Prevention is Key**: This cannot be recovered

**If Lost**:
1. **Cannot update existing app**
2. **Options**:
   - Publish as new app (lose users)
   - Contact Google Play support (rarely helps)

**Prevention**:
- Multiple encrypted backups
- Document storage locations
- Test backup restoration
- Use password manager

## Appendix

### Useful Commands

**Check keystore details**:
```bash
keytool -list -v -keystore release.keystore
```

**Verify APK signature**:
```bash
jarsigner -verify -verbose -certs app-release.apk
```

**Get APK info**:
```bash
aapt dump badging app-release.apk
```

### Resources

- [Play Console](https://play.google.com/console/)
- [Google Cloud Console](https://console.cloud.google.com/)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Google Play Publishing API](https://developers.google.com/android-publisher)
- [Play Store Policies](https://play.google.com/about/developer-content-policy/)

### Support

**Google Play Support**:
- [Help Center](https://support.google.com/googleplay/android-developer)
- Email: Via Play Console
- Phone: Enterprise support only

**GitHub Actions Support**:
- [Documentation](https://docs.github.com/en/actions)
- [Community Forum](https://github.com/orgs/community/discussions)
