**CleanClassLoaderDataMetaspaces** is a stop-the-world cleanup operation for class-loader metadata and metaspace
bookkeeping.

[Source code see VM_CleanClassLoaderDataMetaspaces](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/vmOperations.hpp)
[Source code see walk_metadata_and_clean_metaspaces](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/classfile/classLoaderDataGraph.cpp)

In practice, it means the JVM is cleaning metadata structures associated with class loaders,
usually after some classes or loaders became unreachable.

This is not specific to one collector, but it often appears near GC phases that finalize liveness information
and can now safely reclaim metadata-related state.

##### Why it may be slow

1. **Many classes or class loaders were created and discarded.** There is simply more metadata to process.
2. **Metaspace usage is high.** More metadata structures mean more bookkeeping work.
3. **Class unloading is active and expensive.** Applications with dynamic loading, proxies, or code generation often
   make this heavier.

##### When it is suspicious

1. **It appears often.** Frequent metadata cleanup may indicate heavy dynamic class generation or class-loader churn.
2. **It is large relative to other pauses.** Then metaspace or class unloading may be a noticeable part of pause
   latency.
