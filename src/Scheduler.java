import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Connects the job collection to the custom priority queue and scheduling calculations. */
public class Scheduler {
    private final List<Job> jobs = new ArrayList<Job>();
    private final List<Job> executionOrder = new ArrayList<Job>();
    private String currentJobName = "—";

    public List<Job> getJobs() { return jobs; }
    public List<Job> getExecutionOrder() { return executionOrder; }
    public String getCurrentJobName() { return currentJobName; }

    public boolean containsId(String id, Job excluded) {
        for (Job job : jobs) if (job != excluded && job.getId().equalsIgnoreCase(id)) return true;
        return false;
    }

    public void addJob(Job job) { jobs.add(job); resetSchedule(); }
    public void removeJob(Job job) { jobs.remove(job); resetSchedule(); }
    public void clearAll() { jobs.clear(); executionOrder.clear(); currentJobName = "—"; }

    public void resetSchedule() {
        executionOrder.clear();
        currentJobName = "—";
        for (Job job : jobs) job.clearSchedule();
    }

    /** Builds the actual current queue from jobs that have not completed. */
    public CustomPriorityQueue createQueueSnapshot() {
        CustomPriorityQueue queue = new CustomPriorityQueue();
        for (Job job : jobs) if (!Job.COMPLETED.equals(job.getStatus())) queue.insert(job);
        return queue;
    }

    /**
     * Non-preemptive priority scheduling: jobs that have arrived are placed in
     * our custom heap; each removal selects the next CPU job.
     */
    public boolean runScheduler() {
        if (jobs.isEmpty()) return false;
        resetSchedule();
        List<Job> arrivals = new ArrayList<Job>(jobs);
        Collections.sort(arrivals, new Comparator<Job>() {
            public int compare(Job a, Job b) {
                if (a.getArrivalTime() != b.getArrivalTime()) return a.getArrivalTime() - b.getArrivalTime();
                return a.getId().compareToIgnoreCase(b.getId());
            }
        });
        CustomPriorityQueue readyQueue = new CustomPriorityQueue();
        int time = 0, nextArrival = 0;
        while (nextArrival < arrivals.size() || !readyQueue.isEmpty()) {
            if (readyQueue.isEmpty() && nextArrival < arrivals.size() && time < arrivals.get(nextArrival).getArrivalTime()) {
                time = arrivals.get(nextArrival).getArrivalTime();
            }
            while (nextArrival < arrivals.size() && arrivals.get(nextArrival).getArrivalTime() <= time) {
                Job arrived = arrivals.get(nextArrival++);
                arrived.markReady();
                readyQueue.insert(arrived);
            }
            Job selected = readyQueue.removeHighestPriority();
            if (selected == null) continue;
            int start = time;
            time += selected.getBurstTime();
            selected.setScheduleTimes(start, time);
            executionOrder.add(selected);
            currentJobName = selected.getName();
        }
        return true;
    }

    public double averageWaiting() { return average(0); }
    public double averageTurnaround() { return average(1); }
    public double averageResponse() { return average(2); }
    private double average(int measure) {
        if (executionOrder.isEmpty()) return 0;
        double total = 0;
        for (Job job : executionOrder) {
            if (measure == 0) total += job.getWaitingTime();
            else if (measure == 1) total += job.getTurnaroundTime();
            else total += job.getResponseTime();
        }
        return total / executionOrder.size();
    }
}
