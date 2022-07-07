# glag

glag is a Java **G**C **l**og **ag**gregator.

## Short story about Java GC and safepoints

Java has garbage collector.

Any GC have to do some work under the application pause.

These pauses make effect on your application behave:
mostly add random latency, sometimes lead to troubles like connection break.

To pause your application Java uses safepoints.

When GC need such pause JVM protects special memory page from reading.

Reading of this page added in a lot of places of your application when it could be safely stopped  by JVM. 

When thread step into this trap JVM handle signal by OS and stop this thread.

Eventually all thread stops and GC could do his operation.

## Usage

You could use it to analize your gc and safepoint logs.

## How to run

```bash
mvn package
java -jar ./target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar -s /home/safepoint.log -g /home/gc.log
```
