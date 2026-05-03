**ShenandoahFinalUpdateRefs** is the final pause of the reference-update phase in **Shenandoah**.

[Source code see VM_ShenandoahFinalUpdateRefs](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/shenandoah/shenandoahVMOperations.cpp)

Shenandoah tries to do most update-reference work concurrently,
but this pause is used to finish the phase consistently before the JVM proceeds.

So this is another normal stop-the-world coordination pause of Shenandoah,
usually near the end of a collection cycle.

##### Why it may be slow

1. **Many references still need final processing.** Then more work remains for the pause.
2. **The heap contains many live objects.** More references usually mean more update work.
3. **Concurrent progress fell behind.** If the runtime could not keep enough work off the pause path, the final pause
   grows.

##### When it is suspicious

1. **It becomes large relative to the rest of the cycle.** Then update-reference work may be a major source of latency.
2. **It grows together with object graph size.** That usually points to live-set complexity rather than raw allocation
   rate.
