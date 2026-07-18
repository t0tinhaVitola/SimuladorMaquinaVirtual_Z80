package cpu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.ObjectModule;

public class Linker {

    public static class LinkedProgram {
        public List<Byte> code = new ArrayList<>();
        public Map<String, Integer> globalSymbolTable = new HashMap<>();
        public List<Integer> relocationTable = new ArrayList<>(); 
        public List<String> sourceLines = new ArrayList<>();
        public Map<Integer, Integer> pcToLine = new HashMap<>();
        public Map<Integer, Integer> lineToPc = new HashMap<>();
        public boolean hasHalt = false;
        public int baseAddress; 

        public int size() {
            return code.size();
        }
    }









    private static int[] assignBases(List<ObjectModule> modules, int startAdress){
        int moduleAmount = modules.size();
        int[] bases = new int[moduleAmount];
        int cursor = startAdress;
        for (int i = 0; i < moduleAmount; i++) {
            bases[i] = cursor;
            cursor += modules.get(i).size();
        }
        return bases;
    }

    private static Map<String, Integer> buildGlobalSymbolTable(List<ObjectModule> modules, int[] bases) throws Exception {
        Map<String, Integer> global = new HashMap<>();
        for (int i = 0; i < modules.size(); i++) {
            ObjectModule m = modules.get(i);
            for (Map.Entry<String, Integer> e : m.symbolTablePart.entrySet()) {
                if (global.containsKey(e.getKey())) {
                    throw new Exception("Símbolo duplicado entre módulos (todo label é global): " + e.getKey());
                }
                global.put(e.getKey(), bases[i] + e.getValue());
            }
        }
        return global;
    }

    private static void patchAddress(List<Byte> code, int offset, int base){
        int lo = code.get(offset) & 0xFF;
        int hi = code.get(offset + 1) & 0xFF;
        int value = ((hi << 8) | lo) + base;
        code.set(offset, (byte) (value & 0xFF));
        code.set(offset + 1, (byte) ((value >> 8) & 0xFF));
    }

    private static void writeAddress(List<Byte> code, int offset, int value) {
        code.set(offset, (byte) (value & 0xFF));
        code.set(offset + 1, (byte) ((value >> 8) & 0xFF));
    }

    private static void mergeModule(LinkedProgram out, ObjectModule m, List<Byte> moduleCode) {
        int startInCombined = out.code.size();
        int lineOffset = out.sourceLines.size();

        out.code.addAll(moduleCode);
        out.sourceLines.addAll(m.sourceLines);
        out.hasHalt |= m.hasHalt;

        for (Map.Entry<Integer, Integer> e : m.pcToLine.entrySet()) {
            out.pcToLine.put(startInCombined + e.getKey(), lineOffset + e.getValue());
        }
        for (Map.Entry<Integer, Integer> e : m.lineToPc.entrySet()) {
            out.lineToPc.put(lineOffset + e.getKey(), startInCombined + e.getValue());
        }
    }

    public static LinkedProgram linkAbsolute(List<ObjectModule> modules, int loadAdress) throws Exception{
        int[] bases = assignBases(modules, loadAdress);
        Map<String, Integer> globalTable = buildGlobalSymbolTable(modules, bases);

        LinkedProgram out = new LinkedProgram();
        out.globalSymbolTable = globalTable;
        out.baseAddress = loadAdress;

        for (int i = 0; i < modules.size(); i++) {
            ObjectModule m = modules.get(i);
            int base = bases[i];
            List<Byte> moduleCode = new ArrayList<>(m.code);

            for (int offset : m.relocationTable) {
                patchAddress(moduleCode, offset, base);
            }
            for (Map.Entry<Integer, String> e : m.externalReferences.entrySet()) {
                String label = e.getValue();
                if (!globalTable.containsKey(label)){
                    throw new Exception("Label não encontrada em nenhum módulo: " + label);
                }
                writeAddress(moduleCode, e.getKey(), globalTable.get(label));
            }

            mergeModule(out, m, moduleCode);
        }
        return out;
    }

    public static void load(Linker.LinkedProgram program, Z80 z80) {
        int loadAddress = program.baseAddress;

        for (int i = 0; i < program.code.size(); i++) {
            int addr = loadAddress + i;
            if (addr >= z80.MEMSIZE) {
                throw new RuntimeException("Programa ultrapassou o limite de memória");
            }
            z80.MEM[addr] = program.code.get(i);
        }

        z80.pcToLine = new HashMap<>(program.pcToLine);
        z80.lineToPc = new HashMap<>(program.lineToPc);
        z80.sourceLines = program.sourceLines;
        if(program.globalSymbolTable.containsKey("MAIN")){
            z80.PC = program.globalSymbolTable.get("MAIN");
        }else z80.PC = loadAddress;
    }
}
