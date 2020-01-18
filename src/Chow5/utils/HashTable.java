package Chow5.utils;

public class HashTable<T> {
    LinkedList<T>[] table;
    int size;
    public HashTable(int size) {
        table = new LinkedList[size];
        this.size = size;
        for (int i = size; --i>= 0; ) {
            table[i] = new LinkedList<T>();
        }
    }
    public void add(T obj) {
        int index = obj.hashCode() % this.size;
        System.out.println(table + " adding " + obj + " to index " + index +" with table sizE:" + this.size);
        table[index].add(obj);
    }
    public boolean contains(T obj) {
        int index = obj.hashCode() % this.size;
        return table[index].contains(obj);
    }
    public boolean remove(T obj) {
        int index = obj.hashCode() % this.size;
        return table[index].remove(obj);
    }
}
