# Chisels & Bits 2
A successor to [AlgorithmX2's original Chisels & Bits](https://github.com/AlgorithmX2/Chisels-and-Bits) that had releases for Minecraft 1.8 through 1.12. A Minecraft mod about chiseling, sculpting and designing custom blocks made of various materials, fluids or colours.

Download
--------------
You can always find the latest playable version of the mod on the [GitHub releases page](https://github.com/Aeltumn/Chisels-and-Bits-2/releases)!

### 1.14
The 1.14 versions are playable but lack many key features, they use placeholder textures and have severe performance issues. These versions offer a demo experience showing off what 1.14 C&B looks like they are not a good representation of the final C&B2 mod.

The table below shows the recommended and latest versions for Minecraft 1.14 with links to the download page:

| Type | Version |
| ------------- | ------------- |
| Recommended  | [0.5.2a](https://github.com/Aeltumn/Chisels-and-Bits-2/releases/tag/0.5.2a)  |
| Latest  | [0.5.2a](https://github.com/Aeltumn/Chisels-and-Bits-2/releases/tag/0.5.2a) |

### 1.15
C&B2 for 1.15 is currently in development, there are no playable builds yet!

Differences from the original mod
--------------
Chisels & Bits 2 is an reimagining of the original mod, C&B2 will still be about chiseling, sculpting and designing custom blocks but with every old feature tweaked or improved and many brand new features that were never in the original mod.

Contributing
--------------
If you'd like to contribute something to Chisels & Bits, you're free to do so. However, not all changes will be accepted. If you're unsure if your suggestion fits the mod, [open an issue](https://github.com/Aeltumn/Chisels-and-Bits-2/issues) to discuss it first!

Compilation
--------------
1) Clone this repository and check out the branch of the version you want to build. (master is 1.14)
2) Load it into an IDE of your choice and import the project.
3) Run `genIntellijRuns`, `genVSCodeRuns` or `genEclipseRuns` depending on the IDE you use.
4) Run `runData` to generate all model, recipe and advancement files.
5) Run `build` using gradle
6) You'll find the built jar in the `/build/libs/` folder.

Extra steps if you want to setup a C&B development environment:

7) Create an empty file called `settings.gradle` file and enter the path to the built C&B jar to it as key "gradle.ext.buildJar". The file should contain something similar to this:
```gradle.ext.buildJar = "C:\\Chisels-and-Bits-2\\build\\libs\\chiselsandbits2-0.5.2a.jar"```
8) Run `genIntellijRuns`, `genVSCodeRuns` or `genEclipseRuns` depending on the IDE you use.
9) Run `runClient` to start the mod.