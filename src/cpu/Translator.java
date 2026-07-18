package cpu;
import java.util.*;

public class Translator {
    Map<String, Integer> fixedInstructionsTable = new HashMap<>();
    Map<String, Integer> registerTable = new HashMap<>();
    Map<String, Integer> registerPairTable = new HashMap<>();

    public Translator(){
        initializefixedInstructionsTable();
        initializeRegisterTable();
        initializeRegisterPairTable();
    }

    private void initializefixedInstructionsTable(){
        fixedInstructionsTable.put( "NOP", 0x00 );
        fixedInstructionsTable.put( "HALT", 0x76 );
        fixedInstructionsTable.put( "JP", 0xC3 );
        fixedInstructionsTable.put( "JR", 0x18 );
        fixedInstructionsTable.put( "CALL", 0xCD );
        fixedInstructionsTable.put( "RET", 0xC9 );
    }   

    private void initializeRegisterTable(){  
        registerTable.put("A", 0b111 );
        registerTable.put("B", 0b000 );
        registerTable.put("C", 0b001 );
        registerTable.put("D", 0b010 );
        registerTable.put("E", 0b011 );
        registerTable.put("H", 0b100 );
        registerTable.put("L", 0b101 );
        registerTable.put("(HL)", 0b110);
    }

    private void initializeRegisterPairTable(){
        registerPairTable.put("BC", 0);
        registerPairTable.put("DE", 1);
        registerPairTable.put("HL", 2);
        registerPairTable.put("AF", 3);
    }



    public static class TranslationResult {
        public final List<Byte> bytes;
        public final Integer relocOffset;
        public final String labelRef;
        public final boolean containsHalt;

        public TranslationResult(List<Byte> bytes, Integer relocOffset, String labelRef, boolean containsHalt) {
            this.bytes = bytes;
            this.relocOffset = relocOffset;
            this.labelRef = labelRef;
            this.containsHalt = containsHalt;
        }
    }

    public TranslationResult mnemonicsToBinary(String mnemonic, Map<String, Integer> symbolTable, int currentPc){
        String[] tokens = mnemonic.split( "\\s+", 2 );
        ArrayList<Byte> binary_opcode = new ArrayList<>();
        String[] temp;
        int opcode = 0;
        Integer relocOffset = null;
        String labelRef = null;
        boolean containsHalt = false;

        switch(tokens[0]){
            case "LD": { // LD HL, r e LD r, HL implementados
                temp = tokens[1].split( "," );

                if ( temp.length != 2 ) {
                    throw new IllegalArgumentException("LD no formato errado! LD r, r' ou LD r, n");
                }

                int destiny = registerTable.get( temp[0].trim() );
                int origin;

                if ( !isNumber( temp[1].trim() ) ) {
                    origin = registerTable.get( temp[1].trim() ); // LD r, r'
                } else {
                    opcode = 0b00000000 + (destiny << 3 ) + 0b110; // LD r, n
                    binary_opcode.add( ( byte ) opcode );
                    binary_opcode.add( ( byte ) Integer.parseInt( temp[1].trim() ) );
                    break;
                }

                opcode = 0b01000000 + ( destiny << 3 ) + origin;
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "ADD": {
                temp = tokens[1].split( "," );

                if ( temp.length != 2 || !(temp[0].trim().equals("A"))) {
                    throw new IllegalArgumentException("ADD no formato errado! ADD A, r'");
                }                
                
                int operand = registerTable.get( temp[1].trim() );

                opcode = 0b10000000 + operand;
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "SUB": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("SUB não está no formato esperado! (SUB r)");
                }

                int operand = registerTable.get( temp[0].trim() );

                opcode = 0b10010000 + operand;
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "INC": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("INC não está no formato esperado! (INC r)");
                }

                int operand = registerTable.get( temp[0].trim() );

                opcode = 0b00000100 + (operand << 3);
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "DEC": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("DEC não está no formato esperado! (DEC r)");
                }

                int operand = registerTable.get( temp[0].trim() );

                opcode = 0b00000101 + (operand << 3);
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "AND": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("AND não está no formato esperado! (AND r)");
                }

                int operand = registerTable.get( temp[0].trim() );

                opcode = 0b10100000 + operand;
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "OR": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("OR não está no formato esperado! (OR r)");
                }

                int operand = registerTable.get( temp[0].trim() );

                opcode = 0b10110000 + operand;
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "XOR": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("XOR não está no formato esperado! (XOR r)");
                }

                int operand = registerTable.get( temp[0].trim() );

                opcode = 0b10101000 + operand;
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "CP": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("CP não está no formato esperado! (CP r)");
                }

                int operand = registerTable.get( temp[0].trim() );

                opcode = 0b10111000 + operand;
                binary_opcode.add( ( byte ) opcode );
                break;
            }
            case "JP": {
                temp = tokens[1].split( "," );
                String labelOrAddress = temp[0].trim();
                int targetAddress = 0;



                if(temp.length != 1){
                    throw new IllegalArgumentException("JP não está no formato esperado! (JP rr)");
                }

                binary_opcode.add( ( byte ) 0xC3 );
                relocOffset = binary_opcode.size(); 

                if ( isNumber(labelOrAddress) ) {
                    targetAddress = Integer.parseInt(labelOrAddress);
                } else {
                    labelRef = labelOrAddress;
                    if ( symbolTable.containsKey(labelOrAddress) ) {
                        targetAddress = symbolTable.get(labelOrAddress);
                    } else {
                        targetAddress = 0;
                    }
                }
                binary_opcode.add((byte) (targetAddress & 0xFF)); //low byte
                binary_opcode.add((byte) ((targetAddress >> 8) & 0xFF)); //high byte
                break;
            }
            case "JR": {
              
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("JR não está no formato esperado! (JR offset ou JR label)");
                }

                String operand = tokens[1].trim();
                int offset;

                if (symbolTable.containsKey(operand)) {
                    int target = symbolTable.get(operand);

                    // PC após ler uma instrução JR fica 2 bytes à frente
                    offset = target - (currentPc + 2);

                } else if (isNumber(operand)) {
                    offset = Integer.parseInt(operand);
                } else {
                    throw new IllegalArgumentException("Label não encontrada: " + operand);
                }

                if (offset < -128 || offset > 127) {
                    throw new IllegalArgumentException("Offset do JR fora do intervalo (-128 a 127).");
                }

                binary_opcode.add((byte) 0x18);
                binary_opcode.add((byte) offset);
                break;
            }

            case "CALL": {
                temp = tokens[1].split( "," );
                String labelOrAddress = temp[0].trim();
                int targetAddress = 0;

                binary_opcode.add((byte) 0xCD);
                relocOffset = binary_opcode.size();

                if ( isNumber(labelOrAddress) ) {
                    targetAddress = Integer.parseInt(labelOrAddress);
                } else {
                    labelRef = labelOrAddress;
                    if (symbolTable.containsKey(labelOrAddress)) {
                        targetAddress = symbolTable.get(labelOrAddress);
                    } else {
                        targetAddress = 0;
                    }
                }
                binary_opcode.add((byte) (targetAddress & 0xFF));
                binary_opcode.add((byte) ((targetAddress >> 8) & 0xFF));
                
                break;
            }
            case "RET": {
                if ( tokens.length != 1 ) {
                    throw new IllegalArgumentException("RET não está no formato esperado! (RET)");
                }
                binary_opcode.add( ( byte ) 0xC9 );
                
                break;
            }
            case "PUSH": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("PUSH não está no formato esperado! (PUSH qq)");
                }

                String pair = temp[0].trim();

                if(!registerPairTable.containsKey(pair)){
                    throw new IllegalArgumentException("Par de registradores inválido: " + pair);
                }

                int qq = registerPairTable.get(pair);

                opcode = 0xC5 + (qq << 4);

                binary_opcode.add((byte) opcode);
                break;
            }
            case "POP": {
                temp = tokens[1].split( "," );
                if ( temp.length != 1 ) {
                    throw new IllegalArgumentException("POP não está no formato esperado! (POP qq)");
                }

                String pair = temp[0].trim();

                if(!registerPairTable.containsKey(pair)){
                    throw new IllegalArgumentException("Par de registradores inválido: " + pair);
                }

                int qq = registerPairTable.get(pair);

                opcode = 0xC1 + (qq << 4);

                binary_opcode.add((byte) opcode);
                break;
            }
            case "NOP": {
                if ( tokens.length != 1 ) {
                    throw new IllegalArgumentException("NOP não está no formato esperado! (NOP)");
                }

                binary_opcode.add( ( byte ) 0x00 );
                break;
            }
            case "HALT": {
                if ( tokens.length != 1 ) {
                    throw new IllegalArgumentException("HALT não está no formato esperado! (HALT)");
                }
                binary_opcode.add( ( byte ) 0x76 );
                containsHalt = true;
                break;
            }
            default:
                throw new IllegalArgumentException("Instrução não existente");
        }

        return new TranslationResult(binary_opcode, relocOffset, labelRef, containsHalt);
    }

    public boolean isNumber( String a ) {
        try {
            Integer.parseInt( a );
            return true;
        } catch ( NumberFormatException e ) {
            return false;
        }
    }

    public int getInstructionSize(String mneumonic){
        String[] tokens = mneumonic.split("\\s+", 2);
        String op = tokens[0].toUpperCase();

        switch(op) {
            case "JR":
                return 2;

            case "LD":
                if(tokens.length > 1){
                    String[] args = tokens[1].split(",");
                    if(args.length == 2 && isNumber(args[1].trim())){
                        return 2; // LD r, n
                    }
                }
                return 1; //LD r, r'
            
                case "JP":
                    return 3;
                case "CALL":
                    return 3;
                
                default:
                    return 1; //todas as outras
        }
    }


}
