package org.polydevs.opusmc.bootstrap;

public interface ClassTransformer {
    String id();

    int priority();

    byte[] transform(String className, byte[] originalBytecode) throws Exception;

    /** Built-ins keep adapter routing; managed wrappers override this for raw targets. */
    default byte[] transform(String rawName, String routedName, byte[] originalBytecode) throws Exception {
        return transform(routedName, originalBytecode);
    }
}

