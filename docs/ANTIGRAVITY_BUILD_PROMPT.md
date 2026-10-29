# Antigravity / VS Code Build Specification — Smart Pantry Manager

## Your role

Act as a senior Android engineer, Java developer, mobile UI/UX designer, and software testing engineer. Build a complete, polished, functional Android application that satisfies the Richfield Graduate Institute of Technology Mobile App Development 700 practical assignment.

Do not stop after creating mockups, screens, or a project plan. Implement the actual application, connect the UI to persistent storage, test the core workflows, and document the result.

## 1. Project definition

- **Application name:** Smart Pantry Manager
- **Platform:** Native Android
- **Language:** Java only. Kotlin is prohibited.
- **IDE:** Android Studio
- **Layout system:** XML layouts using appropriate Android layout containers.
- **Database:** SQLite using `SQLiteOpenHelper`, unless the existing project has a well-justified, working alternative.
- **Purpose:** Help users reduce food waste by tracking ingredients they have at home and recommending recipes they can prepare using only their available ingredients.

### Non-negotiable business rule

A recipe may appear in the main Suggested Recipes screen only when the user's pantry contains every required ingredient in the required quantity. If one required ingredient is missing or insufficient, the recipe must not appear.

Do not replace this rule with approximate matching, AI-generated recommendations, shopping suggestions, or recipes that require ingredients the user does not have.

## 2. First inspect the workspace

Before coding:

1. Inspect the complete project directory, existing Git repository, Android SDK configuration, dependencies, and current implementation.
2. Determine whether the workspace contains an existing Smart Pantry Manager project or an unrelated application.
3. If it contains the contact-list assignment, do not simply rename contact classes or overwrite that project. Create a clean Smart Pantry Manager project in a separate directory and repository.
4. Produce a concise requirements checklist mapping every assignment requirement to the planned implementation.
5. Preserve useful existing configuration only when compatible with this assignment.
6. This repository is named `smart_pantry`. Use its existing Git remote and default branch; do not create a differently named repository or overwrite unrelated work.

## 3. Required screens

Build at least these five complete screens, with working navigation.

### A. Pantry Home

Create a polished dashboard showing:
- A clear greeting and the application's purpose.
- Number of ingredients currently in the pantry.
- Ingredients approaching their expiry dates, when dates are available.
- A prominent action to add an ingredient.
- A prominent action to discover recipes.
- A dynamic pantry list populated from SQLite.
- Ingredient name, quantity, unit, and optional expiry date.
- Search and useful sorting or filtering.
- Clear empty states when no ingredients exist.

Use a RecyclerView with a custom Java adapter.

### B. Add/Edit Ingredient

Provide a functional form for ingredient name, quantity, unit selected from a sensible dropdown, and optional expiry date.

Requirements:
- Validate required fields.
- Reject invalid quantities, including zero or negative values.
- Display useful inline validation messages.
- Support both creation and editing.
- Prevent duplicate or inconsistent ingredient records where appropriate.
- Return to the pantry list and refresh displayed data after saving.

### C. Suggested Recipes

Seed at least 15–20 recipes into the local database on first launch. Each recipe must have a unique ID, name, short description, list of required ingredients and quantities, preparation instructions, and reasonable preparation time.

The screen must run the actual strict-matching algorithm against the current pantry. Only qualifying recipes may appear in the main suggestions list. Each result should show its name, preparation time, and concise ingredient summary.

When no recipes qualify, display a designed empty state explaining that the user can add ingredients to unlock more recipes. Whenever pantry data changes, suggestions must reflect the updated pantry without reinstalling the app.

### D. Recipe Detail

Display the recipe name and description, complete ingredient requirements and quantities, preparation method as readable numbered steps, preparation time, and clear back navigation.

Do not imply that a recipe is makeable merely because its detail screen is open. The strict eligibility decision belongs to the matching service.

### E. Settings

Provide useful settings such as:
- Expiring-soon alert preference.
- Preferred quantity units or display preference.
- A way to restore or reload the seeded recipe collection.
- An application information section.

Persist appropriate preferences using SharedPreferences or the existing database design.

## 4. Strict recipe-matching engine

This is the highest-priority functional requirement. Implement matching in a dedicated Java service/class, separate from Activities and UI code.

For every recipe:
1. Load all required ingredients and quantities.
2. Normalize ingredient names to handle capitalization, surrounding whitespace, and simple singular/plural differences.
3. Match equivalent units only where a reliable conversion is supported.
4. Compare each required quantity against the available pantry quantity.
5. Reject the recipe immediately if any ingredient is absent or insufficient.
6. Return the recipe only if every requirement passes.

Do not silently treat incompatible units as equivalent. If a conversion cannot be established safely, treat the quantities as incompatible and exclude the recipe.

Use a canonical ingredient representation or alias mapping for common variations such as `tomato` and `tomatoes`. Avoid unsafe substring matching, where an ingredient might accidentally match an unrelated name.

### Acceptance tests

- Recipe requires 2 eggs; pantry contains 3 eggs: eligible.
- Recipe requires 2 eggs; pantry contains 1 egg: excluded.
- Recipe requires eggs and flour; pantry contains eggs only: excluded.
- All required ingredients exist in sufficient quantities: eligible.
- A user removes one required ingredient: the recipe disappears from suggestions.
- A user adds the missing ingredient: the recipe appears if all other requirements are satisfied.
- Case or simple plural differences do not incorrectly prevent a valid match.
- Reopening the app preserves pantry data and produces correct suggestions.

Do not use fabricated matching results or hardcoded recipe eligibility.

## 5. Database and persistence

Use SQLite with a sensible relational data model.

Suggested entities:
- `PantryItem`: ID, canonical ingredient name, display name, quantity, unit, optional expiry date.
- `Recipe`: ID, name, description, preparation time, instructions.
- `RecipeIngredient`: recipe ID, ingredient reference or canonical name, required quantity, unit.
- `AppSettings`, only if settings cannot be represented more simply using SharedPreferences.

Implement database creation, upgrades, seeded recipes, and complete CRUD operations. Preserve user data after closing and reopening the app. Seed recipes only when appropriate; do not insert duplicates on every launch. Keep database operations and business logic separate from Activities. Handle database failures and empty results gracefully.

## 6. UI/UX direction

Design this as a contemporary consumer mobile application, not a generic student CRUD demonstration.

Use a warm, modern kitchen-inspired visual identity:
- Cream or warm off-white backgrounds.
- Deep forest green as the principal accent.
- Muted sage secondary surfaces.
- Charcoal typography.
- Restrained amber accents for expiry warnings.
- Consistent spacing, typography, corner radii, and iconography.

Create a clear visual hierarchy with generous spacing, readable ingredient quantities, purposeful visual groupings, and attractive recipe presentation. Use local drawable assets or original illustrations where useful. Do not depend on broken remote image URLs or placeholder images.

Design loading, empty pantry, no matching recipes, validation errors, database errors, expiring ingredients, and successful save/delete states. Use accessible contrast, sensible touch targets, scrollable forms, and layouts that work on different Android screen sizes.

Avoid excessive cards, overcrowded dashboards, emoji-heavy interfaces, and decorative features that interfere with the core workflow.

## 7. Navigation and Android fundamentals

Demonstrate correct use of:
- Java Activities and the Android Activity lifecycle.
- Explicit Intents for screen navigation.
- Intent extras or another appropriate mechanism to pass record IDs.
- RecyclerView and a custom Adapter.
- XML layouts.
- SQLite persistence.
- Input validation.
- Android resources and strings.

Implement a consistent toolbar and a working navigation element, such as bottom navigation or an appropriate toolbar menu. Use sensible back navigation and prevent stale screen data after CRUD operations.

Do not add Google Maps, mapping SDKs, GPS, device location, payment processing, or unrelated features. These are explicitly outside the assignment scope.

## 8. Testing and quality assurance

Write and execute tests for the strict-matching algorithm, including complete matches, missing ingredients, insufficient quantities, name normalization, supported unit conversions, unsupported/incompatible units, empty pantry, and multiple recipes with different eligibility outcomes.

Test the full pantry CRUD cycle, recipe seeding, navigation, and persistence where feasible. Build and run the application on an Android emulator or device if available. Fix compilation errors, runtime crashes, resource errors, broken navigation, and database defects before declaring the project complete.

Do not claim a test passed unless it was actually executed.

## 9. GitHub requirements

Use this repository: `https://github.com/dillan-garahushoma/smart_pantry`.

Develop incrementally under Git version control. Make at least 10 genuine commits across meaningful development stages, such as:
1. Initialize Android project and resources.
2. Create SQLite schema and database helper.
3. Implement pantry CRUD.
4. Build pantry list and RecyclerView adapter.
5. Implement ingredient validation.
6. Seed the recipe collection.
7. Implement strict recipe matching.
8. Build suggested-recipe and detail screens.
9. Add settings and persistence.
10. Test, fix defects, and complete documentation.

Commit messages must describe real changes. Do not manufacture historical commits, falsely represent development dates, or make all changes in one final commit just to satisfy the count.

Push actual working source code and verify that the remote repository contains the intended branch and files. If GitHub access or credentials are unavailable, finish local work and clearly explain what is needed to push it. Never claim a push succeeded without verification.

## 10. README and portfolio of evidence

Create a useful README containing the application overview, features and technical stack, database choice and justification, architecture and database model, Android Studio setup/run instructions, testing instructions, repository link, and known limitations.

Also create a report-support folder containing:
- A screen-flow diagram.
- An entity-relationship diagram.
- A checklist of required screenshots.
- A checklist of concepts to explain in the demonstration.

Do not fabricate screenshots of a running application, testing evidence, development history, challenges, or personal reflections. These must come from actual implementation and testing.

## 11. Assessment-oriented completion checklist

Verify each rubric category:
- App functionality and requirements: 20 marks.
- Database implementation: 15 marks.
- Code quality and Java fundamentals: 25 marks.
- UI/UX design: 10 marks.
- GitHub version control: 10 marks.
- Video demonstration support: 10 marks.
- Written report support: 10 marks.

Prioritize correctness, clear Java code, a defensible database design, and demonstrable strict-matching behaviour over unnecessary features.

The assignment requires a 5–7 minute narrated video and a written report with screenshots from the actual running app. Prepare the project to make these deliverables straightforward, but do not fabricate the student's narration or personal reflection.

## 12. Final deliverable

At completion, provide:
1. The Android Studio project source.
2. The confirmed GitHub repository URL and branch, if successfully pushed.
3. Build and test results, including any failures.
4. A concise list of implemented features.
5. A list of remaining defects or limitations.
6. Precise steps to run the app.
7. A checklist for capturing real screenshots, recording the narrated demonstration, and preparing the report and submission ZIP.

Work systematically from the existing workspace, implement the real application end to end, and verify the strict-matching rule before polishing secondary features.
