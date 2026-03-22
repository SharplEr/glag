**G1TryInitiateConcMark**

[Source code](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/g1/g1VMOperations.cpp):

```C++
void VM_G1TryInitiateConcMark::doit() {
  G1CollectedHeap* g1h = G1CollectedHeap::heap();

  GCCauseSetter x(g1h, _gc_cause);

  // Record for handling by caller.
  _terminating = g1h->concurrent_mark_is_terminating();

  if (_terminating && GCCause::is_user_requested_gc(_gc_cause)) {
    // When terminating, the request to initiate a concurrent cycle will be
    // ignored by do_collection_pause_at_safepoint; instead it will just do
    // a young-only or mixed GC (depending on phase).  For a user request
    // there's no point in even doing that much, so done.  For some non-user
    // requests the alternative GC might still be needed.
  } else if (!g1h->policy()->force_concurrent_start_if_outside_cycle(_gc_cause)) {
    // Failure to force the next GC pause to be a concurrent start indicates
    // there is already a concurrent marking cycle in progress.  Set flag
    // to notify the caller and return immediately.
    _cycle_already_in_progress = true;
  } else if ((_gc_cause != GCCause::_wb_breakpoint) &&
             ConcurrentGCBreakpoints::is_controlled()) {
    // WhiteBox wants to be in control of concurrent cycles, so don't try to
    // start one.  This check is after the force_concurrent_start_xxx so that a
    // request will be remembered for a later partial collection, even though
    // we've rejected this request.
    _whitebox_attached = true;
  } else {
    _gc_succeeded = g1h->do_collection_pause_at_safepoint();
    assert(_gc_succeeded, "No reason to fail");
  }
}
```

This operation is trying to start concurrent mark cycle if it's okay to start one.
