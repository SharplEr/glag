**ZMarkStartYoung** is the initial-mark pause for a young-generation **ZGC** collection.

[Source code see VM_ZMarkStartYoung](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zGeneration.cpp)

This pause starts a young marking cycle.
ZGC briefly stops Java threads, prepares marking state, records the initial root state, and then continues the expensive
marking work concurrently.

In the ZGC logs this usually appears near:

1. `Minor Collection`,
2. `y: Young Generation`,
3. `y: Pause Mark Start`,
4. `y: Concurrent Mark`.

So **ZMarkStartYoung** should be read as **"ZGC is starting concurrent marking for a young collection"**.

##### Why it may be slow

1. **Root setup is expensive.** More threads, stacks, JNI handles, or runtime roots can increase the startup pause.
2. **The young generation is under heavy allocation pressure.** A rushed collection can make coordination work more
   visible.
3. **Worker or marking state is being adjusted.** ZGC may need to prepare mark stripes and worker distribution before
   concurrent marking begins.

##### When it is suspicious

1. **It is large compared to `ZMarkEndYoung`.** Initial mark should usually be a short setup pause.
2. **It happens extremely often.** Frequent young mark starts usually point at allocation pressure or a heap size that is
   too small for the workload.
