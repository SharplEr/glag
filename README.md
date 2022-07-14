# glag

glag is a Java **G**C **l**og **ag**gregator.

## Use cases

Glag is not some console version of GC Easy
(with all respect to GC Easy).

The main goal to give you enough information about GC to understand
what you should do with your problems.

It also helps you find huge pauses in logs.
And aggregates some simple statistic.

But that's all.
There isn't any kind of AI which find problems and give you solutions.

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
java -jar ./target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar -s /home/safepoint.log -g /home/gc.log -o /home/report.md
```

After this you could convert `*.md` output file into `pdf` and read it as a book. 