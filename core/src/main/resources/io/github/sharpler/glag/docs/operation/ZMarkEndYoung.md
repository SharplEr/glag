**ZMarkEndYoung** is the final-mark pause for a young-generation **ZGC** collection.

[Source code see VM_ZMarkEndYoung](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zGeneration.cpp)

At this point ZGC has already done most young marking concurrently.
This stop-the-world pause completes the young mark phase and lets the collector compute which young pages contain live
data, which pages can be reclaimed, and which pages should be considered for relocation.

In the ZGC logs this usually appears near:

1. `y: Concurrent Mark`,
2. `y: Pause Mark End`,
3. `y: Concurrent Mark Free`,
4. `y: Concurrent Reset Relocation Set`,
5. `y: Concurrent Select Relocation Set`.

So **ZMarkEndYoung** should be read as **"ZGC is finishing young marking before choosing what to relocate"**.

##### Why it may be slow

1. **Concurrent marking left work to finish.** More remaining mark state can stretch the final pause.
2. **There is a large young live set.** More live objects usually means more liveness bookkeeping at the transition.
3. **Remembered-set or root state is busy.** Cross-generation references and runtime roots can make young final marking
   less trivial.

##### When it is suspicious

1. **It dominates other ZGC pauses.** ZGC mark-end pauses are normally short coordination pauses.
2. **It grows together with allocation pressure.** That can mean young collections are being forced before concurrent
   work has enough time to complete comfortably.
