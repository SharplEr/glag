**Cleanup** is general JVM operation.

[Source code:](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/runtime/vmOperations.hpp)

```C++
class VM_Cleanup: public VM_EmptyOperation {
 public:
  VMOp_Type type() const { return VMOp_Cleanup; }
};
```

