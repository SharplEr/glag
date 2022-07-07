**ICBufferFull** is general JVM operation.

[Source code:](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/vmOperations.hpp)

```C++
// empty vm op, when forcing a safepoint due to inline cache buffers being full
class VM_ICBufferFull: public VM_EmptyOperation {
 public:
  VMOp_Type type() const { return VMOp_ICBufferFull; }
};
```

