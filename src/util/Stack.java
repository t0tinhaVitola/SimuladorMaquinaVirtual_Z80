package util;
public class Stack {
    private final int maxSize;
    private int[] myStack;
    private int top;

    public Stack( int size ) {
        this.maxSize = size;
        this.myStack = new int[maxSize];
        this.top = -1;
    }

    public void push( int value ) {
        if ( top < maxSize - 1 ) {
            top++;
            myStack[top] = value;
        } else {
            System.out.println( "Pilha cheia '-'" );
        }
    }

    public void pop() {
        if ( top >= 0 ) {
            top--;
        } else {
            System.out.println( "Pilha vazio '- '" );
        }
    }

    public void printTop () {
        if ( this.top == -1 ) {
            System.out.println("Vazio");
            return;
        }

        System.out.println(this.myStack[this.top]);
    }
}