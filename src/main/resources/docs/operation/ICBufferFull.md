**ICBufferFull** is a general HotSpot VM operation.

It is also represented as an **empty VM operation**:

[Source code see VM_ICBufferFull](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/vmOperations.hpp)

The important part is the comment: this safepoint is triggered because the JVM needs to deal with
**inline cache buffer pressure**.
This is part of HotSpot runtime and JIT machinery, not a GC-specific operation.

So if you see **ICBufferFull** in the safepoint log, it usually means the JVM needed a safepoint
to process inline-cache-related state.

## Why it may be slow

1. **Reaching the safepoint is slow.** As with other small VM operations, this is often the main reason.
2. **The application is generating a lot of dynamic call-site activity.** Heavy compilation, deoptimization, or inline cache updates can make this operation appear more often.
3. **There is general runtime pressure in the code cache / compilation subsystem.** The operation itself is small, but the need for it may become more frequent under heavy JIT activity.
