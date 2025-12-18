# GitHub Actions - Automatic APK Build Instructions

## Overview

This repository is now configured with GitHub Actions to automatically build Android APK files whenever you push code to the `main` or `master` branch, or when you create a pull request. You can also manually trigger a build from the GitHub Actions tab.

## How It Works

The workflow file is located at `.github/workflows/android-build.yml` and it:

1. ✅ Sets up the build environment with Java 17 and Android SDK
2. ✅ Builds a **Debug APK** (for testing)
3. ✅ Builds a **Release APK** (unsigned, for distribution)
4. ✅ Uploads both APKs as downloadable artifacts

## How to Download Built APKs

### Method 1: After Pushing Code

1. Push your code to the `main` or `master` branch:
   ```bash
   git add .
   git commit -m "Your changes"
   git push origin main
   ```

2. Go to your GitHub repository page
3. Click on the **"Actions"** tab at the top
4. Click on the latest workflow run (it will have your commit message as the title)
5. Wait for the build to complete (usually 2-5 minutes)
6. Scroll down to the **"Artifacts"** section
7. Download either:
   - `app-debug` - Debug APK for testing
   - `app-release` - Release APK for distribution

### Method 2: Manual Trigger

1. Go to your GitHub repository page
2. Click on the **"Actions"** tab
3. Click on **"Android CI - Build APK"** in the left sidebar
4. Click the **"Run workflow"** button on the right
5. Select the branch you want to build from
6. Click **"Run workflow"**
7. Wait for the build to complete
8. Download the APK from the **"Artifacts"** section

## APK Types

### Debug APK (`app-debug.apk`)
- **Purpose**: For testing and development
- **Signing**: Signed with a debug certificate
- **Installation**: Can be installed directly on any device
- **Size**: Larger (includes debug symbols)
- **Use when**: Testing changes before release

### Release APK (`app-release.apk`)
- **Purpose**: For distribution
- **Signing**: Unsigned (you may need to sign it for release)
- **Installation**: Can be installed on devices that allow unsigned apps
- **Size**: Smaller (optimized)
- **Use when**: Distributing to users

## Signing Release APK (Optional)

The release APK is unsigned by default. For official releases:

1. Create a keystore file
2. Add GitHub Secrets for signing (see GitHub documentation)
3. Update the workflow to sign the release APK

For most users, the **unsigned release APK** works fine for sideloading.

## Troubleshooting

### Build Failed
- Check the build logs in the Actions tab
- Look for error messages in the red sections
- Common issues:
  - Gradle dependencies not downloading
  - Compilation errors in code
  - Missing dependencies

### Can't Find Artifacts
- Make sure the build completed successfully (green checkmark)
- Artifacts are only available for completed builds
- Artifacts expire after 90 days by default

### APK Won't Install
- Enable "Install from Unknown Sources" on your Android device
- Make sure you're using the correct APK for your device architecture
- Try the debug APK if the release APK doesn't work

## Benefits of Using GitHub Actions

✅ **No Local Build Required**: Build APKs without Android Studio  
✅ **Consistent Builds**: Same environment every time  
✅ **Version History**: Download APKs from any past commit  
✅ **Free**: GitHub provides free build minutes for public repositories  
✅ **Automated**: Builds happen automatically on push  

## Quick Start Summary

```
Push Code → Actions Tab → Wait for Build → Download Artifacts ✅
```

That's it! Your APK is ready to install on Android devices.
