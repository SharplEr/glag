**Cleanup** is a general HotSpot VM operation.

In current HotSpot it is represented as an **empty VM operation**:

[Source code see VM_EmptyOperation](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/vmOperations.hpp)

So this operation is better understood as **"run a safepoint for JVM cleanup-related work around the VM thread"**
than as a heavy operation with a lot of work in its own body.

If you see a long safepoint with operation name **Cleanup**, it usually does **not** mean that
`VM_Cleanup` itself is expensive.

##### Why it may be slow

1. **Reaching the safepoint is slow.** Threads take too long to stop.
2. **The work attributed to safepoint cleanup is expensive.** In the safepoint log this is reflected in the `Cleanup` field, not in the tiny `VM_Cleanup` class itself.
3. **The JVM is under general runtime pressure.** Many threads, heavy JNI/native activity, or other VM housekeeping can make such pauses more visible.
