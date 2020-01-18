package rush.utils;

import Chow5.utils.Node;

public class LinkedList<T> {
    public int size = 0;
    public Chow5.utils.Node head;
    public Chow5.utils.Node end;
    public LinkedList() {

    }
    public void add(T obj) {
        if (end != null) {
            Chow5.utils.Node newNode = new Chow5.utils.Node(obj);
            newNode.prev = end;
            end.next = newNode;
            end = newNode;
        }
        else {
            head = new Chow5.utils.Node(obj);
            end = head;
        }
        size++;
    }
    public Chow5.utils.Node dequeue() {
        if (this.size > 0) {
            Chow5.utils.Node removed = head;
            remove(head);
            this.size--;
            return removed;
        }
        return null;
    }
    public boolean contains(T obj) {
        Chow5.utils.Node node = head;
        while (node != null) {
            if (node.val.equals(obj)) {
                return true;
            }
            node = node.next;
        }
        return false;
    }
    public void remove(Chow5.utils.Node node) {
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
    public boolean remove(T obj) {
        Node node = head;
        while (node != null) {
            if (node.val.equals(obj)) {
                remove(node);
                return true;
            }
            node = node.next;
        }
        return false;
    }

}



