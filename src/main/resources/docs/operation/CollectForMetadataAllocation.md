**CollectForMetadataAllocation** is the general JVM operation used to GC.

[Source code](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/shared/gcVMOperations.cpp)

```C++
void VM_CollectForMetadataAllocation::doit() {
  SvcGCMarker sgcm(SvcGCMarker::FULL);

  CollectedHeap* heap = Universe::heap();
  GCCauseSetter gccs(heap, _gc_cause);

  // Check again if the space is available.  Another thread
  // may have similarly failed a metadata allocation and induced
  // a GC that freed space for the allocation.
  if (!MetadataAllocationFailALot) {
    _result = _loader_data->metaspace_non_null()->allocate(_size, _mdtype);
    if (_result != NULL) {
      return;
    }
  }

#if INCLUDE_G1GC
  if (UseG1GC && ClassUnloadingWithConcurrentMark) {
    G1CollectedHeap::heap()->start_concurrent_gc_for_metadata_allocation(_gc_cause);
    // For G1 expand since the collection is going to be concurrent.
    _result = _loader_data->metaspace_non_null()->expand_and_allocate(_size, _mdtype);
    if (_result != NULL) {
      return;
    }

    log_debug(gc)("G1 full GC for Metaspace");
  }
#endif

  // Don't clear the soft refs yet.
  heap->collect_as_vm_thread(GCCause::_metadata_GC_threshold);
  // After a GC try to allocate without expanding.  Could fail
  // and expansion will be tried below.
  _result = _loader_data->metaspace_non_null()->allocate(_size, _mdtype);
  if (_result != NULL) {
    return;
  }

  // If still failing, allow the Metaspace to expand.
  // See delta_capacity_until_GC() for explanation of the
  // amount of the expansion.
  // This should work unless there really is no more space
  // or a MaxMetaspaceSize has been specified on the command line.
  _result = _loader_data->metaspace_non_null()->expand_and_allocate(_size, _mdtype);
  if (_result != NULL) {
    return;
  }

  // If expansion failed, do a collection clearing soft references.
  heap->collect_as_vm_thread(GCCause::_metadata_GC_clear_soft_refs);
  _result = _loader_data->metaspace_non_null()->allocate(_size, _mdtype);
  if (_result != NULL) {
    return;
  }

  log_debug(gc)("After Metaspace GC failed to allocate size " SIZE_FORMAT, _size);

  if (GCLocker::is_active_and_needs_gc()) {
    set_gc_locked();
  }
}
```

This operation is trying to allocate memory in Metaspace and
retry on fails with metaspace expansion and collection.

It also could start concurrent class unloading on G1 GC.

You could read about metaspace more [here](https://poonamparhar.github.io/understanding-metaspace-gc-logs/).