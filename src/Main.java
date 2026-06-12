
public class Main {
    public static void main(String[] args) {
        // Lança a interface gráfica, passando o arquivo .asm se fornecido
        GUI.main(args);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Para compilar e executar:
//
//   javac -d bin src/Main.java src/GUI.java src/cpu/*.java src/util/*.java
//   java -cp bin Main src/test.asm
//
// Ou sem arquivo (usa o botão Carregar na janela):
//   java -cp bin Main
// ─────────────────────────────────────────────────────────────────────────────