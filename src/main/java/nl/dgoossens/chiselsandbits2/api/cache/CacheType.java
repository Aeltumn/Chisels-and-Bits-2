package nl.dgoossens.chiselsandbits2.api.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public enum CacheType {
    DEFAULT,
    MODEL,
    ROUTINE(175), //Every 175ms
    ;

    private final List<CacheClearable> clearable = new ArrayList<>();

    CacheType() {
    }

    CacheType(long repeating) {
        //Call this cache every repeating milliseconds.
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                call();
            }
        }, repeating, repeating);
    }

    /**
     * Register a class implementing {@link CacheClearable} to this cache.
     */
    public void register(CacheClearable c) {
        clearable.add(c);
    }

    /**
     * Clear all {@link CacheClearable} registered to this cache type.
     */
    public void call() {
        for (CacheClearable c : clearable)
            c.clearCache();
    }
}
