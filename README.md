# Priority Job Scheduler

A second-year DSA project that demonstrates **non-preemptive priority scheduling** using a manually implemented, array-based binary heap. No `java.util.PriorityQueue` is used.

## Run

```bash
javac -d out src/*.java
java -cp out Main
```

## Project map

- `Job.java` — job data, status and calculated scheduling metrics.
- `CustomPriorityQueue.java` — the custom array heap with `insert`, `peek`, `removeHighestPriority`, and heap ordering.
- `Scheduler.java` — scheduling logic and calculated results.
- `Dashboard.java` — polished Swing desktop interface.
- `Main.java` — application entry point.

