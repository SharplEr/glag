**SetNotifyJvmtiEventsMode** is a serviceability-related VM operation used when the JVM changes whether JVMTI events should be delivered.

[Source code see VM_SetNotifyJvmtiEventsMode](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/prims/jvmtiEnvBase.cpp)

At a high level this operation means:

1. A JVMTI agent, debugger, or profiling tool changed which JVM events it wants to receive.
2. The JVM performs a VM operation to apply that change consistently.
3. After that, HotSpot starts or stops delivering the requested notifications.

In practical terms, this operation is about **changing observability / debugging behavior**,
not about reclaiming memory or doing GC work.

You are most likely to see it when a process is being observed by tooling that uses JVMTI,
for example a debugger, a profiler, or some other diagnostic agent.

## Why it may be slow

1. **The pause includes reaching a safepoint.** For this kind of operation, a long pause is often more about stopping threads than about the body of the operation itself.
2. **A debugger or agent is actively changing event subscriptions.** Tooling-heavy workloads can cause more serviceability-related VM work.
3. **There are many Java threads.** Coordinating thread state for a VM-wide observability change tends to get more expensive in large processes.
4. **JVMTI-related bookkeeping is already active.** Breakpoints, single stepping, method entry/exit notifications, and similar features usually come with extra runtime overhead around this area.

This operation is usually expected to be small.
If it is slow, the main suspect is often **tooling/debugger pressure or time to safepoint** rather than expensive work inside the operation itself.

For background:

1. [Oracle JVMTI documentation](https://docs.oracle.com/en/java/javase/25/docs/specs/jvmti.html)
2. [OpenJDK Serviceability in HotSpot overview](https://openjdk.org/groups/hotspot/docs/Serviceability.html)
