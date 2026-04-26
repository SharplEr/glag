**ShenandoahInitMark** is the initial-mark pause of a **Shenandoah** marking cycle.

In practice, it means:

1. Shenandoah is starting a new concurrent marking cycle,
2. the JVM performs a short stop-the-world pause,
3. after that, most of the marking work continues concurrently.

So this is one of the normal coordination pauses of Shenandoah.

##### Why it may be slow

1. **The heap is large or busy.** Even short coordination pauses can grow when there is more GC state to prepare.
2. **Marking is starting under pressure.** If free space is already tight, the whole cycle tends to be heavier.
3. **Root processing is expensive.** More threads, references, or runtime structures can increase pause cost.

##### When it is suspicious

1. **It is much larger than other Shenandoah coordination pauses.** That may indicate root-processing or heap-pressure
   problems.
2. **It happens very often.** Then the workload may be forcing marking cycles too aggressively.
