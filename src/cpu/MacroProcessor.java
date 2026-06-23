package cpu;

import java.util.*;

public class MacroProcessor {
    Map<String, List<String>> macroTable = new HashMap<>();         //Instruções do corpo da macro
    Map<String, List<String>> parametersTable = new HashMap<>();    //Parâmetros da macro

    public List<String> process(List<String> source){
        boolean readingMacro = false;
        boolean readingHeader = false;
        String currentMacro = "";
        List<String> body = new ArrayList<>();
        List<String> intermediate = new ArrayList<>();

        //1a PASSAGEM
        for(String rawLine : source){
            String line = rawLine.split("#")[0].trim();
            if(line.isEmpty()){
                continue;
            }
            
            //Entrando no modo de definição de macro
            if(line.equals("MCDEFN")){
                readingMacro = true;
                readingHeader = true;
                body.clear();
                continue;
            }
            //Lê o cabeçalho
            if(readingHeader){
                String[] tokens = line.trim().split("\\s+");
                currentMacro = tokens[0];

                List<String> parameters = new ArrayList<>();

                for(int i = 1; i < tokens.length; i++){
                    parameters.add(tokens[i]);
                }

                parametersTable.put(currentMacro, parameters);

                readingHeader = false;
                continue;
            }
            if(readingMacro){
                if(line.equals("MCEND")){
                    macroTable.put(currentMacro, new ArrayList<>(body));
                    readingMacro = false;
                    continue;
                }
                body.add(line);
            }else{
                intermediate.add(line);
            }
        }


        //2a PASSAGEM
        List<String> output = new ArrayList<>();

        for(String line : intermediate){
            String[] tokens = line.trim().split("\\s+");

            if(macroTable.containsKey(tokens[0])){
                List<String> macroBody = macroTable.get(tokens[0]);
                List<String> formalParameters = parametersTable.get(tokens[0]);
                List<String> realParameters = new ArrayList<>();

                for(int i = 1; i < tokens.length; i++){
                    realParameters.add(tokens[i]);
                }

                for(String instruction : macroBody){
                    String expanded = instruction;

                    for(int i = 0; i < formalParameters.size(); i++){
                        expanded = expanded.replace(formalParameters.get(i), realParameters.get(i));
                    }

                    output.add(expanded);
                }
            }else{
                output.add(line);
            }
        }
        return output;
    }
}
