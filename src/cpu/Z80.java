package cpu;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import util.ObjectModule;

public class Z80 {
    public byte A, B, C, D, E, H, L, F;
    public int PC, SP, IX, IY;
    public boolean halted = false;

    private boolean hasBinary = false;
    private boolean hasMnemonic = false;

    public final int MEMSIZE = 65536;
    public byte[] MEM = new byte[MEMSIZE];

    // Source tracking
    public List<String> sourceLines = new ArrayList<>();
    public Map<Integer, Integer> pcToLine = new HashMap<>();   // PC address -> source line index
    public Map<Integer, Integer> lineToPc = new HashMap<>();   // source line index -> PC address

    Translator trans = new Translator();

    public Z80() {
        reset();
    }

    public void reset(){
        PC = 0; SP = 0xFFFF;
        A = B = C = D = E = H = L = F = 0;
        IX = IY = 0;
        halted = false;
        Arrays.fill(MEM, (byte) 0);
        hasBinary = hasMnemonic = false;
    }
    public void loadFiles(java.util.List<String> paths) throws Exception {
        reset();
        sourceLines.clear();
        pcToLine.clear();
        lineToPc.clear();
        List<ObjectModule> modules = new ArrayList<>();
        MacroProcessor mp = new MacroProcessor();
        for(String path : paths){
            List<String> sL = new ArrayList<>();
            try (BufferedReader buffer = new BufferedReader(new FileReader(path))){
                String line;
                while((line = buffer.readLine()) != null){
                    sL.add(line.replace("\r", ""));
                }
            }
            modules.add(Assemble(sL, mp));
        }

        Linker.LinkedProgram program = Linker.linkAbsolute(modules, 0);
        if(!program.hasHalt){
            throw new Exception("Programa não foi encerrado devidamente");
        }

        Linker.load(program, this);
    }
    public ObjectModule Assemble(List<String> sL, MacroProcessor mp) throws Exception{
        ObjectModule oM = new ObjectModule();
        sL = new ArrayList<>(mp.process(sL));
        oM.sourceLines = sL;

         //mapear labels
        int locationCounter = 0;
        for(String rawLine : sL){
            String line = rawLine.split("#")[0].trim();
            if(line.isEmpty()){
                continue;
            }
            if(line.contains(":")){
                String[] parts = line.split(":", 2);
                String labelName = parts[0].trim();

                oM.symbolTablePart.put(labelName, locationCounter);
                line = parts[1].trim();
            }
            if(!line.isEmpty()){
                locationCounter += trans.getInstructionSize(line);
            }
        }

        int instructionCounter = 0;
        for(int LineIndex = 0; LineIndex < sL.size(); LineIndex++){
            String line = sL.get(LineIndex).split("#")[0].trim();

            if(line.contains(":")){
                line = line.split(":", 2)[1].trim();
            }
            
            if(!line.isEmpty()){
                List<Byte> instructions = null;
                Integer relocOffsetInInstruction = null;
                String labelRef = null;
                String[] ev_currentString = line.split("\\s+");
                if ( verifyBinaryInstruction(ev_currentString[0]) == true ) {
                    if ( ( hasBinary && !hasMnemonic ) || ( !hasBinary && !hasMnemonic ) ) {
                        instructions = insertBinaryIntoByteList(line, oM);
                        if ( !hasBinary ) {
                            hasBinary = true;
                        }
                    } else {
                        throw new Exception("Não é aceito programas com mistura entre binário e mnemonico");
                    }
                } else {
                    if ( ( hasMnemonic && !hasBinary ) || ( !hasBinary && !hasMnemonic ) ) {
                        Translator.TranslationResult result  = trans.mnemonicsToBinary(line, oM.symbolTablePart, instructionCounter);
                        instructions = result.bytes;
                        relocOffsetInInstruction = result.relocOffset;
                        labelRef = result.labelRef;
                        if(result.containsHalt) oM.hasHalt = true;
                        hasMnemonic = true;
                    } else { 
                        throw new Exception("Não é aceito programas com mistura entre binário e mnemonico");
                    }
                }
                oM.lineToPc.put(LineIndex, instructionCounter);

                int instructionStart = instructionCounter;
                for(int i = 0; i < instructions.size(); i++){
                    oM.pcToLine.put(instructionCounter, LineIndex);
                    oM.code.add(instructions.get(i));
                    instructionCounter++;
                    if (instructionCounter >= 65536) {
                        throw new RuntimeException("Módulo ultrapassou o limite de memória");
                    }
                }
                if (labelRef != null) {
                    int absOffsetInModule = instructionStart + relocOffsetInInstruction;
                    if (oM.symbolTablePart.containsKey(labelRef)) {
                        oM.relocationTable.add(absOffsetInModule);
                    } else {
                        oM.externalReferences.put(absOffsetInModule, labelRef);
                    }
                }
            } else {
                oM.lineToPc.put(LineIndex, instructionCounter);
            }
        }
        return oM;
    }
    
    // Returns the source line currently being executed (based on PC)
    public int getCurrentSourceLine() {
        // Walk back from PC to find the latest known mapping
        for (int addr = PC; addr >= 0; addr--) {
            if (pcToLine.containsKey(addr)) return pcToLine.get(addr);
        }
        return -1;
    }

    byte getRegister(int code) {
        switch (code) {
            case 0b000: return B;
            case 0b001: return C;
            case 0b010: return D;
            case 0b011: return E;
            case 0b100: return H;
            case 0b101: return L;
            case 0b110: return MEM[getHLAddress()];
            case 0b111: return A;
            default: throw new RuntimeException("Registrador inválido: " + code);
        }
    }

    void setRegister(int code, byte value) {
        switch (code) {
            case 0b000: B = value; break;
            case 0b001: C = value; break;
            case 0b010: D = value; break;
            case 0b011: E = value; break;
            case 0b100: H = value; break;
            case 0b101: L = value; break;
            case 0b110: MEM[getHLAddress()] = value; break;
            case 0b111: A = value; break;
            default: throw new RuntimeException("Registrador inválido: " + code);
        }
    }

    int getHLAddress() {
        return ((H & 0xFF) << 8) | (L & 0xFF); // 11101010 00000000 - H
    }                                          // 00000000 11110010 - L

    // -------------------------------------------------------------------------
    // STEP: executes a single instruction and returns true if can continue
    // -------------------------------------------------------------------------
    public boolean step() {
        if (halted || PC >= MEMSIZE) {
            halted = true;
            return false;
        }

        int opcode = Byte.toUnsignedInt(MEM[PC++]);

        // --- Fixed instructions ---
        if (opcode == 0x00) return true;  // NOP
        if (opcode == 0x76) { halted = true; return false; } // HALT

        // --- JP addr (0xC3) ---
        if (opcode == 0xC3) {
            int lo = Byte.toUnsignedInt(MEM[PC++]);
            int hi = Byte.toUnsignedInt(MEM[PC++]);
            PC = (hi << 8) | lo;
            return true;
        }

        // --- JR offset (0x18) ---
        if (opcode == 0x18) {
            byte offset = MEM[PC++];
            PC += offset;
            return true;
        }

        // --- CALL addr (0xCD) ---
        if (opcode == 0xCD) {
            int lo = Byte.toUnsignedInt(MEM[PC++]);
            int hi = Byte.toUnsignedInt(MEM[PC++]);
            int target = (hi << 8) | lo;
            // Push return address
            MEM[--SP] = (byte)((PC >> 8) & 0xFF);
            MEM[--SP] = (byte)(PC & 0xFF);
            PC = target;
            return true;
        }

        // --- RET (0xC9) ---
        if (opcode == 0xC9) {
            int lo = Byte.toUnsignedInt(MEM[SP++]);
            int hi = Byte.toUnsignedInt(MEM[SP++]);
            PC = (hi << 8) | lo;
            return true;
        }

        // --- PUSH rp ---
        // BC=0xC5, DE=0xD5, HL=0xE5, AF=0xF5
        if (opcode == 0xC5 || opcode == 0xD5 || opcode == 0xE5 || opcode == 0xF5) {
            int hi, lo;
            switch (opcode) {
                case 0xC5: hi = B & 0xFF; lo = C & 0xFF; break;
                case 0xD5: hi = D & 0xFF; lo = E & 0xFF; break;
                case 0xE5: hi = H & 0xFF; lo = L & 0xFF; break;
                default:   hi = A & 0xFF; lo = F & 0xFF; break;
            }
            if (SP < 2) { System.out.println("Stack overflow!"); return false; }
            MEM[--SP] = (byte) hi;
            MEM[--SP] = (byte) lo;
            return true;
        }

        // --- POP rp ---
        // BC=0xC1, DE=0xD1, HL=0xE1, AF=0xF1
        if (opcode == 0xC1 || opcode == 0xD1 || opcode == 0xE1 || opcode == 0xF1) {
            if (SP > 0xFFFD) { System.out.println("Stack underflow!"); return false; }
            byte lo = MEM[SP++];
            byte hi = MEM[SP++];
            switch (opcode) {
                case 0xC1: C = lo; B = hi; break;
                case 0xD1: E = lo; D = hi; break;
                case 0xE1: L = lo; H = hi; break;
                default:   F = lo; A = hi; break;
            }
            return true;
        }

        // --- ADD A, r ---
        if ((opcode & 0b11111000) == 0b10000000) {
            int regCode = opcode & 0b111;
            int valA = A & 0xFF;
            int valR = getRegister(regCode) & 0xFF;
            int res = valA + valR;
            F = 0;
            if ((res & 0xFF) == 0) F |= 0x40; // Zero flag
            if (res > 255) F |= 0x01;          // Carry flag
            A = (byte) res;
            return true;
        }

        // --- SUB r ---
        if ((opcode & 0b11111000) == 0b10010000) {
            int regCode = opcode & 0b111;
            int valA = A & 0xFF;
            int valR = getRegister(regCode) & 0xFF;
            int res = valA - valR;
            F = 0x02; // N flag (subtração)
            if ((res & 0xFF) == 0) F |= 0x40;
            if (valR > valA) F |= 0x01;
            A = (byte) res;
            return true;
        }

        // --- INC r ---
        if ((opcode & 0b11000111) == 0b00000100) {
            int regCode = (opcode >> 3) & 0b111;
            byte val = (byte)(getRegister(regCode) + 1);
            setRegister(regCode, val);
            if (val == 0) F |= 0x40; else F &= ~0x40;
            return true;
        }

        // --- DEC r ---
        if ((opcode & 0b11000111) == 0b00000101) {
            int regCode = (opcode >> 3) & 0b111;
            byte val = (byte)(getRegister(regCode) - 1);
            setRegister(regCode, val);
            F |= 0x02; // N flag
            if (val == 0) F |= 0x40; else F &= ~0x40;
            return true;
        }

        // --- AND r ---
        if ((opcode & 0b11111000) == 0b10100000) {
            int regCode = opcode & 0b111;
            A = (byte)(A & getRegister(regCode));
            F = 0x10; // H flag
            if (A == 0) F |= 0x40;
            return true;
        }

        // --- OR r ---
        if ((opcode & 0b11111000) == 0b10110000) {
            int regCode = opcode & 0b111;
            A = (byte)(A | getRegister(regCode));
            F = 0;
            if (A == 0) F |= 0x40;
            return true;
        }

        // --- XOR r ---
        if ((opcode & 0b11111000) == 0b10101000) {
            int regCode = opcode & 0b111;
            A = (byte)(A ^ getRegister(regCode));
            F = 0;
            if (A == 0) F |= 0x40;
            return true;
        }

        // --- CP r ---
        if ((opcode & 0b11111000) == 0b10111000) {
            int regCode = opcode & 0b111;
            int valA = A & 0xFF;
            int valR = getRegister(regCode) & 0xFF;
            int res = valA - valR;
            F = 0x02;
            if ((res & 0xFF) == 0) F |= 0x40;
            if (valR > valA) F |= 0x01;
            return true;
        }

        // --- LD r, r' ---
        if ((opcode & 0b11000000) == 0b01000000) {
            int src = opcode & 0b111;
            int dst = (opcode >> 3) & 0b111;
            setRegister(dst, getRegister(src));
            return true;
        }

        // --- LD r, n ---
        if ((opcode & 0b11000111) == 0b00000110) {
            int regCode = (opcode >> 3) & 0b111;
            byte value = MEM[PC++];
            setRegister(regCode, value);
            return true;
        }

        return true; // Unknown opcode: skip
    }

    // Run until HALT
    public void run() {
        while (!halted) {
            if (!step()) break;
        }
    }

    public void display_registers() {
        System.out.printf("A=%02X B=%02X C=%02X D=%02X E=%02X H=%02X L=%02X F=%02X%n",
            A & 0xFF, B & 0xFF, C & 0xFF, D & 0xFF, E & 0xFF, H & 0xFF, L & 0xFF, F & 0xFF);
        System.out.printf("PC=%04X SP=%04X IX=%04X IY=%04X%n", PC, SP, IX, IY);
    }

    

    public boolean verifyBinaryInstruction( String line ){
        if ( line == null || line.length() != 8 ) {
            return false;
        }

        for ( int i = 0; i < line.length(); i++ ) {
            if ( line.charAt(i) != '1' && line.charAt(i) != '0' ) {
                return false;
            }
        }
        return true;
    }

    public Byte stringToByte ( String line ) {
        int result = Integer.parseInt( line, 2 );

        return (byte) result;
    }

    public List<Byte> insertBinaryIntoByteList ( String line, ObjectModule oM ) {
        List <Byte> binary_opcode = new ArrayList<>();

        String[] newCoolString = line.split("\\s+");
        for ( int i = 0; i < newCoolString.length; i++ ) {
            binary_opcode.add( stringToByte( newCoolString[i]) );
            if ( newCoolString[i].equals( "01110110" )) {
                oM.hasHalt = true;
            }
        }
        
        return binary_opcode;
    }
}