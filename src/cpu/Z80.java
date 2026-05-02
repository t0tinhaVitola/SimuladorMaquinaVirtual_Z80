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

    void setRegister(int code, byte value){
        switch(code){
            case 0b000: B = value; break;
            case 0b001: C = value; break;
            case 0b010: D = value; break;
            case 0b011: E = value; break;
            case 0b100: H = value; break;
            case 0b101: L = value; break;
            case 0b111: A = value; break;
            default: throw new RuntimeException("Invalid Register");
        }
    }

    public void run(){
        while (true){
            int opcode = Byte.toUnsignedInt(MEM[PC++]);
            
            //instrucoes fixas 
            switch(opcode){
                case 0x00: break; //NOP
                case 0x76: 
                    return; //HALT
            }

            //instrucoes genericas

            //ADD A,r
            if((opcode & 0b11111000) == 0b10000000){
                int regCode = opcode & 0b111; 
                int valA = A & 0xFF;
                int valR = getRegister(regCode) & 0xFF;
                int res = valA + valR;

                F = 0;
                if((res & 0xFF) == 0) F |= 0x40;
                if(res > 255) F |= 0x01;

                A = (byte) res;
            }

            //SUB B
            if((opcode & 0b11111000) == 0b10010000){
                int regCode = opcode & 0b111;
                int valA = A & 0xFF;
                int valR = getRegister(regCode) & 0xFF;
                int res = valA - valR;

                F = 0x02;
                if((res & 0xFF) == 0) F |= 0x40;
                if(valR > valA) F |= 0x01;

                A = (byte) res;
            }

            //LD r, r'
            if((opcode & 0b11000000) == 0b01000000){
                int src = opcode & 0b111;
                int dst = (opcode >> 3) & 0b111;

                setRegister(dst, getRegister(src));
            }

            //LD r, n
            if((opcode & 0b11000111) == 0b00000110){
                int regCode = (opcode >> 3) & 0b111;
                byte value = MEM[PC++];

                setRegister(regCode, value);
            }
        }
    }

    public void display_registers(){
        System.out.printf("\nA: %d", A);
        System.out.printf("\nB: %d", B);
        System.out.printf("\nC: %d", C);
        System.out.printf("\nD: %d", D);
        System.out.printf("\nE: %d", E);
        System.out.printf("\nH: %d", H);
        System.out.printf("\nL: %d", L);
        System.out.printf("\nF: %d", F);
        System.out.printf("\n\nPC: %d", PC);
    }
}
