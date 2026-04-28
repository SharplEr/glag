**Parallel GC** is a throughput-oriented HotSpot garbage collector.

Like Serial GC, it is **generational** and does its meaningful collection work during
**stop-the-world pauses**, not through mostly concurrent phases.
Unlike Serial GC, it uses **multiple GC worker threads** during those collections.

The main idea is simple:
accept stop-the-world pauses, but try to finish them faster by doing more work in parallel.

At a high level:

1. **young collections** reclaim short-lived objects in parallel,
2. **old generation collections** may also use multiple threads,
3. the collector aims to maximize overall application throughput rather than minimize pause latency.

This collector is often a good fit for batch-style workloads,
CPU-rich environments, and services where occasional larger pauses are acceptable.

Compared to G1, Shenandoah, or ZGC, Parallel GC usually has a simpler pause model:
it does not try to be mostly concurrent.
Instead, it tries to make **stop-the-world collections** efficient.

That usually means:

1. better throughput than more latency-sensitive collectors in some workloads,
2. but less predictable and potentially longer pauses as the heap grows.

If your application can tolerate noticeable pauses and mainly cares about work completed per unit of time,
Parallel GC is often the collector to compare against first.

You can read more here:

1. [Oracle Parallel GC guide for JDK 25](https://docs.oracle.com/en/java/javase/25/gctuning/parallel-collector1.html)
2. [Oracle available collectors overview](https://docs.oracle.com/en/java/javase/25/gctuning/available-collectors.html)
