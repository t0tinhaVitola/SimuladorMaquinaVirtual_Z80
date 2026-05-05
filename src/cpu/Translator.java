package cpu;
import java.util.*;

public class Translator {
    Map<String, Integer> fixedInstructionsTable = new HashMap<>();
    Map<String, Integer> SymbolTable = new HashMap<>();
    Map<String, Integer> registerTable = new HashMap<>();

    public Translator(){
        initializefixedInstructionsTable();
        initializeRegisterTable();
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

    public ArrayList<Byte> mnemonicsToBinary( String mnemonic ){
        String[] tokens = mnemonic.split( "\\s+", 2 );
        ArrayList<Byte> binary_opcode = new ArrayList<>();
        String[] temp;
        int opcode = 0;
        switch( tokens[0] ){
            case "LD": { // LD HL, r e LD r, HL implementados
                temp = tokens[1].split( "," );

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
                
                if ( !temp[0].trim().equals( "A" ) )
                    break;
                
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
                binary_opcode.add( ( byte ) 0xC3 );
                //Aqui só está salvando o byte do opcode, não soube como lidar com o endereço para onde irá saltar.
                break;
            }
            case "JR": {
                temp = tokens[1].split( "$" );

                if ( !isNumber( temp[1] ) )
                    throw new IllegalArgumentException("JR não está no formato esperado! (JR offset)");
                
                int offset = Integer.parseInt( temp[1].trim() );
                if ( offset > 127 || offset < -128 ) {
                    throw new IllegalArgumentException("O offset está fora dos limites.");
                }

                binary_opcode.add( ( byte ) 0x18 );
                binary_opcode.add( ( byte ) offset );
                break;
            }

            case "CALL": {
                binary_opcode.add( ( byte ) 0xCD );
                //Aqui só está salvando o byte do opcode, não soube como lidar com o endereço para onde irá saltar.
                break;
            }
            case "RET": {
                binary_opcode.add( ( byte ) 0xC9 );
                //Aqui só está salvando o byte do opcode, não soube como lidar com o endereço para onde irá saltar.
                break;
            }
            case "PUSH": {
                //Não soube aplicar a lógica de par de registradores
                break;
            }
            case "POP": {
                //Não soube aplicar a lógica de par de registradores
                break;
            }
            case "NOP": {
                binary_opcode.add( ( byte ) 0x00 );
                break;
            }
            case "HALT": {
                binary_opcode.add( ( byte ) 0x76 );
                break;
            }
        }

        return binary_opcode; 
    }

    public boolean isNumber( String a ) {
        try {
            Integer.parseInt( a );
            return true;
        } catch ( NumberFormatException e ) {
            return false;
        }
    }
}
