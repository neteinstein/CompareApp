# Play Store Release Notes

This directory contains "What's New" release notes for Play Store deployments.

## Important

**These files must be manually updated before each Play Store deployment** to reflect the actual changes in the release.

## Structure

Each file should be named with the locale code and `.txt` extension:
- `en-US.txt` - English (United States)
- `es-ES.txt` - Spanish (Spain)
- `fr-FR.txt` - French (France)
- etc.

## Guidelines

- **Maximum 500 characters per file** (Play Store limit)
- Focus on **user-facing changes only**
- Be concise and clear
- **Update before each Play Store release** (this is not automated)
- Check the CHANGELOG.md and recent GitHub releases for changes to include

## Workflow

Before deploying to Play Store:
1. Review the [CHANGELOG.md](../../CHANGELOG.md) in the repository root
2. Review recent [GitHub Releases](https://github.com/neteinstein/CompareUberVsBoltPriceApp/releases)
3. Extract user-facing changes and summarize them here
4. Ensure the text is under 500 characters
5. Deploy using the Play Store workflow

## Example

```
🚀 New Features:
- Dark mode support for better viewing at night
- Favorite locations for quick access

🐛 Bug Fixes:
- Fixed crash when switching apps
- Improved location accuracy
```
