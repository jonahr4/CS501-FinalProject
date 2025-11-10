# Google OAuth & Profile Persistence Guide

This guide outlines what is needed to add Google authentication with profile storage for MealMap.

## 1. Enable Google Sign-In
- Create a Firebase project (or Google Cloud project) tied to the app package `com.example.cs501_mealmapproject`.
- Add an Android app in the Firebase console, download `google-services.json`, and place it under `app/`.
- In Firebase > Authentication, enable **Google** as a sign-in provider. Copy the Web client ID.

## 2. Gradle Setup
- Add the following dependencies:
  - In `gradle/libs.versions.toml` add versions for `firebaseBom`, `googleAuth`, and `playServicesAuth`.
  - In `app/build.gradle.kts`, apply `com.google.gms.google-services` plugin and add:
    - `implementation(platform(libs.firebase.bom))`
    - `implementation(libs.firebase.auth)`
    - `implementation(libs.play.services.auth)`
- Add the `com.google.gms.google-services` classpath in `build.gradle.kts` (project level) if not present, and apply the plugin at the bottom of the module file.

## 3. Compose Integration Steps
1. Create an `AuthRepository` to abstract FirebaseAuth operations (signIn, signOut, currentUser Flow).
2. Use `GoogleSignInOptions.Builder` with the Web client ID to request the ID token.
3. Launch the Google sign-in intent from an Activity result launcher inside `MainActivity` (use `rememberLauncherForActivityResult` for Compose).
4. On success, exchange the ID token via `GoogleAuthProvider.getCredential` and sign in with FirebaseAuth.
5. Expose authentication state to Compose using a ViewModel (`AuthViewModel`) that maps Firebase user info to a `MealMapUser` model.
6. Gate the new onboarding flow on two conditions: authenticated user and whether onboarding profile exists in persistence (see section 4).

## 4. Persisting Profile Data
- Store per-user stats (calorie target, weights, activity level, streak counters) in Room.
- Suggested schema:
  - `UserEntity(id: String, displayName: String?, email: String?)`
  - `UserProfileEntity(userId: String, calorieTarget: Int, currentWeight: Float, goalWeight: Float, activityLevel: String, updatedAt: Long)`
  - `MealLogEntity`, `RecipeBookmarkEntity`, etc. can be added later.
- Use `DataStore` for lightweight flags (e.g., `hasCompletedOnboarding`). Persist the onboarding profile in Room so it can sync and drive dashboards offline.
- Create a `UserProfileRepository` that reads from Room and exposes Flows. Update onboarding completion to write through this repository.

## 5. Wiring Everything Together
1. On app start, observe auth state. If the user is signed out, show a **Sign in with Google** screen before onboarding.
2. After sign-in, fetch profile data:
   - If profile missing, show the onboarding form (current `OnboardingScreen`).
   - If profile exists, hydrate view models (nutrition dashboard, planner targets).
3. Persist onboarding submissions via `UserProfileRepository.saveProfile()` and set `hasCompletedOnboarding=true` in DataStore.
4. Ensure sign-out clears local caches: wipe Room tables or scope them by `userId`.

## 6. Additional Notes
- Update `AndroidManifest.xml` with the required `meta-data` tag for the default web client ID when using Google Sign-In.
- Add SHA-1 (and SHA-256 if using Play-integrity) fingerprints in the Firebase console for accurate OAuth validation.
- Consider using Firebase App Check or backend token verification if you later expose cloud APIs that rely on authenticated requests.

## 7. UI Hooks
- Replace the stub user in `MealMapApp` with the authenticated `MealMapUser` from `AuthViewModel`.
- Feed the current Firebase user photo URL into the profile menu: load it with Coil (`AsyncImage`) inside `UserAvatar`.
- Wire the **Reset goals** menu action to `UserProfileRepository.clearProfile(userId)` and then emit `hasCompletedOnboarding = false` so the onboarding form re-opens with previously saved values.
- Call `AuthRepository.signOut()` from the **Sign out** action, clear cached Room tables for that `userId`, and surface the Sign-In screen until `onSignIn` runs again.
- When `AuthRepository` signals a returning user with an existing profile, skip directly to the main navigation shell; otherwise, drop them into onboarding to capture their targets before proceeding.
