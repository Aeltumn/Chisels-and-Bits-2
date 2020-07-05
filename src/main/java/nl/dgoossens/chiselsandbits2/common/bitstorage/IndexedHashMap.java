package nl.dgoossens.chiselsandbits2.common.bitstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a hashmap where the index of the keys are tracked.
 * Not fully finished yet.
 */
public class IndexedHashMap<K, V> {
    private Map<K, V> data = new HashMap<>();
    private List<K> index = new ArrayList<>();

    public List<K> keySet() {
        return index;
    }

    public K getAt(int index) {
        return this.index.get(index);
    }

    public Map<K, V> getMap() {
        return data;
    }

    public void add(K k, V v) {
        if (!index.contains(k)) index.add(k);
        data.put(k, v);
    }

    public void remove(K k) {
        index.remove(k);
        data.remove(k);
    }
}
