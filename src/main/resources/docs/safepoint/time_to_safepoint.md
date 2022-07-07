Time while JVM waiting until threads reach safepoint call "time to safepoint".

During this time some threads keep doing calculation while some others don't.

Usialy time to safepoint is very minor about 0.2 ms.

But JVM couldn't put safepoint too often because extra reading operation effect application throughput.