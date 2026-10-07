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

## Viva points

- **Lower priority number wins**: CRITICAL (1), HIGH (2), MEDIUM (3), LOW (4).
- Equal priorities use **arrival time**, then job ID, for deterministic ordering.
- The heap puts the highest-priority job at array index `0`.
- Insertion uses **heapify up**; removal moves the final item to the root and uses **heapify down**.
- `TAT = Completion Time - Arrival Time`; `WT = TAT - Burst Time`; `RT = Start Time - Arrival Time`.
