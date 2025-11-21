# BiteSight

**BiteSight** is an advanced nutrition tracking application designed to revolutionize how users monitor their diet. Unlike traditional calorie counters that rely solely on manual entry, BiteSight leverages **Augmented Reality (AR)** and **Machine Learning (ML)** to estimate food portion sizes and identify dishes directly through your smartphone camera.

## Features

*   **Smart Food Scanning**: Utilizes **ARCore Depth API** to measure food volume and **TensorFlow Lite** for image classification to identify foods instantly.
*   **Comprehensive Dashboard**: Real-time visualization of your daily caloric intake and macronutrients (Protein, Carbs, Fats).
*   **Progress Tracking**: Visual indicators for daily goals and weekly streaks.
*   **Meal History**: Log and review past meals to stay consistent with your dietary habits.
*   **Local Storage**: All data is securely stored on your device using **Room Database**.
*   **Hybrid UI**: A modern interface built with a mix of **XML Views** and **Jetpack Compose**.

## Tech Stack

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **UI**: Android Views (XML), Jetpack Compose, Material Design 3, MPAndroidChart
*   **Augmented Reality**: [Google ARCore](https://developers.google.com/ar) (Depth API required)
*   **Computer Vision & ML**:
    *   [CameraX](https://developer.android.com/training/camerax)
    *   [TensorFlow Lite](https://www.tensorflow.org/lite)
*   **Persistence**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)

## Hardware Requirements

To use the core scanning features of BiteSight, you need a physical Android device with specific capabilities. **Simulators/Emulators will not work for the AR features.**

1.  **Android Device**: Running Android 8.0 (Oreo) or later (Min SDK 26).
2.  **ARCore Support**: The device must be on the [list of ARCore supported devices](https://developers.google.com/ar/devices).
3.  **Depth API Support**: The device **must support the ARCore Depth API** to accurately calculate food volume from the camera feed.

## How to Run

Follow these steps to get the application running on your device.

### 1. Clone the Repository

Open your terminal or command prompt and run the following command to clone the project:

```bash
git clone https://github.com/shiahoilam/Hackathon.git
```

### 2. Open in Android Studio

1.  Launch **Android Studio**.
2.  Select **File > Open**.
3.  Navigate to the cloned directory (e.g., `.../Hackathon` or `.../BiteSite`) and select it.
4.  Wait for Android Studio to index the project and for **Gradle** to sync. This may take a few minutes as it downloads dependencies.

### 3. Set Up Your Device

1.  Enable **Developer Options** on your Android phone (Settings > About Phone > Tap Build Number 7 times).
2.  Enable **USB Debugging** in Developer Options.
3.  Connect your device to your computer via USB.
4.  Ensure your device has internet access to download Google Play Services for AR if it's not already installed.

### 4. Build and Run

1.  In Android Studio, ensure your physical device is selected in the device dropdown menu (top toolbar).
2.  Click the green **Run** button.
3.  The app will compile, install, and launch on your device.
4.  **Permissions**: Grant Camera permissions when prompted to enable the scanning features.

---
