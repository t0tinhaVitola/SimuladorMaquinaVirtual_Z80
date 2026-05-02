package cpu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;



public class Z80{
    byte AC, R1, R2, R3, FLAG, R4, R5, R6;
    int PC, SP, IX, IY;
    final int MEMSIZE = 65536;
    byte MEM[] = new byte[MEMSIZE];
    Translator trans = new Translator();
    

    public Z80(String fileAdress) throws Exception{
        //SP, IX, IY?
        PC   = 0;
        AC   = 0;
        R1   = 0;
        R2   = 0;
        R3   = 0;
        R4   = 0;
        R5   = 0;
        R6   = 0;
        FLAG = 0;
        
        fillMemoryWithInstructions(fileAdress);
    }


    private void fillMemoryWithInstructions(String fileAdress) throws Exception{
        try(BufferedReader buffer = new BufferedReader(new FileReader(fileAdress))){
            String line;
            int instructionCounter = 0;
            while((line = buffer.readLine()) != null){
                String mnemonics = line.split("#")[0].trim();
                if(mnemonics.isEmpty()) continue;
                List<Byte> instructions = trans.mnemonicsToBinary(mnemonics);

                for(int i = 0; i < instructions.size(); i++){
                    MEM[instructionCounter++] = instructions.get(i);
                    if(instructionCounter >= MEMSIZE){
                        throw new RuntimeException("Text file surpassed the max instruction count");
                    }
                }
            }
        }
    }

    private void executeLdAn(){
        byte n = MEM[PC + 1];

        this.AC = n;

        PC += 2;

        System.out.println("LD A, " + (n & 0xFF) + " executado. Novo AC: " + (AC & 0xFF));
    }

    private void executeLdBn(){
        byte n = MEM[PC + 1];
        this.R1 = n;
        PC += 2;
        System.out.println("LD B, " + (n & 0xFF) + " executado. Novo B: " + (R1 & 0xFF));
    }

    private void executeAddAB(){
        int valA = this.AC & 0xFF;
        int valB = this.R1 & 0xFF;

        int resultado = valA + valB;

        this.FLAG = 0;

        if((resultado & 0xFF) == 0){
            this.FLAG |= 0x40;
        }

        if(resultado > 255){
            FLAG |= 0x01;
        }

        this.AC = (byte) resultado;

        this.PC++;

        System.out.println("ADD A, B realizado. AC agora é: " + (AC & 0xFF));
    }

    //Executa o SUB B = AC - B
    private void executeSubB(){
        int valA = this.AC & 0xFF;      //valA vai ser o AC
        int valB = this.R1 & 0xFF;      //valB vai ser o reg R1

        int resultado = valA - valB;

        this.FLAG = 0;
        this.FLAG |= 0x02;

        if((resultado & 0xFF) == 0){
            this.FLAG |= 0x40;
        }

        //flag de carry
        if(valB > valA){
            this.FLAG |= 0x01;
        }

        this.AC = (byte) (resultado & 0xFF);
        this.PC++;

        System.out.println("SUB B realizado. AC: " + (AC & 0xFF) + " Carry: " + (FLAG & 0x01));
    }

    public void run(){
        while(true){
            int opcode = MEM[PC] & 0xFF;

            if(opcode == 0x76){
                System.out.println("FIM (HALT).");
                break;
            }

            switch(opcode){
                case 0x80:
                    executeAddAB();
                    break;

                case 0x00:
                    PC++;
                    break;

                case 0x3E:
                    executeLdAn();
                    break;

                case 0x06:
                    executeLdBn();
                    break;

                case 0x90:
                    executeSubB();
                    break;

                default:
                    System.out.println("ERRO");
                    return;
            }
        }
    }
}
