package farmbot1.utils;

public class LinkedList<T> {
    public int size = 0;
    public Node head;
    public Node end;
    public LinkedList() {

    }
    public void add(T obj) {
        if (end != null) {
            Node newNode = new Node(obj);
            end.next = newNode;
            end = newNode;
        }
        else {
            head = new Node(obj);
            end = head;
        }
    }

}
class Node<T> {
    public Node next;
    public T val;
    public Node(T obj) {
        val = obj;
    }
}


