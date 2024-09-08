package net.bowen.system;

import java.util.HashSet;
import java.util.Set;

public abstract class Deleteable {
    private final static Set<Deleteable> INSTANCES = new HashSet<>();

    public Deleteable(boolean shouldCollectAndDeleteAtOnce) {
        if (shouldCollectAndDeleteAtOnce)
            INSTANCES.add(this);
    }

    public static void deleteCreatedInstances() {
        for (Deleteable instance : INSTANCES) {
            instance.delete();
        }
    }

    protected abstract void delete();
}
