package util;
import java.util.*;

public class ObjectModule {
    public List<Byte> code = new ArrayList<>();
    public Map<String, Integer> symbolTablePart = new HashMap<>();

    public List<String> sourceLines = new ArrayList<>();
    public Map<Integer, Integer> pcToLine = new HashMap<>();
    public Map<Integer, Integer> lineToPc = new HashMap<>();

    public List<Integer> relocationTable = new ArrayList<>();
    public Map<Integer, String> externalReferences = new HashMap<>();


    public boolean hasHalt = false;


    public int size() {
        return code.size();
    }
}
