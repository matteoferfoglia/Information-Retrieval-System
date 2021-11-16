package it.units.informationretrieval.ir_boolean_model.utils;

import java.util.AbstractMap;
import java.util.Map;

public class Pair<K, V> extends AbstractMap.SimpleEntry<K,V> {
    public Pair(K key, V value) {
        super(key, value);
    }

    public Pair(Map.Entry<? extends K, ? extends V> entry) {
        super(entry);
    }
}