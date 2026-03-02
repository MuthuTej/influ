## YouTube OAuth Integration Setup Guide

### 1. **Get Your Google Client ID**

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create or select a project
3. Enable these APIs:
   - YouTube Data API v3
   - YouTube Analytics API

4. Create an OAuth 2.0 credential (Installed application):
   - Go to **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID**
   - Select **Android Application**
   - Enter your app's package name: `np.com.bimalkafle.firebaseauthdemoapp`
   - Add your app's signing certificate SHA-1:
     - Run: `./gradlew signingReport` 
     - Copy the SHA-1 hash from the debug variant
   - Create the credential and copy your Client ID

### 2. **Update YouTubeViewModel with Your Client ID**

In `YouTubeViewModel.kt`, replace `YOUR_GOOGLE_CLIENT_ID`:

```kotlin
fun initializeGoogleSignIn(context: Context): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com") // ← Replace with your Client ID
        .requestScopes(
            Scope("https://www.googleapis.com/auth/youtube.readonly"),
            Scope("https://www.googleapis.com/auth/youtube.upload"),
            Scope("https://www.googleapis.com/auth/youtube.analytics.readonly")
        )
        .build()
    return GoogleSignIn.getClient(context, gso)
}
```

### 3. **Verify Backend URL**

Ensure `GraphQLClient.kt` has your backend GraphQL endpoint:

```kotlin
private const val BASE_URL = "https://your-backend-url/graphql"
```

### 4. **Add Dependencies (if not already present)**

Your `build.gradle.kts` should have:

```gradle
implementation("com.google.android.gms:play-services-auth:21.0.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
```

### 5. **Update AndroidManifest.xml**

Ensure these permissions are present:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 6. **Integration in Navigation**

The YouTube connection is accessible from:
- **InfluencerProfileScreen** → "Connect YouTube" button
- Route: `connectYouTube`

### 7. **What Happens When User Connects YouTube**

**Flow:**
1. User clicks "Connect YouTube" on profile
2. Google Sign-In dialog opens requesting:
   - YouTube channel access (readonly)
   - YouTube upload permission
   - YouTube Analytics permission
3. After successful auth, server receives authorization code
4. `connectYouTube(code)` GraphQL mutation is called
5. Backend exchanges code for tokens and syncs channel data
6. Channel info is returned and displayed to user

**Data Returned:**
- Channel ID
- Title & Description
- Subscriber count
- Total views
- Video count
- Playlist IDs
- Topics & Keywords
- Country information
- Sync timestamp

### 8. **Testing the Integration**

1. **Test Authorization Flow:**
   ```
   - Navigate to InfluencerProfileScreen
   - Tap "Connect YouTube"
   - Complete Google Sign-In
   - Verify channel data displays
   ```

2. **Check Backend Logs:**
   ```
   - Monitor your backend for:
     - connectYouTube mutation calls
     - YouTube API requests
     - Token exchange logs
   ```

3. **Verify Data Storage:**
   ```
   - Check Firestore: influencers/{userId}/socialData/youtube
   - Should contain all channel fields
   ```

### 9. **Troubleshooting**

**Issue: "No server auth code received"**
- Ensure `requestIdToken()` uses correct package-name
- Regenerate signing certificate SHA-1
- Update it in Google Cloud Console

**Issue: "connectYouTube mutation fails"**
- Verify backend GraphQL endpoint is accessible
- Check Firebase token is valid
- Ensure backend scopes match what app requests

**Issue: Channel data shows zeros (0 views, 0 subscribers)**
- New YouTube channels take 24-48 hours for stats
- Check YouTube Analytics dashboard permissions
- Ensure authorization includes `youtube.analytics.readonly` scope

**Issue: "Network error" in connection**
- Verify Firebase is authenticated
- Check internet connectivity
- Ensure backend URL is correct in GraphQLClient.kt

### 10. **Files Created/Modified**

**New Files:**
- `YouTubeViewModel.kt` - Manages YouTube connection state
- `YouTubeModels.kt` - Data classes for YouTube data
- `ConnectYouTubeScreen.kt` - UI for connection flow

**Modified Files:**
- `InfluencerProfileScreen.kt` - Added YouTube connect button
- `MyAppNavigation.kt` - Added connectYouTube route

### 11. **Architecture Overview**

```
UI Layer:
  ConnectYouTubeScreen
    ├─ InitialState (show connect button)
    ├─ LoadingState (animation while connecting)
    ├─ SuccessState (show channel data)
    └─ ErrorState (show error with retry)

ViewModel Layer:
  YouTubeViewModel
    ├─ initializeGoogleSignIn()
    ├─ connectYouTube(authCode, token)
    └─ Private: parseYouTubeChannelData()

Network Layer:
  GraphQLClient.query()
    └─ Sends connectYouTube mutation to backend

Backend:
  youtube.resolver.js
    └─ connectYouTube(code: String!): YouTubeChannelData!
```

### 12. **Next Steps**

1. ✅ Replace `YOUR_GOOGLE_CLIENT_ID` with actual ID
2. ✅ Verify backend URL
3. ✅ Test the flow end-to-end
4. ✅ Monitor backend logs during testing
5. ✅ Deploy to production

---

For issues or questions, check:
- Backend documentation: `connect-backend/docs/YOUTUBE_VIDEO_WORKFLOW.md`
- Google OAuth docs: https://developers.google.com/identity/gsi/web
- YouTube API docs: https://developers.google.com/youtube/v3
