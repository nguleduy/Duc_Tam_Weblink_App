<h1 align="center">Test Android Abaltatech 🚧 [Work in progress] 🚧</h1></br>

**A demo app clone from the source code WebLinkSampleClient.**

## Screenshots 
<p align="center">
<img src="/preview/preview1.png" width="1080"/>

- The main challenge we encountered while developing the WebLinkSampleClient app included a few hurdles:
  - Dealing with outdated source code (target SDK 30 and NDK 20.xxx) that couldn't be built using the latest Android IDE.
  - We had to manually download Android version 3.6.3 and NDK version r20b (20.1.5948944) and make adjustments in the build.gradle file to accommodate the older code, as using the latest NDK was not an option(androideabi has deprecated).
  - The need for 2 physical Android devices to run the demo app effectively.
  - ....

In the end, i conducted thorough research, addressed the issues, and successfully made the project compatible with the latest Android IDE (Android Studio Giraffe | 2022.3.1 Patch 2).

Please note that i apologize for not creating a brand-new client app. Instead, i cloned the sample and adapted it to work with the latest IDE due to time constraints caused by my busy schedules. However, i invested time in understanding the source code, documentation, and overall architecture to ensure I'm on the right track.
