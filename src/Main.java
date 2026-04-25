import cpu.Z80;
public class Main {
    public static void main(String[] args){ 
        try{
             Z80 z80 = new Z80(args[0]);
        }catch(Exception e){
            System.out.println("Erro na leitura do arquivo");
        }
       















        /* uns testes ai, nada importante :p 
        LinkedList something = new LinkedList();

        something.addNode(2);
        something.addNode(3);
        something.addNode(100);

        something.printAll();

        something.removeFirstOccurance(3);
        something.printAll();

        Stack somethingStack = new Stack( 10 );
        somethingStack.printTop();
        somethingStack.push(5);
        somethingStack.printTop();
        somethingStack.push(3);
        somethingStack.push(-1);
        somethingStack.push(9);
        somethingStack.printTop(); */

    }
}
