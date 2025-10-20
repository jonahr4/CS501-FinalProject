# MealMap
**Smart Meal Planning & Nutrition Tracker**  
_CS501 Final Project – Jonah Rothman & Abidul Islam_

## App Concept
MealMap is a mobile app designed to simplify weekly meal planning, recipe discovery, and nutrition tracking. The goal is to help users eat healthier by making it easier to plan meals, discover new recipes, and track food intake. Key features include barcode scanning, meal photo logging, a drag-and-drop meal planning calendar, and auto-generated shopping lists.

The app will be built natively for Android using Jetpack Compose, following an MVVM architecture. We will use a Room database for offline data storage, and integrate camera and GPS sensors for barcode scanning and store detection.

## Target Users & Problem
MealMap is built for busy college students and young professionals who want to eat healthier but don’t have time to plan or shop properly. These users often fall into repetitive eating habits or rely on takeout due to the effort required to plan meals, find recipes, and build shopping lists.

MealMap combines these tasks into a single streamlined experience. With built-in tracking and streak features, users can see their progress over time and stay motivated.

## Features

### MVP (Minimum Viable Product)
- **Barcode Food Logging**: Scan barcodes to fetch nutrition data (Nutritionix/OpenFoodFacts).
- **Recipe Discovery**: Browse recipes from TheMealDB.
- **Meal Planning**: Weekly calendar to assign meals.
- **Shopping List**: Automatically built from weekly meal plan.
- **Nutrition Tracking**: Track calories and macros over time.

### Stretch Goals
- Smart ingredient substitutions.
- Receipt scanning for budget tracking.
- Meal prep video tutorials (YouTube API).
- Recipe sharing with friends.

## APIs & Sensors
- **APIs**: TheMealDB (recipes), Nutritionix or OpenFoodFacts (barcodes), Google Places (store search)
- **Sensors**: 
  - **CameraX**: Barcode scanning and photo logging
  - **GPS**: Find nearby grocery stores and show reminders

## Navigation Map
- **Meal Plan Screen**
- **Recipe Browser & Detail View**
- **Camera / Barcode Scanner Screen**
- **Nutrition Dashboard**
- **Shopping List**
- Uses bottom nav bar (phone) and responsive layout (tablet)
- Built with Scaffold + LazyColumn for composable layouts

## Team & Roles
We are a 2-person Agile team:
- **Jonah Rothman** – API and sensor integration (CameraX, GPS), backend logic
- **Abidul Islam** – UI/UX design, theming, layout using Jetpack Compose
- **Both** – Shared responsibility for Room DB, data models, testing, and integration

We’ll follow weekly sprints, commit to a shared GitHub repo, and collaborate on blockers.

## Tech Stack
- Kotlin + Jetpack Compose
- MVVM architecture
- Room database (Android Jetpack)
- Retrofit for API calls
- CameraX + ML Kit for barcode/photo capture
- GPS via FusedLocationProviderClient
- GitHub for version control

## Risks & Open Questions
- **Barcode Coverage**: Some food items may be missing; fallback to manual entry.
- **Sensor Reliability**: GPS/geofencing can be inconsistent across devices.
- **Nutrition Framing**: Language will be health-positive, not restrictive.
- **Stretch Feature Time**: Time limits may affect extra features like receipt scanning or video integration.
