import java.util.ArrayList;
import java.util.List;


public class CustomPriorityQueue {
    private Job[] heap;
    private int size;

    public CustomPriorityQueue() {
        heap = new Job[10];
    }

    public boolean isEmpty() { 
        return size == 0; 
    }
    public int size() {
         return size; 
        }
    public Job peek() { 
        return isEmpty() ? null : heap[0]; 
    }

    public void insert(Job job) {
        ensureCapacity();
        heap[size] = job;
        heapifyUp(size);
        size++;
    }

    public Job removeHighestPriority() {
        if (isEmpty()) return null;
        Job highest = heap[0];
        size--;
        heap[0] = heap[size];
        heap[size] = null;
        if (size > 0) heapifyDown(0);
        return highest;
    }

    private void heapifyUp(int child) {
        while (child > 0) {
            int parent = (child - 1) / 2;
            if (!comesBefore(heap[child], heap[parent])) break;
            swap(child, parent);
            child = parent;
        }
    }

    private void heapifyDown(int parent) {
        while (true) {
            int left = parent * 2 + 1;
            int right = parent * 2 + 2;
            int best = parent;
            if (left < size && comesBefore(heap[left], heap[best])) best = left;
            if (right < size && comesBefore(heap[right], heap[best])) best = right;
            if (best == parent) return;
            swap(parent, best);
            parent = best;
        }
    }

    private boolean comesBefore(Job first, Job second) {
        if (first.getPriority() != second.getPriority()) return first.getPriority() < second.getPriority();
        if (first.getArrivalTime() != second.getArrivalTime()) return first.getArrivalTime() < second.getArrivalTime();
        return first.getId().compareToIgnoreCase(second.getId()) < 0;
    }

    private void swap(int a, int b) {
        Job temp = heap[a]; heap[a] = heap[b]; heap[b] = temp;
    }

    private void ensureCapacity() {
        if (size < heap.length) return;
        Job[] expanded = new Job[heap.length * 2];
        System.arraycopy(heap, 0, expanded, 0, heap.length);
        heap = expanded;
    }

    public List<Job> orderedSnapshot() {
        CustomPriorityQueue copy = new CustomPriorityQueue();
        for (int i = 0; i < size; i++) copy.insert(heap[i]);
        List<Job> result = new ArrayList<Job>();
        while (!copy.isEmpty()) result.add(copy.removeHighestPriority());
        return result;
    }
}
