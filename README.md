# Chisels & Bits 2
A reimagining of [AlgorithmX2's original Chisels & Bits mod](https://github.com/AlgorithmX2/Chisels-and-Bits). A Minecraft mod about chiseling, sculpting and designing custom blocks made of various materials, fluids or colours.

Download
--------------
The mod is currently unfinished and not fully playable. You can find a few old snapshot builds on the [GitHub releases page](https://github.com/Aeltumn/Chisels-and-Bits-2/releases), but these are not complete and not fully playable.

Differences
--------------
Chisels & Bits 2 is a reimagining of C&B that is separately developed. C&B2 will still be about chiseling, sculpting and designing custom blocks but implemented very differently.

Contributing
--------------
If you'd like to contribute something to Chisels & Bits 2, you're free to do so. Not all changes will be accepted. If you're unsure if your suggestion fits the mod, [open an issue](https://github.com/Aeltumn/Chisels-and-Bits-2/issues) to discuss it first!

Compilation
--------------
1) Clone this repository and check out the branch of the version you want to build. (master is updated whenever a new version is released on CurseForge)
2) Load it into an IDE of your choice and import the project.
3) Run `genIntellijRuns`, `genVSCodeRuns` or `genEclipseRuns` depending on the IDE you use.
4) Run `runData` to generate all model, recipe and advancement files. (run it a second time if it fails the first)
5) Run `build` to build the jar
6) You'll find the built jar in the `/build/libs/` folder.