# MealMap
**Smart Meal Planning & Nutrition Tracker**
_CS501 Final Project – Jonah Rothman & Abidul Islam_

### Demo Video
<a href="https://www.youtube.com/watch?v=SQnV2oUZETA">
  <img src="https://img.youtube.com/vi/SQnV2oUZETA/hqdefault.jpg" alt="MealMap demo video" width="480" />
</a>

### Final Report
[CS501 Final Report (Google Doc)](https://docs.google.com/document/d/1hEBdZXUW6IlqsPdSrZ4G38wX1gVTOeCIexfKeeMVKjs/edit?tab=t.0)

---
### Final Project Overview 12/16
---
## Build & Run Instructions

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK API 26+ (min) and 36 (target)
- Physical device or emulator with camera support (for barcode scanning)

### Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/jonahr4/CS501-FinalProject.git
   cd CS501-FinalProject
   ```

2. **Open in Android Studio**
* Launch Android Studio
* Select File → Open… and choose the cloned project folder.

3. **Sync Gradle**
* Android Studio will automatically prompt to sync.
* If not, click “Sync Project with Gradle Files” in the toolbar.

4. **Run the App**
* Connect the Android Studio emulator.
* Click Run  in Android Studio.

The app should build successfully and launch into the onboarding screen.

---
## Feature Status

### ✅ Fully Implemented Features

| Feature | Implementation Details |
|---------|-----------------------|
| **Meal Planning + Shopping List** | Plan the week and auto-generate grouped ingredients per user |
| **Recipe Discovery** | Search TheMealDB and cache details/ingredients for logging |
| **Food Logging** | Manual entries, barcode scans, and recipe-based logging with nutrition lookup |
| **Nutrition Dashboard** | Daily calories/macros with goals, progress bars, and alerts |
| **Onboarding & Goals** | Collect calorie target, weights, activity level and persist per user |
| **Auth & Sync** | Google/Firebase auth; optional Firestore sync for meal plans and logs |

### Future Features

| Feature | Status | Reason |
|---------|--------|--------|
| **Smart Ingredient Substitution** | Future | Would require additional AI/ML models or API integration |
| **Receipt Scanning / Budget Tracking** | Future | OCR integration complex; out of MVP scope |
| **GPS Store Detection** | Future | Location permissions and geofencing added complexity |
| **Recipe Sharing** | Future | Social features require backend infrastructure |


---

## Core Requirements

### ✅ Core Requirements
- **External APIs**: TheMealDB (recipes), OpenFoodFacts (barcode nutrition), USDA FoodData Central (food search/nutrition), Firebase Auth/Firestore for sync
- **Sensor**: CameraX preview with ML Kit barcode scanning and runtime permissions
- **Local Persistence**: Room for food logs/meal plans/recipe cache; SharedPreferences for onboarding profile and serialized weekly plan per user
- **External DB**: Firebase Firestore mirrors meal plans/food logs when signed in; Google Sign-In via Firebase Auth
- **UI Polish**: Compose + Material 3 theme, responsive layouts, loading/empty states
- **Error Handling**: Friendly network/error states, manual entry fallback for scans, guarded DB operations

---

## Architecture Overview

### MVVM Architecture
- ![Project Architecture](Assets/Project%20Architecture.png) — MVVM with Compose UI, ViewModels using StateFlow, repositories for Room/Retrofit/Firebase
- ![User Flow](Assets/User%20FLow.png) — Onboarding → planning → recipes → logging → dashboard/shopping

## Nutrition Calculation Details

### Daily Calorie Goal
- Set during onboarding; defaults to 2000 calories if not customized with weight and activity 
- Goals persisted per user and reused across sessions

### Macro Targets (current logic)
- **Protein**: ~0.8 g per lb of current weight
- **Fat**: ~30% of calorie goal ÷ 9 cal/g
- **Carbs**: Remaining calories after protein and fat, ÷ 4 cal/g
- Computed from onboarding profile and today’s logs; dashboard shows consumed vs goal


---

## Testing Strategy

### Manual Testing
- **Feature Testing**: Each screen tested for UI responsiveness and data flow
- **Navigation Testing**: Verified all navigation paths work correctly
- **Permission Testing**: Camera permissions tested on physical device
- **API Testing**: Tested with valid/invalid barcodes and recipe searches
- **Edge Cases**:
  - Empty states (no logs, no recipes found)
  - Invalid inputs (negative numbers, empty strings)
  - Network failures (airplane mode testing)

### Debugging Tools Used
- **Android Studio Logcat**: Monitoring API responses and errors
- **Android Studio Database Inspector**: Viewing Room database contents
- **Layout Inspector**: Debugging Compose UI hierarchy
- **Network Profiler**: Monitoring API call performance

### Known Limitations
- Barcode scanner may not work well in Android Emulator (requires physical device)
- Some barcodes may not be in OpenFoodFacts database (manual entry available)
- Macro split is fixed (30/40/30) - not yet customizable per user

---

## Team Workflow

## Team & Roles
We are a 2-person Agile team:
- **Jonah Rothman** – API and sensor integration (CameraX, GPS), backend logic
- **Abidul Islam** – UI/UX design, theming, layout using Jetpack Compose
- **Both** – Shared responsibility for Room DB, data models, testing, and integration

---

## Tech Stack

**Frontend:**
- Kotlin 2.0
- Jetpack Compose (Material 3)
- Jetpack Navigation Compose
- Coil (Image Loading)

**Backend/Data:**
- Room Database 2.6.1
- SharedPreferences
- Retrofit 2.9.0 + Moshi (JSON)
- Kotlin Coroutines + Flow

**APIs:**
- TheMealDB (recipes)
- OpenFoodFacts (barcode nutrition)
- USDA FoodData Central (food search/nutrition)
- Firebase Auth + Firestore (auth/sync)

**Sensors/Hardware:**
- CameraX 1.3.1
- ML Kit Barcode Scanning 17.2.0
- Accompanist Permissions 0.32.0

**Development Tools:**
- Android Studio Hedgehog+
- Gradle 8.13
- Git/GitHub

# AI Usage Statement

## Tools Used
- Android Studio Gemini, Claude Code, ChatGPT
- Used for code lookup, debugging hints, small code snippets, and test scaffolds

## Helpfulness
- Very effective for small, specific tasks
- Speeds up debugging by explaining errors and library behavior
- Helpful for quickly locating project files/functions

## Limitations
- Struggles with full-app context; answers become generic
- Sometimes suggests code that breaks existing logic
- All output required verification with tests/debugger

## Misleading Example
- Asked: “Rewrite the FoodLog flow using best practices”
- AI generated a new architecture that didn’t match our project

## Understanding & Corrections
- We double-checked all AI suggestions
- Used AI as a learning tool, not a final code source
- Ensured we understood each change before applying it
