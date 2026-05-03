**ZMarkStartYoungAndOld** is the initial-mark pause for a **ZGC** major collection that starts both young and old
marking work.

[Source code see VM_ZMarkStartYoungAndOld](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zGeneration.cpp)

A major ZGC collection includes young-generation work and old-generation work.
This pause prepares marking state for both parts of the cycle, after which ZGC continues the expensive marking phases
concurrently.

In the ZGC logs this usually appears near:

1. `Major Collection`,
2. `Y: Young Generation`,
3. `Y: Pause Mark Start (Major)`,
4. `Y: Concurrent Mark`,
5. later `O: Old Generation` and `O: Concurrent Mark`.

So **ZMarkStartYoungAndOld** should be read as
**"ZGC is starting a major collection and preparing concurrent marking for young and old generations"**.

##### Why it may be slow

1. **Both generations need coordination.** The pause prepares more collector state than a young-only mark start.
2. **Old-generation pressure is present.** Major collections usually mean old live data or allocation pressure is
   important enough to involve the old generation.
3. **Root setup is expensive.** More application threads and runtime roots can make the initial stop-the-world setup
   larger.

##### When it is suspicious

1. **It appears often.** Frequent major collection starts are a stronger signal than frequent young collections.
2. **It is much larger than `ZMarkStartYoung`.** Some increase is expected, but a large gap points at old-generation or
   root-processing cost.
