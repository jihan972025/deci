# SLM-CORE PRO - Sound Level Meter

Professional-grade sound level meter (소음측정기) Android application with engineering-grade design.

## Features

- **Real-time Sound Measurement**: Measures sound levels in decibels (dB) with A-weighting
- **Professional Gauge Display**: Analog-style gauge with color-coded markers
- **Live Spectrum Analysis**: Real-time frequency spectrum visualization
- **Statistical Analysis**: L90 (baseline) and L10 (peak) calculations
- **Fast Response**: 125ms update rate for accurate measurements
- **Calibrated Measurement**: Reference calibration at 94.0 dB SPL

## Technical Specifications

- **Sample Rate**: 44,100 Hz
- **Audio Format**: 16-bit PCM
- **Channel**: Mono
- **Weighting**: A-weighted (approximation)
- **Measurement Range**: 0-120 dB
- **Response Time**: Fast (125ms)

## Installation

1. Open the project in Android Studio Arctic Fox or later
2. Sync Gradle files
3. Build and run on an Android device (API 26+)

## Permissions

The app requires the following permission:
- `RECORD_AUDIO`: To access the device microphone for sound measurement

## Usage

1. Launch the app
2. Grant microphone permission when prompted
3. The app automatically starts measuring sound levels
4. View real-time measurements on the LCD display and analog gauge
5. Monitor spectrum analysis and statistical data (L90/L10)

## Architecture

### Core Components

- **MainActivity**: Main UI controller with lifecycle management
- **AudioRecorder**: Handles audio capture and decibel calculation
- **GaugeView**: Custom view for analog gauge visualization
- **SpectrumView**: Custom view for frequency spectrum display

### Calculation Method

Sound pressure level (SPL) is calculated using:
```
RMS = √(Σ(sample²) / n)
Amplitude = RMS / 32768
dB = 20 × log₁₀(Amplitude) + 94
```

Reference: 94 dB SPL = 1 Pa = full scale digital

## Design

The UI follows an engineering-grade design inspired by professional sound level meters:
- Dark theme with high contrast
- Monospace fonts for numerical displays
- Color-coded measurement zones (green/yellow/red)
- Brushed metal texture background
- Professional status indicators

## Minimum Requirements

- Android 8.0 (API 26) or higher
- Microphone hardware
- ~10 MB storage space

## Build Configuration

- **Compile SDK**: 34
- **Min SDK**: 26
- **Target SDK**: 34
- **Kotlin**: 1.9.20
- **Gradle**: 8.2.0

## Dependencies

- AndroidX Core KTX
- Material Design 3
- AndroidX Lifecycle
- Kotlin Coroutines

## License

Copyright © 2024 SLM-CORE. All rights reserved.

## Disclaimer

This app is intended for reference purposes. For professional noise measurement, use certified sound level meters that comply with IEC 61672 standards.
