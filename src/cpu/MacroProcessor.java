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

        List<String> output = new ArrayList<>();

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
                    parameters.add(tokens[i].replace( ",", "" ) );
                }

                parametersTable.put(currentMacro, parameters);

                readingHeader = false;
                continue;
            }
            if (readingMacro){
                if(line.equals("MCEND")){
                    macroTable.put(currentMacro, new ArrayList<>(body));
                    readingMacro = false;
                    continue;
                }
                body.add(line);
            }else{ // taquei tooooda a lógica da 2° passagem dentro do else e agora ele funciona em uma única passagem
                String[] tokens = line.trim().split("\\s+");

                if ( macroTable.containsKey( tokens[0] ) ) {
                    expandMacro( tokens, output );
                }else{
                    output.add(line);
                }
            }
        }
        return output;
    }

    private void expandMacro( String[] macroInfo, List<String> output ) {
        List<String> macroBody = macroTable.get(macroInfo[0]);
        List<String> formalParameters = parametersTable.get(macroInfo[0]);
        List<String> realParameters = new ArrayList<>();

        for(int i = 1; i < macroInfo.length; i++){
            realParameters.add( macroInfo[i].replace( ",", "") );
        }

        if ( realParameters.size() != formalParameters.size() ) {
            throw new IllegalArgumentException( "A quantidade de parâmetros de '" + macroInfo[0] + "'' está incorreta!" );
        }

        for( String instruction : macroBody ){
            String expanded = instruction;

            for(int i = 0; i < formalParameters.size( ); i++){
                expanded = expanded.replace(formalParameters.get(i), realParameters.get(i));
            }

            String[] tokens = expanded.trim().split("\\s+");
            if ( macroTable.containsKey( tokens[0] ) ) {
                expandMacro( tokens, output );
            }
            else {
                output.add( expanded );
            }
        }
    }
}
