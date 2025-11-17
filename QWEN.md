# Qwen Code Context - Hackathon Android Project

## Project Overview

This is an Android application for tracking daily calorie and macronutrient intake called "Hackathon". The app features a dashboard that displays a user's current progress towards their daily goals. The application is built using Kotlin with AndroidX libraries and includes ARCore for potential food scanning functionality.

### Key Features
- Daily calorie tracking dashboard with visual progress indicators
- Macronutrient tracking (Protein, Carbs, Fat)
- Weekly progress tracking
- Bottom navigation for app sections (Home, Analysis, Camera, History)
- Custom circular progress view with color gradient based on completion percentage

### Technologies Used
- **Kotlin**: Primary programming language
- **AndroidX Libraries**: Core Android libraries for UI and app components
- **Material Design Components**: UI components (MaterialCardView, BottomNavigationView, etc.)
- **ConstraintLayout**: Primary layout manager
- **ARCore**: Google's augmented reality platform (included but not yet implemented)
- **Gradle**: Build system with Kotlin DSL

## Project Structure

```
Hackathon/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/hackathon/
│   │   │   ├── MainActivity.kt          # Main activity with bottom navigation
│   │   │   └── CircularProgressView.kt  # Custom circular progress view component
│   │   ├── res/                        # Resources (layouts, drawables, values)
│   │   └── AndroidManifest.xml
├── gradle/
│   └── libs.versions.toml              # Dependency version management
├── build.gradle.kts                   # Project and app level build files
├── settings.gradle.kts                # Project settings
└── gradlew, gradlew.bat               # Gradle wrapper executables
```

## Key Components

### MainActivity.kt
- Main entry point of the application
- Implements bottom navigation handling
- Sets up the dashboard UI with calorie tracking components
- Contains commented code for camera functionality (to be implemented)

### CircularProgressView.kt
- Custom View that draws a circular progress indicator
- Features gradient coloring from green to yellow based on progress percentage
- Uses Canvas drawing APIs to render the circular progress bar

### UI Components
- Custom layout with ScrollView and ConstraintLayout
- Circular progress view for overall calorie tracking
- Linear progress bars for individual macronutrients
- MaterialCardView for weekly progress
- BottomNavigationView for app navigation

## Building and Running

### Prerequisites
- Android Studio (latest version recommended)
- Android SDK with API level 36 (compileSdk)
- Android device or emulator with API level 26+ (minSdk)

### Build Commands
```bash
# Build the project
./gradlew build

# Assemble debug APK
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug
```

### From Android Studio
1. Open the project directory in Android Studio
2. Sync the project with Gradle files
3. Click "Run" button or press Shift+F10

## Development Conventions

### Code Style
- Kotlin coding conventions
- Android development best practices
- Material Design guidelines for UI components
- Meaningful variable and function names

### Dependencies Management
- Dependencies defined in `gradle/libs.versions.toml` 
- Plugin aliases used in build.gradle.kts files
- AndroidX libraries preferred for compatibility

### UI Design
- XML layouts using ConstraintLayout as primary layout manager
- Material Design components for consistent UI
- Custom views for specialized components (CircularProgressView)
- Responsive design for different screen sizes

## Notable Features in Development

- ARCore integration (Google's AR platform) is included in dependencies but not yet implemented - likely intended for food scanning feature
- Camera functionality mentioned in comments but currently disabled
- Custom drawing implementation for progress indicators

## Testing

The project includes standard Android testing dependencies:
- JUnit for unit tests
- Espresso for UI tests
- AndroidX test libraries

To run tests:
```bash
./gradlew test  # Unit tests
./gradlew connectedAndroidTest  # Instrumented tests
```