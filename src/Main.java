import cpu.Z80;
public class Main {
    public static void main(String[] args){ 
        try{
             Z80 z80 = new Z80(args[0]);
        }catch(Exception e){
            System.out.println("Erro na leitura do arquivo");
        }
       

    }
}
