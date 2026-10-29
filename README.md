# Smart Pantry Manager

Smart Pantry Manager is a native Android application built in Java for tracking pantry ingredients, preventing food waste, and surfacing only recipes whose ingredient requirements are fully satisfied by the user’s current pantry.

## Features
- Pantry dashboard with ingredient count and expiring-soon indicators
- Add, edit, and delete pantry items with SQLite persistence
- Strict recipe matching with canonical ingredient normalization and unit conversion rules
- Suggested Recipes screen that refreshes when pantry data changes
- Recipe detail view with eligibility status
- Settings screen with expiry alerts and recipe reset option
- Seeded recipe library with database initialization on first launch

## Tech stack
- Java
- Android XML layouts
- SQLiteOpenHelper
- RecyclerView and Material components
- SharedPreferences for app settings

## Database design
The app stores pantry data and recipe data in SQLite tables. Pantry entries are unique by canonical ingredient name to prevent duplicate or conflicting records, while recipe requirements are stored separately and matched at runtime using a dedicated service class.

## Run instructions
1. Open the project in Android Studio.
2. Let Gradle sync.
3. Connect an emulator or Android device.
4. Run the app from the `app` module.

## Testing
Run:

```bash
./gradlew test
```

## Repository
https://github.com/dillan-garahushoma/smart_pantry
