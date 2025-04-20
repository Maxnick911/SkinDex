# SkinDex

SkinDex is an Android application and it fetches and displays posts from the JSONPlaceholder API, showcasing modern Android development practices. The app is planned to be extended into a diploma project for analyzing medical skin images.

## Features

- Fetch and display a list of posts from JSONPlaceholder API.
- View detailed information for each post.
- Smooth navigation between screens.

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM
- **Libraries**:
  - Jetpack Navigation Component for navigation
  - Retrofit and Moshi for API requests
  - Kotlin Coroutines and StateFlow for asynchronous operations
  - RecyclerView with ListAdapter for efficient list rendering
  - Hilt for dependency injection
  - Material 3 for UI design

## Setup

1. Clone the repository:

   ```
   git clone https://github.com/Maxnick911/SkinDex.git
   ```

2. Open the project in Android Studio.

3. Sync the project with Gradle.

4. Build and run the app on an emulator or device.

## Usage

- Launch the app.
- Tap the "Fetch Posts" button to load posts from the API.
- Tap on a post to view its details.

## Future Plans

This project will be extended for a diploma thesis to include medical skin image analysis, potentially integrating machine learning models for skin condition detection.

## License

This project is licensed under the MIT License.
