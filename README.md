# MealMap
**Smart Meal Planning & Nutrition Tracker**
_CS501 Final Project – Jonah Rothman & Abidul Islam_

---
### Update - 12/2
---

## Feature Status

### ✅ Fully Implemented Features

| Feature | Implementation Details | Technologies |
|---------|----------------------|--------------|
| **Recipe Discovery** | Search and browse recipes from TheMealDB API with detailed ingredient lists and instructions | Retrofit, Moshi, Coil |
| **Meal Planning Calendar** | Weekly meal planner with SharedPreferences persistence. Assign recipes to specific days/meals | SharedPreferences, StateFlow |
| **Barcode Food Logging** | Scan product barcodes using device camera, fetch nutrition data from OpenFoodFacts API | CameraX, ML Kit Barcode Scanning, Retrofit |
| **Manual Food Logging** | Input food manually with custom nutrition values (calories, protein, carbs, fat) via dialog form | Jetpack Compose Dialogs, Room Database |
| **Shopping List Generation** | Auto-generated from weekly meal plan recipes with smart ingredient grouping and duplicate detection | ViewModel, StateFlow |
| **Nutrition Tracking Dashboard** | Real-time dashboard showing daily calorie and macro progress with visual progress bars | Room Database, Flow, Jetpack Compose |
| **User Onboarding** | Collect user goals (calorie target, current/goal weight, activity level) to personalize experience | SharedPreferences via SessionViewModel |

### Future Features

| Feature | Status | Reason |
|---------|--------|--------|
| **Smart Ingredient Substitution** | Future | Would require additional AI/ML models or API integration |
| **Receipt Scanning / Budget Tracking** | Future | OCR integration complex; out of MVP scope |
| **GPS Store Detection** | Future | Location permissions and geofencing added complexity |
| **Recipe Sharing** | Future | Social features require backend infrastructure |


---

## Core Requirements

### ✅ External API Integration
- **TheMealDB API**: Recipe search, ingredient lists, cooking instructions
- **OpenFoodFacts API**: Barcode scanning for nutrition data (calories, macros, product names)
- Both APIs use Retrofit with Moshi for JSON parsing
- Error handling with try-catch blocks and fallback UI states

### ✅ Sensor Feature Implementation
- **CameraX**: Real-time camera preview for barcode scanning
- **ML Kit Barcode Scanning**: Detects and decodes product barcodes (EAN-13, UPC-A, etc.)
- Runtime camera permissions via Accompanist Permissions library

### ✅ Local Persistence
- **Room Database**: Stores food log entries with nutrition data
  - `FoodLogEntity` table with fields: id, mealName, calories, protein, carbs, fat, source, timestamp
  - Database version 1 with auto-migration support
- **SharedPreferences**: Stores user onboarding profile and meal plan data
  - Calorie targets, weight goals, activity level
  - Weekly meal assignments persisted across app restarts

### ✅ UI Polish
- Material 3 Design System with custom theme colors
- Smooth animations and transitions
- Progress bars with rounded corners and color coding
- Consistent spacing and typography
- Empty state messages and loading indicators

### ✅ Error Handling
- Network failures display user-friendly error messages
- Barcode scan failures fallback to manual entry
- Database operations wrapped in try-catch with logging
- Input validation on all user forms (non-empty checks, number parsing)
- Graceful degradation when APIs are unavailable

---

## Architecture Overview

### MVVM Architecture

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  (Composables: Screens, Dialogs, Components)    │
│              ↓ StateFlow ↑ Events               │
└─────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────┐
│               ViewModel Layer                    │
│  • FoodLogViewModel                             │
│  • NutritionDashboardViewModel                  │
│  • MealPlanViewModel                            │
│  • RecipeDiscoveryViewModel                     │
│  • SessionViewModel                             │
└─────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────┐
│               Data Layer                         │
│  • Room Database (FoodLogDao, AppDatabase)      │
│  • Repositories (RecipeDiscoveryRepository)     │
│  • API Services (OpenFoodFactsService,         │
│    MealApiService)                              │
│  • SharedPreferences                            │
└─────────────────────────────────────────────────┘
```

## Nutrition Calculation Details

### Daily Calorie Goal
- Set by user during onboarding (default: 2000 calories)
- Used as basis for macro calculations

### Macro Distribution (30/40/30 Split)
The app calculates macro goals using a balanced macro split:

```kotlin
// Protein: 30% of calories from protein
val proteinGoal = (calorieGoal * 0.30 / 4).toFloat()  // ÷4 cal/gram

// Carbs: 40% of calories from carbs
val carbsGoal = (calorieGoal * 0.40 / 4).toFloat()    // ÷4 cal/gram

// Fat: 30% of calories from fat
val fatGoal = (calorieGoal * 0.30 / 9).toFloat()      // ÷9 cal/gram
```

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

## Stretch Goals

### ✅ Achieved
- ~~Barcode scanning~~ - Fully implemented with ML Kit
- ~~Smart shopping list~~ - Auto-generated with ingredient grouping
- ~~Nutrition dashboard~~ - Real-time tracking with progress visualization

### 🕓 Future Work
- **Smart Ingredient Substitution**: Would require NLP or additional recipe API
- **Receipt Scanning / Budget Tracking**: OCR integration is complex
- **GPS Store Detection**: Location permissions and geofencing out of scope
- **Recipe Sharing**: Would require backend server and user accounts
- **Meal Prep Video Tutorials**: YouTube API integration deprioritized

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
- TheMealDB API (recipe data)
- OpenFoodFacts API (barcode nutrition data)

**Sensors/Hardware:**
- CameraX 1.3.1
- ML Kit Barcode Scanning 17.2.0
- Accompanist Permissions 0.32.0

**Development Tools:**
- Android Studio Hedgehog+
- Gradle 8.13
- Git/GitHub

