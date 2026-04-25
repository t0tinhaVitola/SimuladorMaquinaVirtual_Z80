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
}
