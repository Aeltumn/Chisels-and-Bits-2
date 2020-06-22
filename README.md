# Chisels & Bits 2
A successor to [AlgorithmX2's original Chisels & Bits](https://github.com/AlgorithmX2/Chisels-and-Bits) that had releases for Minecraft 1.8 through 1.12. A Minecraft mod about chiseling, sculpting and designing custom blocks made of various materials, fluids or colours.

Download
--------------
You can always find the latest playable version of the mod on the [GitHub releases page](https://github.com/Aeltumn/Chisels-and-Bits-2/releases)!

### 1.14
The 1.14 versions are playable but lack many key features, use placeholder textures and have severe performance issues. These versions offer a demo experience showing off what 1.14 C&B looks like they are not a good representation of the final C&B2 mod.

| Type | Second Header |
| ------------- | ------------- |
| Recommended  | [0.5.2a](https://github.com/Aeltumn/Chisels-and-Bits-2/releases/tag/0.5.2a)  |
| Latest  | [0.5.2a](https://github.com/Aeltumn/Chisels-and-Bits-2/releases/tag/0.5.2a) |

### 1.15
C&B2 is currently being developed for 1.15, there are no playable builds yet!

### 1.16
No releases for 1.16 yet! I'll stay on 1.15 for a while until it's clear what version most modders will use.

Differences from the original mod
--------------
Chisels & Bits 2 is an inspired reimagining of the mod, the mod will still be about chiseling blocks but every feature has been re-evaluated and recoded with new features. Many features are planned to be added in the future that were never in the original mod.

Contributing
--------------
If you'd like to contribute something to Chisels & Bits, you're free to do so. However, not all changes will be accepted. If you're unsure if your suggestion fits the mod, [open an issue](https://github.com/Aeltumn/Chisels-and-Bits-2/issues) to discuss it first!

Compilation
--------------
1) Clone this repository and check out the branch of the version you want to build. (1.14.x, 1.15.x)
2) Load it into an IDE of your choice and import the project.
3) Run `build` using gradle
4) You'll find the built jar in the `/build/libs/` folder.

Extra steps if you want to setup a C&B development environment:

5) Create an empty file called `settings.gradle` file and enter the path to the built C&B jar to it as key "gradle.ext.buildJar". The file should contain something similar to this:
```gradle.ext.buildJar = "C:\\Chisels-and-Bits-2\\build\\libs\\chiselsandbits2-0.5.1a.jar"```
6) Run `genIntellijRuns` or `genEclipseRuns` depending on your IDE used.
7) Run `runClient` to start the mod.