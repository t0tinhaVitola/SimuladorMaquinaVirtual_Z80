package cpu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class Z80{
    byte A, B, C, D, E, H, L, F;
    int PC, SP, IX, IY;
    final int MEMSIZE = 65536;
    byte MEM[] = new byte[MEMSIZE];
    Translator trans = new Translator();
    

    public Z80(String fileAdress) throws Exception{
        //SP, IX, IY?
        PC   = 0;
        A    = 0;
        B    = 0;
        C    = 0;
        D    = 0;
        E    = 0;
        H    = 0;
        L    = 0;
        F    = 0;
        
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

    byte getRegister(int code){
        switch(code){
            case 0b000: return B;
            case 0b001: return C;
            case 0b010: return D;
            case 0b011: return E;
            case 0b100: return H;
            case 0b101: return L;
            case 0b111: return A;
            default: throw new RuntimeException("Invalid Register");
        }
    }

    public void run(){
        while (true){
            int opcode = Byte.toUnsignedInt(MEM[PC++]);
            
            //instrucoes fixas 
            switch(opcode){
                case 0x00: //NOP
                    break;
                
                case 0x76: //HALT 
                    return;

                case 0x3E: //LD A, n 
                    A = MEM[PC++];
                    break;
            }

            //instrucoes genericas

            //ADD A,r 
            if((opcode & 0b11111000) == 0b10000000){
                int regCode = opcode & 0b0000111;
                byte value = getRegister(regCode); 
                A += value;
            }
        }
    }
}
