**G1PauseCleanup** is a G1-specific stop-the-world cleanup operation near the end of a marking cycle.

[Source code see VM_G1PauseCleanup](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/g1/g1VMOperations.cpp)

This operation belongs to the **cleanup** part of the G1 marking cycle.
At this point G1 finalizes information produced by concurrent marking and prepares the next phase of reclaiming space.

In practical terms, this is part of the transition from "we have marked the heap"
to "we know which old regions are worth reclaiming next".

##### Why it may be slow

1. **The marking cycle produced a lot of bookkeeping work.** Large heaps and many regions increase cleanup effort.
2. **There are many candidate old regions to process.** Cleanup needs to finalize region-level information before mixed collections can proceed.
3. **Class unloading / reference processing around the end of marking is expensive.** Depending on workload, these activities may contribute to the overall pause around this phase.

For background:

1. [Oracle G1 guide describing Remark and Cleanup](https://docs.oracle.com/en/java/javase/14/gctuning/garbage-first-g1-garbage-collector1.html)
