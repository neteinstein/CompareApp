# Release Notes Implementation

This document explains how dynamic release notes work in the CompareApp project.

## Problem Statement

The issue requested that release notes show **actual changes** instead of static information for each release.

## Solution Overview

We implemented a two-tier approach to release notes:

### 1. GitHub Releases - Fully Automated

GitHub releases now automatically generate release notes from commit history when code is pushed to the `main` branch.

**Implementation:**
- Updated `.github/workflows/release.yml` to use `generate_release_notes: true` and `append_body: true`
- Created `.github/release.yml` to configure release note categories and labels
- The workflow appends auto-generated notes to static installation/requirements information

**Categories configured:**
- 🚀 New Features (labels: feature, enhancement)
- 🐛 Bug Fixes (labels: bug, fix)
- 📚 Documentation (labels: documentation, docs)
- 🔧 Maintenance (labels: maintenance, chore, dependencies)
- 🏗️ Infrastructure (labels: ci, workflow, github-actions)
- Other Changes (catch-all for unlabeled changes)

### 2. Play Store Releases - Manual Updates Required

Play Store "What's New" notes must be manually updated before each deployment.

**Workflow:**
1. Review `CHANGELOG.md` for recent changes
2. Review GitHub Releases for detailed change history
3. Update `distribution/whatsnew/en-US.txt` (and other language files)
4. Deploy to Play Store using the manual workflow

**Why manual?**
- Play Store has a 500-character limit
- Requires human curation to extract user-facing changes
- Supports multiple languages (requires translation)

## New Files Created

1. **CHANGELOG.md** - Human-curated changelog following [Keep a Changelog](https://keepachangelog.com/) format
2. **.github/release.yml** - Configuration for automatic GitHub release note generation

## Updated Files

1. **.github/workflows/release.yml** - Added automatic release note generation
2. **DEPLOYMENT.md** - Added section on release notes workflow
3. **distribution/whatsnew/README.md** - Added detailed workflow instructions
4. **distribution/whatsnew/en-US.txt** - Template reminder to update
5. **README.md** - Added reference to CHANGELOG

## Usage

### For GitHub Releases (Automatic)

Just push to `main` branch. The workflow will:
1. Build and sign the APK
2. Create a GitHub release with auto-generated notes from commits
3. Categorize changes by PR labels

**Best Practice:** Label your PRs appropriately (feature, bug, docs, etc.) for better categorization.

### For Play Store Releases (Manual)

Before deploying to Play Store:
1. Review CHANGELOG.md
2. Review recent GitHub releases
3. Update `distribution/whatsnew/en-US.txt` with user-facing highlights
4. Keep it under 500 characters
5. Run the Play Store deployment workflow

## Benefits

✅ GitHub releases now show actual changes instead of static text  
✅ Automatic categorization of changes by type  
✅ Historical tracking via CHANGELOG.md  
✅ Clear workflow for Play Store release notes  
✅ Minimal maintenance overhead  

## References

- [GitHub Auto-generated Release Notes](https://docs.github.com/en/repositories/releasing-projects-on-github/automatically-generated-release-notes)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
