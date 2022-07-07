**G1CollectForAllocation** is JVM operation of G1 GC.

[Source code](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/g1/g1VMOperations.cpp):

```C++
void VM_G1CollectForAllocation::doit() {
  G1CollectedHeap* g1h = G1CollectedHeap::heap();

  if (should_try_allocation_before_gc() && _word_size > 0) {
    // An allocation has been requested. So, try to do that first.
    _result = g1h->attempt_allocation_at_safepoint(_word_size,
                                                   false /* expect_null_cur_alloc_region */);
    if (_result != NULL) {
      // If we can successfully allocate before we actually do the
      // pause then we will consider this pause successful.
      _gc_succeeded = true;
      return;
    }
  }

  GCCauseSetter x(g1h, _gc_cause);
  // Try a partial collection of some kind.
  _gc_succeeded = g1h->do_collection_pause_at_safepoint(_target_pause_time_ms);

  if (_gc_succeeded) {
    if (_word_size > 0) {
      // An allocation had been requested. Do it, eventually trying a stronger
      // kind of GC.
      _result = g1h->satisfy_failed_allocation(_word_size, &_gc_succeeded);
    } else if (g1h->should_upgrade_to_full_gc()) {
      // There has been a request to perform a GC to free some space. We have no
      // information on how much memory has been asked for. In case there are
      // absolutely no regions left to allocate into, do a full compaction.
      _gc_succeeded = g1h->upgrade_to_full_collection();
    }
  }
}
```

This operation is trying to allocate memory in heap and
retry on fails with heap collection which fallbacks to
stronger collection on every fail.

So if you see a huge pause here it probably means operation was degraded to full GC.

It could happen not only bacause of no free memory,
but also because of heap fragmentation.

Also, this operation could be triggered only for [collection without allocation](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/g1/g1CollectedHeap.cpp).

```C++
// Schedule a standard evacuation pause. We're setting word_size
// to 0 which means that we are not requesting a post-GC allocation.
VM_G1CollectForAllocation op(0,     /* word_size */
                             counters_before.total_collections(),
                             cause,
                             policy()->max_pause_time_ms());
```