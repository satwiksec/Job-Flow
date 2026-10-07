/** Small command-line check used during development; not needed to run the UI. */
public class SchedulerCheck {
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        scheduler.addJob(new Job("J1", "Server Failure", 1, 0, 4));
        scheduler.addJob(new Job("J2", "Database Backup", 2, 1, 3));
        scheduler.addJob(new Job("J3", "System Update", 2, 2, 3));
        scheduler.addJob(new Job("J4", "Report Generation", 3, 3, 4));
        scheduler.addJob(new Job("J5", "Data Cleanup", 4, 4, 2));
        if (!scheduler.runScheduler()) throw new AssertionError("Sample jobs should schedule");
        String order = "";
        for (Job job : scheduler.getExecutionOrder()) order += job.getId();
        if (!"J1J2J3J4J5".equals(order)) throw new AssertionError("Unexpected order: " + order);
        if (Math.abs(scheduler.averageWaiting() - 5.0) > .001) throw new AssertionError("Incorrect waiting average");
        if (Math.abs(scheduler.averageTurnaround() - 8.2) > .001) throw new AssertionError("Incorrect turnaround average");
        if (Math.abs(scheduler.averageResponse() - 5.0) > .001) throw new AssertionError("Incorrect response average");
        System.out.println("Scheduler check passed.");
    }
}
