package nl.dgoossens.chiselsandbits2;

import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LibraryFinder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads every single C&B class by force.
 *
 * Forge's new 1.13+ classloader has this fun thing
 * where the order of classloading is somehow not deterministic so
 * on some start-ups classes are loaded by the rendering
 * thread but by the main thread on other start-ups.
 * (even though both threads have the same class loader)
 *
 * This has the effect that ~30% of startups crash completely but
 * 70% of the time nothing goes wrong.
 *
 * To address this issue I force java to load every single class
 * in C&B.
 * It's not clean, but it works.
 */
public class ClassLoadEnforcer {
    public static void loadAllClasses() {
        Class<?> jarFileClass = ClassLoadEnforcer.class;
        String classPath = "nl/dgoossens/chiselsandbits2/";
        File f;

        //Development Environment
        if(System.getProperty("development") != null) {
            //For the develop environment we use the ouptut jar
            if(System.getProperty("chiselsandbits2.buildJar") == null)
                throw new UnsupportedOperationException("Please fill in the settings.gradle file!");

            f = new File(System.getProperty("chiselsandbits2.buildJar"));
        } else {
            //Actual jar in mods folder
            f = FMLLoader.getLoadingModList().getModFileById(ChiselsAndBits2.MOD_ID).getFile().getFilePath().toFile();
        }

        try {
            JarFile jar = new JarFile(f);
            Enumeration<JarEntry> contents = jar.entries();
            URLClassLoader loader = new URLClassLoader(new URL[] { new URL("jar:file:"+f+"!/") }, jarFileClass.getClassLoader());
            while(contents.hasMoreElements()) {
                JarEntry je = contents.nextElement();
                if(je.isDirectory() || !je.getName().endsWith(".class") || !je.getName().startsWith(classPath)) continue;
                try {
                    //Attempt to load the class by force.
                    loader.loadClass(je.getName().substring(0, je.getName().length() - 6).replace('/', '.'));
                } catch(NoClassDefFoundError ignored) {}
            }
            loader.close();
            jar.close();
        } catch(Exception x) {
            x.printStackTrace();
        }
    }
}
