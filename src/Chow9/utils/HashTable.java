package Chow9.utils;

public class HashTable<T> {
    LinkedList<T>[] table;
    int capacity;
    int size = 0;
    public HashTable(int capacity) {
        table = new LinkedList[capacity];
        this.capacity = capacity;
        for (int i = capacity; --i>= 0; ) {
            table[i] = new LinkedList<T>();
        }
    }
    public void add(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        table[index].add(obj);
        size++;
    }
    public boolean contains(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        return table[index].contains(obj);
    }
    public boolean remove(T obj) {
        int index = (Math.abs(obj.hashCode())) % this.capacity;
        this.size--;
        return table[index].remove(obj);
    }
}
