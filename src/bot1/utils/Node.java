package bot1.utils;

public class Node<T> {
    public Node next;
    public Node prev;
    public T val;
    public Node(T obj) {
        val = obj;
    }
}
