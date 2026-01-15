# APK Signing and Play Store Deployment Setup

This document explains how to configure APK signing and Play Store deployment for the CompareApp.

## Table of Contents

1. [APK Signing Setup](#apk-signing-setup)
2. [Play Store Deployment Setup](#play-store-deployment-setup)
3. [GitHub Secrets Configuration](#github-secrets-configuration)
4. [Usage](#usage)

## APK Signing Setup

The app is configured to automatically sign release APKs when the required secrets are available in the GitHub repository.

### Required GitHub Secrets for APK Signing

You need to configure the following secrets in your GitHub repository:

1. **KEYSTORE_FILE**: Base64-encoded keystore file
2. **KEYSTORE_PASSWORD**: Password for the keystore
3. **KEY_ALIAS**: Alias of the key in the keystore
4. **KEY_PASSWORD**: Password for the key

### Creating a Keystore

If you don't have a keystore yet, create one using the following command:

```bash
keytool -genkeypair -v -keystore release.keystore -alias compareapp -keyalg RSA -keysize 2048 -validity 10000
```

Follow the prompts to set passwords and enter your information.

### Encoding the Keystore for GitHub Secrets

To use the keystore in GitHub Actions, encode it to base64:

**On Linux/Mac:**
```bash
base64 release.keystore | tr -d '\n' > keystore.base64.txt
```

**On Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -FilePath keystore.base64.txt -NoNewline
```

Copy the contents of `keystore.base64.txt` and use it as the `KEYSTORE_FILE` secret value.

## Play Store Deployment Setup

The Play Store deployment uses a manual GitHub Action workflow that can be triggered on-demand.

### Prerequisites

1. A Google Play Console account
2. Your app must be registered in Google Play Console
3. A service account with Play Store deployment permissions

### Creating a Service Account

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google Play Android Developer API
4. Navigate to "IAM & Admin" > "Service Accounts"
5. Create a new service account
6. Grant it the necessary permissions
7. Create a JSON key for the service account
8. Download the JSON key file

### Linking Service Account to Play Console

1. Go to [Google Play Console](https://play.google.com/console/)
2. Navigate to "Setup" > "API access"
3. Link your Google Cloud project
4. Grant access to the service account with the following permissions:
   - View app information and download bulk reports (read-only)
   - Create, edit, and delete draft apps
   - Release apps to testing tracks
   - Release apps to production
   - Manage production APKs

### Required GitHub Secret for Play Store Deployment

**PLAY_STORE_SERVICE_ACCOUNT_JSON**: The entire contents of the service account JSON key file

## GitHub Secrets Configuration

To add secrets to your GitHub repository:

1. Go to your repository on GitHub
2. Click on "Settings"
3. Navigate to "Secrets and variables" > "Actions"
4. Click "New repository secret"
5. Add each secret with its respective value:

### APK Signing Secrets

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `KEYSTORE_FILE` | Base64-encoded keystore | (base64 string) |
| `KEYSTORE_PASSWORD` | Keystore password | `myKeystorePassword123` |
| `KEY_ALIAS` | Key alias in keystore | `compareapp` |
| `KEY_PASSWORD` | Key password | `myKeyPassword123` |

### Play Store Deployment Secret

| Secret Name | Description |
|-------------|-------------|
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Service account JSON key contents |

**Note:** For `PLAY_STORE_SERVICE_ACCOUNT_JSON`, copy the entire contents of the JSON file, including the curly braces.

## Usage

### Automatic APK Signing (on Release)

When you push to the `main` branch, the release workflow automatically:
1. Builds the release APK
2. Signs it (if signing secrets are configured)
3. Creates a GitHub release with the signed APK

If signing secrets are not configured, it will create an unsigned APK instead.

### Manual Play Store Deployment

To deploy to Google Play Store:

1. Go to your repository on GitHub
2. Click on "Actions"
3. Select "Deploy to Play Store" workflow
4. Click "Run workflow"
5. Configure the deployment options:
   - **track**: Select the release track (internal/alpha/beta/production)
   - **inAppUpdatePriority**: Set update priority (0-5, default: 2)
   - **userFraction**: Set rollout percentage (0.0-1.0, default: 1.0 for 100%)
6. Click "Run workflow"

The workflow will:
1. Build and sign the release APK
2. Upload it to the specified track on Google Play Store
3. Set the release to "completed" status

#### Staged Rollouts

For production releases, you can use staged rollouts by setting `userFraction` to less than 1.0:
- `0.1` = 10% of users
- `0.5` = 50% of users
- `1.0` = 100% of users (full rollout)

### Release Notes (What's New)

To include release notes in your Play Store deployment:

1. Create a directory: `distribution/whatsnew/`
2. Add language-specific text files with release notes:
   - `distribution/whatsnew/en-US.txt`
   - `distribution/whatsnew/es-ES.txt`
   - etc.

Each file should contain a brief description of what's new (max 500 characters).

## Troubleshooting

### Signing Fails

- Verify that all signing secrets are correctly set in GitHub
- Ensure the keystore password and key password are correct
- Check that the key alias matches the one in your keystore

### Play Store Upload Fails

- Verify the service account JSON is correctly configured
- Ensure the service account has the necessary permissions in Play Console
- Check that the package name matches: `org.neteinstein.compareapp`
- Ensure the version code is higher than the current version in Play Store
- For first-time uploads, you may need to manually upload the first APK through Play Console

### Version Management

The app uses an automatic versioning system:

- **versionName**: Follows semantic versioning (MAJOR.MINOR.PATCH) and is set in `app/build.gradle`
- **versionCode**: Automatically incremented using GitHub Actions run number
- **Release tags**: Use the format `vMAJOR.MINOR.PATCH.BUILD` (e.g., `v1.0.0.42`)

#### Updating the Version

To release a new version:

1. **For patch releases** (bug fixes): No change needed - the build number auto-increments
2. **For minor releases** (new features): Update `versionName` in `app/build.gradle`:
   ```gradle
   versionName "1.1.0"  // Increment MINOR version, reset PATCH to 0
   ```
3. **For major releases** (breaking changes): Update `versionName` in `app/build.gradle`:
   ```gradle
   versionName "2.0.0"  // Increment MAJOR version, reset MINOR and PATCH to 0
   ```

The `versionCode` is automatically set from the `BUILD_NUMBER` environment variable, which GitHub Actions populates with `github.run_number`. For local builds, it defaults to 1.

#### Version Code Issues

The version code automatically increments with each GitHub Actions run. If you need to manually set a version code for testing:

```bash
BUILD_NUMBER=123 ./gradlew assembleRelease
```

For Play Store uploads, ensure your GitHub Actions run number is higher than the current version code in the Play Store.

## Security Best Practices

1. **Never commit** your keystore file, passwords, or service account JSON to the repository
2. Keep your keystore file backed up securely
3. Use strong passwords for keystore and key
4. Rotate service account keys periodically
5. Only grant minimum necessary permissions to the service account
6. Review GitHub Actions logs carefully - secrets are masked but be cautious

## References

- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Google Play Publishing API](https://developers.google.com/android-publisher)
- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
