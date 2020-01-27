package Chow10.utils;

public class HashTable<T> {
    LinkedList<T>[] table;
    int capacity;
    public int size = 0;
    int tabIndex = 0;
    Node<T> curr = null;
    public HashTable(int capacity) {
        table = new LinkedList[capacity];
        this.capacity = capacity;
        for (int i = capacity; --i>= 0; ) {
            table[i] = new LinkedList<T>();
        }
    }
    public void add(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        // doesn't contain, add it
        if (!table[index].contains(obj)) {
            table[index].add(obj);
            size++;
        }
    }
    public boolean contains(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        return table[index].contains(obj);
    }
    public boolean remove(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        // contains it, remove it
        if (table[index].contains(obj)) {
            this.size--;
            return table[index].remove(obj);
        }
        return false;
    }
    public void resetIterator() {
        tabIndex = 0;
        curr = null;
    }
    public Node<T> next() {
        if (size != 0) {
            if (curr == null) {
                for (int i = table.length; --tabIndex >= 0; ) {
                    if (table[tabIndex].size != 0) {
                        curr = table[tabIndex].head;
                        return curr;
                    }
                }
                // no element left!
                return null;
            }
            else {
                // go to the next one
                curr = curr.next;
                return curr;
            }
        }
        else {
            return null;
        }

    }
}
