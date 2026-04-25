public class LinkedList {
    private Node head;

    public LinkedList () {
        this.head = null;
    }

    public void addNode ( int value ) {
        Node newNode = new Node( value );

        if ( head == null ) {
            head = newNode;
            return;
        }

        Node current = head;
        while ( current.getNextNode() != null ) {
            current = current.getNextNode();
        }

        current.setNextNode( newNode );
    }

    public void removeFirstOccurance ( int num ) {
        if ( this.head == null )
            return;
        
        Node temp = head;
        while ( temp.getNextNode() != null && temp.getNextNode().getData() != num ) {
            temp = temp.getNextNode();
        }

        if ( temp.getNextNode() == null )
            return;

        temp.setNextNode(temp.getNextNode().getNextNode());
    }

    public void printAll( ) {
        Node temp = head;
        while (temp != null ) {
            System.out.println(temp.getData());
            temp = temp.getNextNode();
        }
    }
}
