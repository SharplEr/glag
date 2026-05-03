**ZRelocateStartOld** is the pause that starts old-generation relocation work in a **ZGC** major collection.

[Source code see VM_ZRelocateStartOld](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zGeneration.cpp)

Before this pause, ZGC has already selected old pages for relocation and may have remapped roots.
During the pause, it synchronizes with Java threads and installs the state needed to let relocation continue
concurrently.

In the ZGC logs this usually appears near:

1. `O: Concurrent Select Relocation Set`,
2. `O: Concurrent Remap Roots`,
3. `O: Pause Relocate Start`,
4. `O: Concurrent Relocate`.

So **ZRelocateStartOld** should be read as
**"ZGC is switching old-generation pages from selection/remap work into concurrent relocation"**.

##### Why it may be slow

1. **The old relocation set is large or complex.** More selected old pages require more relocation metadata to prepare.
2. **Root remapping was expensive.** This pause often follows old root-remap work, so pressure there can show up around
   the transition.
3. **Old-generation fragmentation or live data is high.** More old data to preserve and move can make relocation setup
   heavier.

##### When it is suspicious

1. **It is large compared to `ZRelocateStartYoung`.** Old relocation can be heavier, but large pauses undermine ZGC's
   low-latency goal.
2. **It coincides with frequent major collections.** Then the old generation may be unable to keep up with allocation or
   promotion pressure.
