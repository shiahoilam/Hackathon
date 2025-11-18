# Project Overview

This is an Android application for tracking daily calorie and macronutrient intake. The app features a dashboard that displays a user's current progress towards their daily goals.

The main technologies used are:
- **Kotlin:** The primary programming language.
- **AndroidX Libraries:** For building the user interface and managing app components.
- **ARCore:** Google's augmented reality platform. This is included as a dependency, but not yet implemented in the main activity. It is likely intended for a feature that involves food scanning.

## Building and Running

This is a standard Android Gradle project.

- **To build the project:** Run `./gradlew build` in the root directory.
- **To run the project:** Open the project in Android Studio and run it on an emulator or a physical device.

## Development Conventions

- **UI:** The UI is built using XML layouts and standard Android UI components. The design is clean and modern, with a focus on data visualization.
- **Code:** The code is written in Kotlin and follows standard Android development practices.
- **Dependencies:** Dependencies are managed using Gradle and the `libs.versions.toml` file.
