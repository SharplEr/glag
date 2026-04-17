**Serial GC** is the simplest HotSpot garbage collector.

It uses a **single GC thread** for most of its work and is primarily oriented toward
small heaps and simple deployments where pause times are not the main concern.

Serial GC is **generational**:

1. young generation collections reclaim short-lived objects,
2. old generation collections reclaim longer-lived objects.

When Eden fills up, the JVM performs a **stop-the-world minor collection**.
Live objects are copied out of Eden, some survive into Survivor space,
and some may be promoted to the old generation.

If the old generation becomes too full, Serial GC performs a **stop-the-world full GC**.
That pause is usually much more expensive than a young collection because it scans and compacts
more memory and still does all the work with a single GC thread.

In practice this collector is usually chosen when:

1. the application is small,
2. only one CPU core is really available,
3. simplicity matters more than peak throughput or low latency.

The main drawback is straightforward:
all meaningful GC work happens during **stop-the-world pauses**,
and those pauses can grow quickly with heap size.

You can read more here:

1. [Oracle Serial GC guide for JDK 25](https://docs.oracle.com/en/java/javase/25/gctuning/available-collectors.html)
2. [Oracle GC tuning guide overview](https://docs.oracle.com/en/java/javase/25/gctuning/introduction-garbage-collection-tuning.html)
