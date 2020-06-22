# Chisels & Bits 2
A successor to [AlgorithmX2's original Chisels & Bits](https://github.com/AlgorithmX2/Chisels-and-Bits) that had releases for Minecraft 1.8 through 1.12. A Minecraft mod about chiseling, sculpting and designing custom blocks made of various materials, fluids or colours.

Current state of development
--------------
I've taken a break from working on C&B2 for the last month and decided to change the way I'm going to work on the mod for now. Instead of trying to release 'functional' alpha builds on GitHub I'll be silently working on the mod.

I'll keep at it until the mod is ready for a CurseForge release and ready for modpack makers to add into their packs.

I'm going to be working on the mod and rewriting the entire rendering code that is currently a copy of C&B1. Rewriting all the rendering code will take a while but I'm hoping to get something out this year. I'll also be making changes that will make 1.14 C&B2 worlds crash when loaded in 1.15 but when a 1.15 build is finally ready it will hopefully have support for fluid chiseling, transparent blocks and none of the current rendering issues.

If you are interested in the current progress you can clone the 1.15.x branch, build the mod yourself and have a look.

~ Aeltumn (21/03/2020)

Download
--------------
Some old versions for Minecraft 1.14 are available on the [GitHub releases page](https://github.com/Aeltumn/Chisels-and-Bits-2/releases). These versions are not representitive of the final version of Chisels & Bits 2 as many features are missing or incomplete and textures used are not the final versions yet.

Differences from the original mod
--------------
Chisels & Bits 2 was remade almost completely and all features in the mod were re-evaluated and often reimplemented slightly differently. [You can read up on the original planned changes here.](DIFFERENCES.md)
You can also take a look at [the roadmap](ROADMAP.md) where you can read up on what features are going to be worked on next and what needs to be done.

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