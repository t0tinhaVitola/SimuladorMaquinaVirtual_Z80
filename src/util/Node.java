package util;
public class Node {
    private int data;
    private Node nextNode;

    public Node ( int newData ) {
        this.data = newData;
        this.nextNode = null;
    }

    public Node getNextNode ( ) {
        return this.nextNode;
    }

    public int getData () {
        return this.data;
    }

    public void setNextNode( Node newNode ) {
        this.nextNode = newNode;
    }

    public void setInt ( int newValue ) {
        this.data = newValue;
    }

}
