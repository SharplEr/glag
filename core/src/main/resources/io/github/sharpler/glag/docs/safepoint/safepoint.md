A **safepoint** is a moment when the JVM can safely stop Java threads and perform
runtime work that requires the heap and thread stacks to be in a well-defined state.

Typical examples include moving objects, updating references, collecting stack information,
and performing some VM housekeeping work.

The key idea is stop threads only at places where the JVM knows the state is safe to inspect or modify.
For example, it would be unsafe to stop a thread in the middle of updating object references
if the JVM could observe a partially updated state.

In modern **HotSpot**, safepoints are reached through a **polling mechanism**.
Historically this is often explained as a read from a special polling page:
when the JVM wants threads to stop, it arms the poll, and a thread that hits the poll
blocks instead of continuing normal execution.

That high-level picture is still useful, but modern HotSpot is more nuanced than
"just a read from one protected page".
In particular, thread-local polling and **thread-local handshakes** were added in newer JDKs,
so the exact low-level mechanism depends on the operation and the HotSpot implementation.

From the point of view of GC log analysis, it is enough to think about two broad phases:

1. **Reaching safepoint**: the JVM has requested a safepoint and is waiting for threads to arrive.
2. **Time at safepoint**: all required threads are stopped and the JVM performs its work.

This distinction matters because the reasons for slowness are different.
If reaching the safepoint is slow, the problem is usually in application execution paths:
busy loops, long native sections, blocking calls, or simply many threads.
If time at safepoint is slow, the JVM operation itself is expensive.

A single GC activity may involve multiple safepoints with concurrent work in between.
So even if a collector is described as *concurrent*, some parts of its work are still performed
under stop-the-world pauses.

As a rough rule of thumb:

1. **Concurrent GC work** usually affects throughput more than latency.
2. **Safepoint pauses** are directly visible as latency spikes.

Current HotSpot safepoint logging usually looks like this:

```text
Safepoint "ICBufferFull", Time since last: 177611286 ns, Reaching safepoint: 69282 ns, Cleanup: 130048 ns, At safepoint: 8449 ns, Total: 207779 ns
```

For reading such logs, the most important fields are:

1. **Reaching safepoint**: how long the JVM waited for threads to stop.
2. **At safepoint**: how long the VM operation itself took while threads were stopped.
3. **Total**: the whole pause as observed by the application.

You can read more here:

1. [JEP 312: Thread-Local Handshakes](https://openjdk.org/jeps/312)
2. [Oracle troubleshooting guide: safepoint polling mechanism and signals](https://docs.oracle.com/en/java/javase/24/troubleshoot/handle-signals-and-exceptions.html)
3. [HotSpot discussion describing the polling-page mechanism](https://mail.openjdk.org/pipermail/hotspot-gc-dev/2009-April/000914.html)
