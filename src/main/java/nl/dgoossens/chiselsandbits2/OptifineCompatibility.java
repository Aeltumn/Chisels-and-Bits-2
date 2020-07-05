package nl.dgoossens.chiselsandbits2;

import java.lang.reflect.Method;

/**
 * A class that offers methods used to be compatible with optifine.
 */
public class OptifineCompatibility {
    private static boolean initialised;
    private static Method usingShaders;

    private static void initialise() {
        initialised = true;
        try {
            Class configClass = Class.forName("net.optifine.Config");
            usingShaders = configClass.getMethod("isShaders");
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Returns true if any kind of shaders are currently enabled.
     */
    public static boolean isUsingShaders() {
        if (!initialised) initialise();
        try {
            return (boolean) usingShaders.invoke(null);
        } catch (Exception exe) {
            exe.printStackTrace();
            return false;
        }
    }
}
