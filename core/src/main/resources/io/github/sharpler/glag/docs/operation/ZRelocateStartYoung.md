**ZRelocateStartYoung** is the pause that starts young-generation relocation work in a **ZGC** collection.

[Source code see VM_ZRelocateStartYoung](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zGeneration.cpp)

Before this pause, ZGC has finished young marking and selected the young pages that should be relocated.
During the pause, it synchronizes with Java threads and installs the state needed to continue moving objects
concurrently.

In the ZGC logs this usually appears near:

1. `y: Concurrent Select Relocation Set`,
2. `y: Pause Relocate Start`,
3. `y: Concurrent Relocate`,
4. `y: Young Generation ... -> ...`.

So **ZRelocateStartYoung** should be read as
**"ZGC is switching selected young pages into concurrent relocation"**.

##### Why it may be slow

1. **The relocation set is large.** More selected pages require more forwarding and relocation metadata.
2. **Promotion or tenuring work is involved.** Young relocation can include deciding what survives and what should move
   toward older generations.
3. **Allocation pressure is high.** If the application keeps allocating aggressively, ZGC may be starting relocation
   while the heap has little free headroom.

##### When it is suspicious

1. **It is much larger than other ZGC coordination pauses.** Relocation start should usually be a short transition into
   concurrent work.
2. **It appears in rapid bursts.** That usually means the young generation is being cycled very aggressively.
