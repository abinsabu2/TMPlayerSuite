# Git Workflow Instructions

## To create a new branch and push this code:

### 1. Initialize Git Repository (if not already done)
```bash
git init
git remote add origin <your-repository-url>
```

### 2. Create and Switch to New Branch
```bash
git checkout -b feature/tdlib-integration
```

### 3. Stage All Changes
```bash
git add .
```

### 4. Commit Changes
```bash
git commit -m "feat: Add TDLib integration with TV-optimized login

- Add Telegram Database Library (TDLib) support
- Implement complete authentication flow (phone, code, 2FA)
- Create TV-friendly login interface using GuidedStepSupportFragment
- Add real-time message and chat management
- Integrate with existing Android TV leanback UI
- Add proper error handling and state management

Dependencies added:
- TDLib 1.8.29
- Kotlin Coroutines 1.7.3
- Kotlin Serialization 1.6.0

Requires API credentials from https://my.telegram.org/apps"
```

### 5. Push to Remote Repository
```bash
git push -u origin feature/tdlib-integration
```

### 6. Create Pull Request
After pushing, create a pull request on your Git platform (GitHub, GitLab, etc.) to merge the feature branch into your main branch.

## File Structure Summary
```
app/src/main/java/com/example/tmplayersuite/
├── telegram/
│   ├── TelegramClient.kt          # Low-level TDLib wrapper
│   └── TelegramManager.kt         # High-level Telegram API
├── TelegramActivity.kt            # Main Telegram activity
├── TelegramFragment.kt            # Telegram main interface
├── TelegramLoginActivity.kt       # Login flow activity
├── TelegramLoginFragment.kt       # TV-optimized login UI
└── MainFragment.kt                # Updated with Telegram menu

app/src/main/res/layout/
├── activity_telegram.xml          # Telegram activity layout
├── fragment_telegram.xml          # Telegram fragment layout
└── activity_telegram_login.xml    # Login activity layout

Configuration Files:
├── gradle/libs.versions.toml      # Dependency versions
├── app/build.gradle.kts           # Build configuration
├── app/src/main/AndroidManifest.xml  # App manifest
└── app/src/main/res/values/strings.xml  # String resources
```

## Next Steps After Git Setup
1. Configure Telegram API credentials
2. Test the authentication flow
3. Implement additional Telegram features as needed
4. Consider adding unit tests for Telegram functionality