**ShenandoahFinalMarkStartEvac** is the final-mark pause that completes marking and starts evacuation in **Shenandoah**.

[Source code see VM_ShenandoahFinalMarkStartEvac](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/shenandoah/shenandoahVMOperations.cpp)

At this point Shenandoah has already done most marking concurrently.
This pause finalizes the marking state and switches the collector into evacuation work.

So this is one of the most important stop-the-world transition points in a Shenandoah cycle.

##### Why it may be slow

1. **Concurrent marking left a lot of work to finish.** Then more bookkeeping remains for the pause.
2. **The heap has a lot of live data.** Finalizing liveness and preparing evacuation becomes heavier.
3. **Reference processing or related runtime work is expensive.** These activities can noticeably stretch the pause.

##### When it is suspicious

1. **It is large compared to `ShenandoahInitMark`.** That often means marking finalization is the heavier part of the
   cycle.
2. **It grows together with memory pressure.** Then the collector may be struggling to keep up concurrently.
