# CI/CD Pipeline Documentation

## Table of Contents

1. [Overview](#overview)
2. [Pipeline Architecture](#pipeline-architecture)
3. [Workflows](#workflows)
4. [Workflow Details](#workflow-details)
5. [Secrets Configuration](#secrets-configuration)
6. [Artifacts and Outputs](#artifacts-and-outputs)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

## Overview

CompareApp uses **GitHub Actions** for continuous integration and continuous deployment. The CI/CD pipeline automates:

- **Code Quality**: Linting and static analysis
- **Testing**: Unit and integration tests
- **Building**: APK generation
- **Release**: Automated GitHub releases
- **Deployment**: Play Store publishing

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Repository                         │
└────────┬─────────────────────────┬────────────┬─────────────┘
         │                         │            │
    ┌────▼────┐              ┌─────▼─────┐     │
    │   PR    │              │   Push    │     │
    │  Event  │              │ to Main   │     │
    └────┬────┘              └─────┬─────┘     │
         │                         │            │
         ▼                         ▼            │
┌──────────────────┐      ┌──────────────────┐ │
│   PR Check       │      │  Build & Release │ │
│   Workflow       │      │    Workflow      │ │
│                  │      │                  │ │
│  1. Lint         │      │  1. Build APK    │ │
│  2. Unit Tests   │      │  2. Sign APK     │ │
│  3. Upload       │      │  3. Create       │ │
│     Reports      │      │     Release      │ │
└──────────────────┘      └──────────────────┘ │
                                                │
                                    ┌───────────▼──────────┐
                                    │   Manual Trigger     │
                                    │  (workflow_dispatch) │
                                    └───────────┬──────────┘
                                                │
                                                ▼
                                    ┌──────────────────────┐
                                    │ Deploy to Play Store │
                                    │     Workflow         │
                                    │                      │
                                    │  1. Build APK        │
                                    │  2. Sign APK         │
                                    │  3. Upload to        │
                                    │     Play Store       │
                                    └──────────────────────┘
```

## Workflows

### 1. PR Check (`pr-check.yml`)
- **Trigger**: Pull requests to `main` branch
- **Purpose**: Validate code quality before merging
- **Duration**: ~2-5 minutes

### 2. Build and Release (`release.yml`)
- **Trigger**: Push to `main` branch
- **Purpose**: Automated releases on every merge
- **Duration**: ~3-7 minutes

### 3. Deploy to Play Store (`deploy-playstore.yml`)
- **Trigger**: Manual (`workflow_dispatch`)
- **Purpose**: Deploy to Google Play Store
- **Duration**: ~5-10 minutes

## Workflow Details

### PR Check Workflow

**File**: `.github/workflows/pr-check.yml`

#### Trigger
```yaml
on:
  pull_request:
    branches:
      - main
```

Runs on every pull request targeting the `main` branch.

#### Jobs

##### Job: `lint-and-test`

**Step 1: Checkout Code**
```yaml
- name: Checkout code
  uses: actions/checkout@v4
```
- **Action**: `actions/checkout@v4`
- **Purpose**: Clone repository to runner
- **Output**: Repository files available in workspace

**Step 2: Set up JDK 17**
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: '17'
    distribution: 'temurin'
    cache: 'gradle'
```
- **Action**: `actions/setup-java@v4`
- **Purpose**: Install Java Development Kit
- **JDK Version**: 17 (Temurin distribution)
- **Cache**: Gradle dependencies (speeds up subsequent runs)
- **Why JDK 17**: Required for Android SDK 36 and modern Kotlin

**Step 3: Grant Execute Permission**
```yaml
- name: Grant execute permission for gradlew
  run: chmod +x gradlew
```
- **Purpose**: Make Gradle wrapper executable on Unix-like systems
- **Command**: `chmod +x gradlew`

**Step 4: Run Lint**
```yaml
- name: Run Lint
  run: ./gradlew lint --no-daemon
```
- **Purpose**: Run Android lint checks
- **Command**: `./gradlew lint --no-daemon`
- **Checks**:
  - Code style issues
  - Potential bugs
  - Performance issues
  - Security vulnerabilities
  - Unused resources
  - API usage problems
- **Flag `--no-daemon`**: Prevents Gradle daemon from staying alive (better for CI)
- **Output**: HTML and XML reports in `app/build/reports/lint-results-*.html`

**Step 5: Upload Lint Results**
```yaml
- name: Upload lint results
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: lint-results
    path: app/build/reports/lint-results-*.html
    if-no-files-found: ignore
```
- **Action**: `actions/upload-artifact@v4`
- **Condition**: `if: always()` - uploads even if previous steps failed
- **Purpose**: Store lint reports as artifacts
- **Artifact Name**: `lint-results`
- **Path**: `app/build/reports/lint-results-*.html`
- **Retention**: 90 days (GitHub default)

**Step 6: Run Unit Tests**
```yaml
- name: Run Unit Tests
  run: ./gradlew test --no-daemon
```
- **Purpose**: Execute all unit tests
- **Command**: `./gradlew test --no-daemon`
- **Test Types**:
  - Unit tests (JUnit)
  - Mocked Android tests (Robolectric)
  - ViewModel tests
  - Repository tests
- **Output**: Test reports in `app/build/reports/tests/`
- **Exit Code**: Non-zero if any test fails (fails the workflow)

**Step 7: Upload Test Results**
```yaml
- name: Upload test results
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: app/build/reports/tests/
    if-no-files-found: ignore
```
- **Action**: `actions/upload-artifact@v4`
- **Condition**: `if: always()` - uploads even if tests failed
- **Purpose**: Store test reports for review
- **Artifact Name**: `test-results`
- **Includes**: HTML test reports with pass/fail details

---

### Build and Release Workflow

**File**: `.github/workflows/release.yml`

#### Trigger
```yaml
on:
  push:
    branches:
      - main
```

Runs when code is pushed to the `main` branch (typically after PR merge).

#### Jobs

##### Job: `build-and-release`

**Permissions**
```yaml
permissions:
  contents: write
```
- **Required**: To create GitHub releases and upload assets

**Step 1-3**: Same as PR Check (checkout, JDK setup, permissions)

**Step 4: Build and Sign APK**
```yaml
- name: Build and Sign APK
  env:
    KEYSTORE_FILE: ${{ secrets.KEYSTORE_FILE }}
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: ./gradlew assembleRelease --no-daemon
```
- **Purpose**: Build release APK (signed if secrets available)
- **Command**: `./gradlew assembleRelease --no-daemon`
- **Environment Variables**: Signing secrets from GitHub
- **Gradle Configuration**: `app/build.gradle` checks for these env vars
- **Output**: 
  - `app/build/outputs/apk/release/app-release.apk` (signed)
  - OR `app/build/outputs/apk/release/app-release-unsigned.apk` (unsigned)

**Build Process**:
1. Compile Kotlin code
2. Process resources
3. Generate DEX files
4. Package into APK
5. Sign with keystore (if available)
6. Align APK

**Step 5: Get Version Info**
```yaml
- name: Get version info
  id: version
  run: |
    VERSION_NAME=$(grep "versionName" app/build.gradle | \
      awk '{print $2}' | tr -d '"')
    VERSION_CODE=$(grep "versionCode" app/build.gradle | \
      awk '{print $2}')
    TIMESTAMP=$(date +'%Y%m%d-%H%M%S')
    echo "version_name=${VERSION_NAME}" >> $GITHUB_OUTPUT
    echo "version_code=${VERSION_CODE}" >> $GITHUB_OUTPUT
    TAG="v${VERSION_NAME}-build${VERSION_CODE}-${TIMESTAMP}"
    echo "release_tag=${TAG}" >> $GITHUB_OUTPUT
```
- **Purpose**: Extract version information from `build.gradle`
- **Outputs**:
  - `version_name`: e.g., "1.0"
  - `version_code`: e.g., "1"
  - `release_tag`: e.g., "v1.0-build1-20240115-143022"
- **Technique**: Uses `grep` and `awk` to parse Gradle file
- **Tag Format**: `v{VERSION_NAME}-build{VERSION_CODE}-{TIMESTAMP}`

**Step 6: Rename APK**
```yaml
- name: Rename APK
  run: |
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
      SRC="app/build/outputs/apk/release/app-release.apk"
      SUFFIX="signed"
    else
      SRC="app/build/outputs/apk/release/app-release-unsigned.apk"
      SUFFIX="unsigned"
    fi
    VERSION="${{ steps.version.outputs.version_name }}"
    DEST="app/build/outputs/apk/release/CompareApp-${VERSION}-${SUFFIX}.apk"
    mv "${SRC}" "${DEST}"
    echo "apk_suffix=${SUFFIX}" >> $GITHUB_OUTPUT
  id: rename
```
- **Purpose**: Rename APK with version and signing status
- **Logic**: Check if signed APK exists, otherwise use unsigned
- **Output**: `CompareApp-1.0-signed.apk` or `CompareApp-1.0-unsigned.apk`
- **Step Output**: `apk_suffix` for use in release notes

**Step 7: Create Release**
```yaml
- name: Create Release
  uses: softprops/action-gh-release@v2
  with:
    tag_name: ${{ steps.version.outputs.release_tag }}
    name: Release ${{ steps.version.outputs.version_name }} ...
    body: |
      ## CompareApp Release ...
      Build number: ...
      Built from commit: ${{ github.sha }}
    files: |
      app/build/outputs/apk/release/CompareApp-...
    draft: false
    prerelease: false
```
- **Action**: `softprops/action-gh-release@v2`
- **Purpose**: Create GitHub release with APK
- **Release Name**: "Release 1.0 (Build 1)"
- **Tag**: Generated tag with timestamp
- **Attached Files**: Signed or unsigned APK
- **Release Notes**: Auto-generated with version and commit info
- **Status**: Published (not draft, not pre-release)

---

### Deploy to Play Store Workflow

**File**: `.github/workflows/deploy-playstore.yml`

#### Trigger
```yaml
on:
  workflow_dispatch:
    inputs:
      track:
        description: 'Release track'
        required: true
        default: 'internal'
        type: choice
        options: [internal, alpha, beta, production]
      inAppUpdatePriority:
        description: 'In-app update priority (0-5)'
        required: false
        default: '2'
        type: choice
        options: ['0', '1', '2', '3', '4', '5']
      userFraction:
        description: 'User fraction for staged rollout (0.0-1.0)'
        required: false
        default: '1.0'
        type: string
```
- **Trigger Type**: Manual via GitHub UI
- **Location**: Actions tab → Deploy to Play Store → Run workflow

**Input Parameters**:

1. **track**: Which release track to deploy to
   - `internal`: Internal testing (small team)
   - `alpha`: Alpha testing (limited users)
   - `beta`: Beta testing (larger audience)
   - `production`: Production release (all users)

2. **inAppUpdatePriority**: Update urgency (0-5)
   - `0`: Low priority (user can skip)
   - `5`: Critical (forces update)

3. **userFraction**: Staged rollout percentage
   - `0.1`: 10% of users
   - `0.5`: 50% of users
   - `1.0`: 100% (full release)

#### Jobs

##### Job: `deploy-to-playstore`

**Step 1-4**: Same as release workflow (checkout, JDK, build)

**Step 5: Verify APK is Signed**
```yaml
- name: Verify APK is signed
  run: |
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
    if [ ! -f "$APK_PATH" ]; then
      echo "Error: Signed APK not found at $APK_PATH"
      ls -la app/build/outputs/apk/release/
      exit 1
    fi
    echo "✓ Signed APK found at $APK_PATH"
```
- **Purpose**: Ensure APK is signed before upload
- **Reason**: Play Store rejects unsigned APKs
- **Error Handling**: Lists directory contents if APK not found
- **Exit Code**: 1 if verification fails (stops workflow)

**Step 6: Upload to Play Store**
```yaml
- name: Upload to Play Store
  uses: r0adkll/upload-google-play@v1.1.3
  with:
    serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_SERVICE_ACCOUNT_JSON }}
    packageName: org.neteinstein.compareapp
    releaseFiles: app/build/outputs/apk/release/app-release.apk
    track: ${{ inputs.track }}
    status: completed
    inAppUpdatePriority: ${{ inputs.inAppUpdatePriority }}
    userFraction: ${{ inputs.userFraction }}
    whatsNewDirectory: distribution/whatsnew
```
- **Action**: `r0adkll/upload-google-play@v1.1.3`
- **Authentication**: Service account JSON (from secrets)
- **Package Name**: `org.neteinstein.compareapp`
- **Release Files**: Signed APK
- **Track**: User-selected (internal/alpha/beta/production)
- **Status**: `completed` (immediately available to users)
- **Update Priority**: User-selected (0-5)
- **User Fraction**: User-selected (0.0-1.0)
- **What's New**: Release notes from `distribution/whatsnew/`

**Upload Process**:
1. Authenticate with Google Play API
2. Upload APK to specified track
3. Set release status to "completed"
4. Apply staged rollout percentage
5. Publish release notes from `distribution/whatsnew/`

## Secrets Configuration

### Required Secrets

All secrets are configured in: **GitHub Repository → Settings → Secrets and variables → Actions**

#### APK Signing Secrets

| Secret Name | Description | Required For | Example Value |
|-------------|-------------|--------------|---------------|
| `KEYSTORE_FILE` | Base64-encoded keystore file | Release, Play Store | (base64 string, ~2000 chars) |
| `KEYSTORE_PASSWORD` | Keystore password | Release, Play Store | `mySecurePassword123!` |
| `KEY_ALIAS` | Key alias in keystore | Release, Play Store | `compareapp` |
| `KEY_PASSWORD` | Key password | Release, Play Store | `myKeyPassword456!` |

**How to Generate `KEYSTORE_FILE`**:

```bash
# Linux/Mac
base64 release.keystore | tr -d '\n' > keystore.base64.txt

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File keystore.base64.txt
```

Then copy contents of `keystore.base64.txt` to GitHub secret.

#### Play Store Deployment Secret

| Secret Name | Description | Required For | Format |
|-------------|-------------|--------------|--------|
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Service account credentials | Play Store | JSON file contents |

**How to Get Service Account JSON**:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create or select project
3. Enable "Google Play Android Developer API"
4. Create service account
5. Download JSON key
6. Copy entire JSON contents to GitHub secret

### Secret Security

- **Never commit** secrets to repository
- **Rotate** service account keys periodically
- **Use least privilege**: Grant minimum required permissions
- **Audit access**: Review who has access to secrets
- **Masked in logs**: GitHub automatically masks secret values

## Artifacts and Outputs

### PR Check Artifacts

| Artifact | Contents | Retention | Size |
|----------|----------|-----------|------|
| `lint-results` | HTML lint reports | 90 days | ~500 KB |
| `test-results` | HTML test reports | 90 days | ~2 MB |

**Accessing Artifacts**:
1. Go to PR → Checks → Workflow run
2. Scroll to "Artifacts" section
3. Download ZIP file

### Release Artifacts

| Artifact | Contents | Retention | Size |
|----------|----------|-----------|------|
| GitHub Release APK | Signed/unsigned APK | Permanent | ~5-10 MB |

**Accessing Releases**:
1. Go to repository → Releases
2. Find release by tag
3. Download APK from "Assets"

## Troubleshooting

### Common Issues

#### Issue 1: Lint Failures

**Symptom**: PR check fails at "Run Lint" step

**Causes**:
- Code style violations
- Unused resources
- API deprecation warnings

**Solutions**:
```bash
# Run lint locally
./gradlew lint

# View report
open app/build/reports/lint-results-debug.html

# Fix issues or suppress with annotation
@SuppressLint("RuleName")
```

#### Issue 2: Test Failures

**Symptom**: PR check fails at "Run Unit Tests" step

**Causes**:
- Broken test
- Code change broke existing functionality
- Flaky test

**Solutions**:
```bash
# Run tests locally
./gradlew test

# Run specific test
./gradlew test --tests "MainViewModelTest"

# View report
open app/build/reports/tests/testDebugUnitTest/index.html
```

#### Issue 3: Unsigned APK in Release

**Symptom**: Release contains `app-release-unsigned.apk`

**Causes**:
- Signing secrets not configured
- Incorrect secret names
- Invalid keystore

**Solutions**:
1. Check all 4 signing secrets are set
2. Verify secret names match exactly
3. Re-encode keystore to base64
4. Test locally:
   ```bash
   export KEYSTORE_FILE="base64_string_here"
   export KEYSTORE_PASSWORD="password"
   export KEY_ALIAS="alias"
   export KEY_PASSWORD="password"
   ./gradlew assembleRelease
   ```

#### Issue 4: Play Store Upload Fails

**Symptom**: "Deploy to Play Store" workflow fails at upload step

**Causes**:
- Invalid service account JSON
- Insufficient permissions
- Version code not incremented
- Package name mismatch

**Solutions**:

1. **Invalid Service Account**:
   - Re-download service account JSON
   - Ensure entire JSON is copied to secret
   - No extra whitespace or formatting

2. **Insufficient Permissions**:
   - Go to Play Console → API Access
   - Grant service account these permissions:
     - Create, edit, and delete draft apps
     - Release apps to testing tracks
     - Manage production APKs

3. **Version Code Not Incremented**:
   ```gradle
   // app/build.gradle
   versionCode 2  // Increment this
   versionName "1.1"
   ```

4. **Package Name Mismatch**:
   - Ensure `org.neteinstein.compareapp` matches Play Console

#### Issue 5: Workflow Doesn't Trigger

**Symptom**: Workflow doesn't run when expected

**Solutions**:

1. **PR Check Not Running**:
   - Ensure PR targets `main` branch
   - Check workflow file syntax
   - Verify workflow is enabled in Actions tab

2. **Release Not Running**:
   - Ensure push is to `main` branch
   - Check for GitHub Actions outages
   - Verify workflow permissions

## Best Practices

### 1. Version Management

**Always increment version code before release**:

```gradle
// app/build.gradle
android {
    defaultConfig {
        versionCode 2  // Increment for each release
        versionName "1.1"  // Update for user-facing changes
    }
}
```

**Semantic Versioning**:
- `1.0.0`: Major release
- `1.1.0`: Minor feature
- `1.1.1`: Bug fix

### 2. Release Notes

**Maintain release notes in `distribution/whatsnew/`**:

```
distribution/whatsnew/
├── en-US.txt
├── es-ES.txt
└── de-DE.txt
```

**Example `en-US.txt`**:
```
- Added support for more ride-sharing services
- Improved geocoding accuracy
- Fixed split screen issues on some devices
- Performance improvements
```

**Limits**:
- Max 500 characters
- Plain text only
- One file per language

### 3. Testing Strategy

**Before Merging PR**:
1. Run tests locally: `./gradlew test`
2. Run lint: `./gradlew lint`
3. Build release APK: `./gradlew assembleRelease`
4. Test on physical device

**Staged Rollouts**:
- Start with internal track (1-2 days)
- Promote to alpha (20-50 users, 1-3 days)
- Promote to beta (500-1000 users, 3-7 days)
- Roll out to production gradually:
  - 10% (1 day)
  - 25% (1 day)
  - 50% (1 day)
  - 100% (full release)

### 4. Monitoring

**After Release**:
- Check GitHub Actions for errors
- Monitor Play Console for crash reports
- Review user reviews
- Check app vitals (ANR rate, crash rate)

**Play Console Metrics**:
- Crash-free rate: Should be >99%
- ANR rate: Should be <0.5%
- User reviews: Monitor for issues

### 5. Rollback Plan

**If Critical Bug Found**:

1. **Stop Rollout**:
   - Go to Play Console
   - Pause rollout or reduce user fraction to 0%

2. **Fix Bug**:
   - Create hotfix branch
   - Fix issue
   - Increment version code

3. **Emergency Release**:
   - Merge hotfix to main
   - Run "Deploy to Play Store" workflow
   - Select `production` track
   - Set `userFraction` to 1.0 (override previous version)

### 6. Security

**Keystore Management**:
- ✅ Store keystore securely (encrypted backup)
- ✅ Use strong passwords
- ✅ Never commit keystore to Git
- ✅ Rotate service account keys yearly

**Secret Rotation**:
```bash
# 1. Generate new keystore
keytool -genkeypair -v -keystore new-release.keystore ...

# 2. Update GitHub secrets
# 3. Test build
# 4. Delete old keystore securely
```

## Workflow Visualization

### Complete CI/CD Flow

```
Developer
    │
    ├─► Create Feature Branch
    │       │
    │       ├─► Make Changes
    │       │       │
    │       └─► Open Pull Request
    │               │
    │               ▼
    │         ┌─────────────┐
    │         │  PR Check   │
    │         │  Workflow   │
    │         │             │
    │         │ ✓ Lint      │
    │         │ ✓ Tests     │
    │         └──────┬──────┘
    │                │
    │         ┌──────▼──────┐
    │         │   Review    │
    │         │   Approve   │
    │         └──────┬──────┘
    │                │
    └─► Merge to Main◄┘
            │
            ▼
    ┌───────────────┐
    │  Build &      │
    │  Release      │
    │  Workflow     │
    │               │
    │ 1. Build APK  │
    │ 2. Sign APK   │
    │ 3. Create     │
    │    Release    │
    └───────┬───────┘
            │
            ├─► GitHub Release Created
            │       │
            │       └─► APK Available for Download
            │
            │   (Manual Trigger)
            │
            ▼
    ┌───────────────┐
    │  Deploy to    │
    │  Play Store   │
    │  Workflow     │
    │               │
    │ 1. Build APK  │
    │ 2. Sign APK   │
    │ 3. Upload to  │
    │    Play Store │
    └───────┬───────┘
            │
            └─► App Published on Play Store
```

## Performance Metrics

### Typical Build Times

| Workflow | Duration | Factors |
|----------|----------|---------|
| PR Check | 2-5 min | Tests, lint |
| Build & Release | 3-7 min | APK build, signing |
| Deploy to Play Store | 5-10 min | Build + API upload |

### Optimization Tips

1. **Gradle Caching**: Enabled via `actions/setup-java` with `cache: 'gradle'`
2. **Parallel Execution**: Tests run in parallel by default
3. **No Daemon**: Use `--no-daemon` in CI (prevents memory leaks)
4. **Incremental Builds**: Gradle uses incremental compilation

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Gradle Plugin](https://developer.android.com/build)
- [Google Play Publishing API](https://developers.google.com/android-publisher)
- [APK Signing](https://developer.android.com/studio/publish/app-signing)
