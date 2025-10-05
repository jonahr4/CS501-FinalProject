
# MealPrep Pro 🍽️
**Smart Meal Planning & Nutrition Tracker**  
_CS501 Fall 2025 — Mobile App Development_

## 📱 App Concept
MealPrep Pro is a mobile app that helps users simplify weekly meal planning, log food intake, and track nutrition in one place. Users can discover recipes, scan food barcodes, plan their meals on a calendar, and auto-generate smart shopping lists.

This project is built using Jetpack Compose and MVVM, with Room as our local database. It also uses onboard sensors like the camera (CameraX) and GPS (FusedLocationProviderClient).

## 🎯 Target Users & Problem
Targeted at students and busy professionals who struggle with:
- Repetitive meals and lack of recipe ideas
- Forgetting ingredients while shopping
- Inconsistent nutrition tracking

MealPrep Pro solves this by combining recipe browsing, food logging, meal planning, and store location tools.

## ✨ Features

### MVP
- Barcode food scan (Nutritionix API)
- Recipe discovery & saving (TheMealDB API)
- Weekly meal planner + smart grocery list
- Nutrition dashboard with daily macros

### Stretch Goals
- Ingredient substitutions and pantry checks
- Receipt scanning for budgeting
- Social recipe sharing
- YouTube meal prep tutorials

## 🌐 APIs & 📷 Sensors
- **APIs**: TheMealDB, Nutritionix/OpenFoodFacts, Google Places
- **Sensors**: 
  - CameraX + ML Kit: barcode scanning, meal photos
  - GPS: nearby grocery alerts, store detection

## 🗺️ Navigation Map
- Meal Plan Screen
- Recipe Browser & Detail
- Camera/Barcode Scan Screen
- Nutrition Dashboard
- Shopping List
- Responsive: bottom nav (phone), side nav/tablet support

## 👥 Team & Roles
We are an Agile team of 2–3 developers.  
- **UI/UX** — Jetpack Compose, Material 3, accessibility
- **APIs & Sensors** — Retrofit, CameraX, GPS tools
- **Persistence & Models** — Room DB, data schema, analytics

We follow sprints with GitHub Issues + shared standups.

## 🛠️ Tech Stack
- Kotlin + Jetpack Compose
- MVVM architecture
- Room (local database)
- Retrofit (API layer)
- CameraX, ML Kit, FusedLocationProviderClient
- GitHub version control

## ⚠️ Risks & Open Questions
- Barcode coverage and fallback UX
- Balancing calorie tracking with health-forward language
- Stretch goals depend on time
- Food photo recognition ML is optional (time-permitting)

---

**Instructor**: Ron Czik  
**Course**: CS501 - Mobile Application Development (Fall 2025)
