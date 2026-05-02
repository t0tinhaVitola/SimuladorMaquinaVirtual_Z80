import cpu.Z80;
public class Main {
    public static void main(String[] args){ 
        try{
             if(args.length == 0){
                System.out.println("Uso: java Main arquivo.asm");
                return;
             }
             Z80 z80 = new Z80(args[0]);

             z80.run();

             z80.display_registers();   
        }catch(Exception e){
            System.out.println("Erro na leitura do arquivo");
        }
       

    }
}
