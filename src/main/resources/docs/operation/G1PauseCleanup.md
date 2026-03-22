**G1PauseCleanup**

[Source code](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/g1/g1VMOperations.cpp):

```C++
void VM_G1PauseCleanup::work() {
  G1CollectedHeap* g1h = s::heap();
  g1h->concurrent_mark()->cleanup();
}
```
