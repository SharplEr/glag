**ZMarkEndOld** is the final-mark pause for the old-generation part of a **ZGC** major collection.

[Source code see VM_ZMarkEndOld](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/z/zGeneration.cpp)

At this point ZGC has already done old-generation marking mostly concurrently.
This stop-the-world pause closes the old mark phase and lets ZGC move on to post-mark work such as
freeing mark state, processing non-strong references and selecting old pages for relocation.

In the ZGC logs this usually appears near:

1. `O: Concurrent Mark`,
2. `O: Pause Mark End`,
3. `O: Concurrent Mark Free`,
4. `O: Concurrent Process Non-Strong`,
5. `O: Concurrent Select Relocation Set`.

So **ZMarkEndOld** should be read as **"ZGC is finishing old-generation marking and committing the liveness
result for the next phases of a major collection"**.

##### Why it may be slow

1. **Old-generation root or liveness bookkeeping is expensive.** More live old objects and remembered-set state can
   increase the amount of work needed to finalize marking.
2. **Reference processing is about to become expensive.** This pause may be followed by costly non-strong-reference or
   class-unloading work.
3. **The collector is under memory pressure.** If old collections are running close to exhaustion, coordination pauses
   tend to become more visible.

##### When it is suspicious

1. **It is much larger than `ZMarkEndYoung`.** Old-generation final marking is expected to be heavier, but a large gap
   points at old live-set or metadata pressure.
2. **It appears very frequently.** Frequent old-generation mark-end pauses mean major collections are being started too
   often for the workload.
