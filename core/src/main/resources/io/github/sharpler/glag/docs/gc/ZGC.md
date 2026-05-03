**ZGC** is a low-latency HotSpot garbage collector designed to keep pause times very small,
even on large heaps.

ZGC is **concurrent** and **region-based**.
Its main design goal is not maximum throughput, but keeping stop-the-world pauses short and stable.

At a high level ZGC tries to do most expensive work concurrently:

1. marking,
2. relocating objects,
3. reclaiming memory.

In modern generational ZGC logs, the VM operations are mostly the short transitions around that concurrent work.
For a young collection the typical sequence is:

1. **ZMarkStartYoung** starts young-generation marking.
2. Young marking runs concurrently.
3. **ZMarkEndYoung** finishes young marking and lets ZGC choose what can be reclaimed or relocated.
4. ZGC selects the relocation set concurrently.
5. **ZRelocateStartYoung** starts young relocation.
6. Young relocation runs concurrently.

For a major collection, ZGC starts young and old marking together with **ZMarkStartYoungAndOld**.
The old-generation part then has its own later transitions:

1. **ZMarkEndOld** finishes old-generation marking after concurrent old marking.
2. ZGC processes non-strong references and selects old relocation candidates concurrently.
3. **ZRelocateStartOld** starts old-generation relocation.
4. Old relocation runs concurrently.

Application threads may still participate indirectly through barriers,
but the collector avoids long global pauses as a core design principle.

Compared to **G1**, ZGC usually targets smaller and more stable pauses,
especially as heap size grows, but it often pays for that with more concurrent work
and potentially lower raw throughput on some workloads.

This makes ZGC attractive when:

1. latency matters more than absolute throughput,
2. heap sizes are large,
3. long GC pauses are operationally expensive.

Compared to **Shenandoah**, ZGC is aimed at the same broad problem space: low-latency garbage collection
with most work done concurrently. In practice the choice between them is usually about workload fit,
JDK support expectations, operational familiarity, and the exact tradeoff between latency,
throughput, and overhead on your application.

Like Shenandoah, ZGC is not literally pause-free.
There are still short synchronization pauses,
but they are intended to stay small and not grow dramatically with heap size.

The tradeoff is usually:

1. better latency behavior,
2. more GC machinery and concurrent overhead,
3. potentially lower raw throughput than simpler collectors on some workloads.

If you are investigating safepoints and GC together, long ZGC pauses are often more suspicious
than long pauses under a throughput-oriented collector, because ZGC is specifically designed to avoid them.

You can read more here:

1. [Oracle ZGC guide for JDK 25](https://docs.oracle.com/en/java/javase/25/gctuning/z-garbage-collector.html)
2. [OpenJDK ZGC project page](https://openjdk.org/projects/zgc/)
