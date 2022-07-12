Time while JVM waiting until threads reach safepoint call "time to safepoint".

During this time some threads keep doing calculation while some others don't.

Usialy time to safepoint is very minor about 0.2 ms.

But JVM couldn't put safepoint too often because extra reading operation effect application throughput.

Following situations could incress time to safepoint:

1. A huge number of threads. Because you have to wait them all.
2. Heavy native call, like JNI, system call or even intrinsic. Because safepoints exists only in Java code.
3. Heavy calculation inside `for` loops with huge number of iteration. Because JDK check safepoint in `for` loops only every 1000 iteration.