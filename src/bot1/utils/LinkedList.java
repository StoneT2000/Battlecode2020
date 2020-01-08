package bot1.utils;

public class LinkedList<T> {
    public int size = 0;
    public Node head;
    public Node end;
    public LinkedList() {

    }
    public void add(T obj) {
        if (end != null) {
            Node newNode = new Node(obj);
            newNode.prev = end;
            end.next = newNode;
            end = newNode;
        }
        else {
            head = new Node(obj);
            end = head;
        }
        size++;
    }
    public void remove(Node node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        else {
            // deal with head
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        else {
            end = node.prev;
        }
        node = null;
        size--;
    }

}



