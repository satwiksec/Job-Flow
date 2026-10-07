/** A single CPU job and its scheduling details. */
public class Job {
    public static final String READY = "READY";
    public static final String WAITING = "WAITING";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";

    private final String id;
    private String name;
    private int priority;
    private int arrivalTime;
    private int burstTime;
    private String status = READY;
    private int startTime = -1;
    private int completionTime = -1;
    private int waitingTime;
    private int turnaroundTime;
    private int responseTime;

    public Job(String id, String name, int priority, int arrivalTime, int burstTime) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getPriority() { return priority; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public String getStatus() { return status; }
    public int getStartTime() { return startTime; }
    public int getCompletionTime() { return completionTime; }
    public int getWaitingTime() { return waitingTime; }
    public int getTurnaroundTime() { return turnaroundTime; }
    public int getResponseTime() { return responseTime; }

    public void update(String name, int priority, int arrivalTime, int burstTime) {
        this.name = name;
        this.priority = priority;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
    }

    public void markReady() { status = READY; }

    public void setScheduleTimes(int startTime, int completionTime) {
        this.startTime = startTime;
        this.completionTime = completionTime;
        turnaroundTime = completionTime - arrivalTime;
        waitingTime = turnaroundTime - burstTime;
        responseTime = startTime - arrivalTime;
        status = COMPLETED;
    }

    public void clearSchedule() {
        status = READY;
        startTime = -1;
        completionTime = -1;
        waitingTime = 0;
        turnaroundTime = 0;
        responseTime = 0;
    }

    public static String priorityLabel(int priority) {
        switch (priority) {
            case 1: return "CRITICAL";
            case 2: return "HIGH";
            case 3: return "MEDIUM";
            default: return "LOW";
        }
    }
}
