# Home IoT

## Introduction

This project is a simple home IoT system that allows you to control
your [home devices](https://www.github.com/jetsup/HomeIoT.git) from an
Android app.
This is a client application that communicates with a server application that is running on a
Microcontroller.
The server application is written in C and runs on an ESP32 microcontroller.
The server application is responsible for controlling the devices and the client application is
responsible for sending commands to the server application.

## Features

- Turn on/off a device
- Get the status of a device or all devices
- Get the list of devices

## Technologies

- Android
- Java
- ESP32
- C/C++

## Requirements

- Android Studio
- PlatformIO

## Installation

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the project
4. Flash the ESP32 with the server application
5. Connect the ESP32 to the same network as the Android device
6. Open the app and start controlling your devices
7. Enjoy!

## Project Limitations

- The UI is user friendly but does not have an appealing design. This is because I am not a UI/UX
  designer and therefore any help in this area would be appreciated.
- The UI was designed for a phone with a screen size of 6.8 inches running Android 13. This means
  that the UI may not look good on other devices.

## Future Improvements/Ideas

- Add support for BLE communication

## License

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
