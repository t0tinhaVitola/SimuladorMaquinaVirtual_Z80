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
        fixedInstructionsTable.put("NOPE", 0x00 );
        fixedInstructionsTable.put("HALT", 0x76 );
    }   

    private void initializeRegisterTable(){  
        registerTable.put("AC", 0b111 );
        registerTable.put("R1", 0b000 );
        registerTable.put("R2", 0b001 );
        registerTable.put("R3", 0b010 );
        registerTable.put("R4", 0b011 );
        registerTable.put("R5", 0b100 );
        registerTable.put("R6", 0b101 );
    }

    public ArrayList<Byte> mnemonicsToBinary(String mnemonic){
        String[] tokens = mnemonic.split("\\s+");
        switch(tokens[0]){
            case "LD": 
            
            break;
        }
        return null; //so tirando erro do compilador, enchendo o saco
    }
}
