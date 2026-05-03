**Shenandoah GC** is a low-pause HotSpot garbage collector designed to keep pause times
less sensitive to heap size.

It is a **mostly concurrent** collector.
The key idea is to move a large part of heap reclamation work out of long stop-the-world pauses
and into concurrent phases that run while application threads continue to work.

In broad terms Shenandoah tries to:

1. mark live objects concurrently,
2. evacuate objects concurrently,
3. update references concurrently or with small pauses around the transition points.

The normal Shenandoah cycle is easiest to read as a sequence of short coordination VM operations
with concurrent work between them:

1. **ShenandoahInitMark** starts a concurrent marking cycle.
2. Concurrent marking runs while Java threads continue.
3. **ShenandoahFinalMarkStartEvac** finishes marking and starts evacuation.
4. Evacuation moves objects mostly concurrently.
5. **ShenandoahInitUpdateRefs** starts the update-references phase when that phase is needed.
6. Reference updates run mostly concurrently.
7. **ShenandoahFinalUpdateRefs** finishes the update-references phase.

So if a Shenandoah report shows several small VM operations for one collection cycle,
that is usually expected: the VM operations are the stop-the-world transition points,
not the whole amount of GC work.

That makes Shenandoah very different from throughput-oriented collectors such as Parallel GC.
The tradeoff is usually:

1. lower pause times,
2. more concurrent GC work,
3. some throughput overhead compared to simpler collectors.

Compared to **G1**, Shenandoah usually targets lower and more heap-size-independent pauses,
but it often pays for that with more concurrent GC work and potentially lower throughput.

Shenandoah is especially attractive when:

1. heap sizes are large,
2. pause consistency matters,
3. the application is sensitive to long stop-the-world events.

Compared to **ZGC**, Shenandoah is solving a very similar problem: keeping pauses short on large heaps.
Both are low-latency collectors, but they use different internal mechanisms and may show different
throughput, memory overhead, and operational behavior on a given workload.

Even though Shenandoah is mostly concurrent, it is **not pause-free**.
You still see short pauses for phase transitions and coordination,
but the design goal is to avoid long pauses that scale directly with heap size.

If you see long Shenandoah-related pauses in logs, that usually deserves attention,
because the collector is explicitly trying to keep them small.

You can read more here:

1. [Oracle Shenandoah guide for JDK 25](https://docs.oracle.com/en/java/javase/25/gctuning/shenandoah-garbage-collector.html)
2. [OpenJDK Shenandoah project page](https://openjdk.org/projects/shenandoah/)
