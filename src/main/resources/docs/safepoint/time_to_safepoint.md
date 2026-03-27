**Time to safepoint** is the time the JVM spends waiting for application threads to reach a point
where they can be safely stopped.

In practice this is the delay between "the JVM wants a safepoint now" and "all required Java threads
have actually arrived".

Usually this time is **well below 1 ms**.
If it becomes noticeably larger, it often means that some threads are taking too long to reach a safepoint poll.

Why can this happen?

1. **A very large number of threads.** The JVM has to wait until all relevant threads reach a safepoint.
2. **Long native or blocking sections.** A thread that spends a long time in JNI, a system call, or some other native path may delay safepoint entry.
3. **Long-running tight computation loops.** If code keeps executing useful work for a long time without reaching a safepoint check, other threads may already be waiting while that thread is still running.

The third case is especially important for CPU-bound code.
HotSpot tries to keep safepoint checks in loops, but it also tries to avoid too much overhead from checking too often.
In modern HotSpot this is related to **counted loop safepoints** and **loop strip mining**.
The exact behavior depends on JVM implementation details and options such as
`-XX:+UseCountedLoopSafepoints` and `-XX:LoopStripMiningIter`.
In current Oracle/OpenJDK HotSpot, the default `LoopStripMiningIter` value is **1000**,
which means the inner strip-mined loop may run up to 1000 iterations between outer-loop safepoint checks.

So if you see long **time to safepoint**, think less about the safepoint operation itself
and more about how quickly busy threads can notice and honor the stop request.

You can read more here:

1. [Java launcher options for JDK 21: UseCountedLoopSafepoints and LoopStripMiningIter](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html)
2. [OpenJDK JEP 312: Thread-Local Handshakes](https://openjdk.org/jeps/312)
