**ShenandoahInitUpdateRefs** is the initial pause that starts the update-references phase in **Shenandoah**.

[Source code see VM_ShenandoahInitUpdateRefs](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/gc/shenandoah/shenandoahVMOperations.cpp)

After evacuation, Shenandoah needs object references to point consistently to the new locations.
This pause is the coordination step that starts that phase.

So this is a normal stop-the-world transition pause, not the bulk of the work itself.

##### Why it may be slow

1. **The cycle is already under pressure.** If evacuation was expensive, the next coordination pause may also grow.
2. **The heap has many relocated objects.** More relocation work usually implies more follow-up reference-update work.
3. **Runtime coordination is heavy.** More threads and more GC state can make the transition slower.

##### When it is suspicious

1. **It is large even when other Shenandoah pauses are small.** Then the update-reference transition itself deserves
   attention.
2. **It happens frequently.** Then cycles may be happening too often for the workload.
