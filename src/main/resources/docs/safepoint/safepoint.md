Safepoint is a point where it's safe to stop thread execution and then do operations with the java heap
like move objects and free memory.

For example, it's **unsafe** to stop thread when object already allocated but reference still not assigned.

Safepoint is just read operation from special page in memory.

When JVM want to do something under the pause it protects this page from reading which create trap:
when thread trying to read the page it stops and get signal from OS.
JVM catches this signal.
Eventually all thread stops and JVM could do its operation.

So there are two phases: reaching safepoint and do JVM operation.

Single GC iteration could be split into many safe points so GC could do concurrent operations between them.

While concurrent operations effect on application throughput (because of CPU consuming),
safepoints effect on latency.

Here [source code of safepoints logging:](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/safepoint.cpp)

```C++
log_info(safepoint)(
   "Safepoint \"%s\", "
   "Time since last: " JLONG_FORMAT " ns, "
   "Reaching safepoint: " JLONG_FORMAT " ns, "
   "At safepoint: " JLONG_FORMAT " ns, "
   "Total: " JLONG_FORMAT " ns",
    VM_Operation::name(_current_type),
    _last_app_time_ns,
    _last_safepoint_cleanup_time_ns - _last_safepoint_begin_time_ns,
    _last_safepoint_end_time_ns     - _last_safepoint_cleanup_time_ns,
    _last_safepoint_end_time_ns     - _last_safepoint_begin_time_ns
   );
```
