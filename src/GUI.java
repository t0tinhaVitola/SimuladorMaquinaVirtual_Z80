import cpu.Z80;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class GUI extends JFrame {

    // vibe de computador antigo
    static final Color BG        = new Color(13, 17, 23);
    static final Color PANEL_BG  = new Color(22, 27, 34);
    static final Color GREEN     = new Color(80, 200, 120);
    static final Color GREEN_DIM = new Color(40, 100, 60);
    static final Color AMBER     = new Color(255, 180, 50);
    static final Color CYAN      = new Color(80, 180, 220);
    static final Color RED_FLAG  = new Color(220, 80, 80);
    static final Color BORDER    = new Color(40, 55, 70);
    static final Font  MONO      = new Font("Monospaced", Font.PLAIN, 13);
    static final Font  MONO_BIG  = new Font("Monospaced", Font.BOLD, 15);
    static final Font  TITLE_F   = new Font("Monospaced", Font.BOLD, 12);

    Z80 z80 = new Z80();          
    String loadedFilePath = null; 
    Timer autoRunTimer;           // timer que faz rodar passo a passo

    JLabel lblStatus;             // a barrinha de status lá embaixo

    // Registradores principais (os rótulos que mostram os valores)
    JLabel valA, valB, valC, valD, valE, valH, valL, valF;
    JLabel valPC, valSP, valIX, valIY;

    // Flags individuais (cada booleano vira um "led" na tela)
    JLabel flagS, flagZ, flagH, flagPV, flagN, flagC;

    // Painel de código fonte (a lista com as linhas do .asm)
    JList<String> sourceList;
    DefaultListModel<String> sourceModel;

    // Memória (hex dump) – mostra os bytes bonitinhos
    JTextArea memArea;

    // Botões que a gente clica
    JButton btnLoad, btnStep, btnRun, btnReset;


    public GUI() {
        super("Z80 Emulator — Simulador");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(BG);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(6, 6));

        // Monta cada pedacinho da janela e joga no lugar certo
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildSourcePanel(), BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildMemoryPanel(), BorderLayout.EAST);
        add(buildStatusBar(),   BorderLayout.SOUTH);

        updateUI();                       // atualiza tudo na tela
        setSize(1100, 680);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);      // centraliza a janela
        setVisible(true);                 // mostra pro usuário
    }

    private JPanel buildTopBar() {
        // A parte de cima com o título e os botões
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        p.setBorder(new MatteBorder(0, 0, 2, 0, BORDER));

        JLabel title = new JLabel("  ⚡ Z80 Emulator");
        title.setFont(new Font("Monospaced", Font.BOLD, 17));
        title.setForeground(GREEN);
        title.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.add(title, BorderLayout.WEST);

        // Painelzinho pros botões
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btns.setBackground(PANEL_BG);

        btnLoad  = makeButton("📂 Carregar .asm", CYAN);
        btnStep  = makeButton("⏩ Step",           GREEN);
        btnRun   = makeButton("▶ Executar",        AMBER);
        btnReset = makeButton("↺ Resetar",         RED_FLAG);

        // O que acontece quando clica em cada botão
        btnLoad.addActionListener(e -> onLoad());
        btnStep.addActionListener(e -> onStep());
        btnRun.addActionListener(e -> onRun());
        btnReset.addActionListener(e -> onReset());

        btns.add(btnLoad);
        btns.add(btnStep);
        btns.add(btnRun);
        btns.add(btnReset);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    // Método auxiliar pra criar botão bonitinho (com hover suave)
    private JButton makeButton(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(TITLE_F);
        b.setForeground(fg);
        b.setBackground(new Color(30, 38, 48));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            new EmptyBorder(5, 12, 5, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(40, 55, 70)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(new Color(30, 38, 48)); }
        });
        return b;
    }

    private JPanel buildSourcePanel() {
        // Painel da esquerda: mostra o código .asm linha a linha
        JPanel p = darkPanel("📄 Código Fonte (.asm)", 340);

        sourceModel = new DefaultListModel<>();
        sourceList  = new JList<>(sourceModel);
        sourceList.setFont(MONO);
        sourceList.setBackground(BG);
        sourceList.setForeground(GREEN);
        sourceList.setSelectionBackground(new Color(0, 60, 30));
        sourceList.setSelectionForeground(Color.WHITE);
        sourceList.setFixedCellHeight(20);
        sourceList.setCellRenderer(new SourceCellRenderer()); // renderizador personalizado

        JScrollPane scroll = new JScrollPane(sourceList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // Renderer colorido para a lista de código – coloca um "▶" na linha que está executando
    class SourceCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            lbl.setFont(MONO);
            lbl.setBorder(new EmptyBorder(1, 6, 1, 6));

            String text = (String) value;
            boolean isCurrent = !z80.halted && index == z80.getCurrentSourceLine();

            if (isCurrent) {
                lbl.setBackground(new Color(0, 80, 40));
                lbl.setForeground(Color.WHITE);
                lbl.setText("▶ " + text);
            } else if (isSelected) {
                lbl.setBackground(new Color(30, 50, 40));
                lbl.setForeground(GREEN);
                lbl.setText("  " + text);
            } else {
                lbl.setBackground(BG);
                if (text.trim().startsWith("#") || text.trim().isEmpty()) {
                    lbl.setForeground(GREEN_DIM);
                } else {
                    lbl.setForeground(GREEN);
                }
                lbl.setText("  " + text);
            }
            return lbl;
        }
    }

    private JPanel buildCenterPanel() {
        // Junta os registradores e as flags no meio da tela
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(0, 0, 0, 0));

        outer.add(buildRegistersPanel(), BorderLayout.CENTER);
        outer.add(buildFlagsPanel(),     BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildRegistersPanel() {
        // Painel com todos os registradores (A, B, C, D, E, H, L, F, PC, SP, IX, IY)
        JPanel p = darkPanel("🗂 Registradores", -1);

        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setBackground(PANEL_BG);
        grid.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Registradores de 8 bits
        valA  = addRegRow(grid, "A  (Acumulador)");
        valB  = addRegRow(grid, "B");
        valC  = addRegRow(grid, "C");
        valD  = addRegRow(grid, "D");
        valE  = addRegRow(grid, "E");
        valH  = addRegRow(grid, "H");
        valL  = addRegRow(grid, "L");
        valF  = addRegRow(grid, "F  (Flags)");

        // um separador visual
        grid.add(makeSep()); grid.add(makeSep());

        // Registradores especiais de 16 bits
        valPC = addRegRow(grid, "PC (Program Counter)");
        valSP = addRegRow(grid, "SP (Stack Pointer)");
        valIX = addRegRow(grid, "IX");
        valIY = addRegRow(grid, "IY");

        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    // Adiciona uma linha (nome + valor) no grid dos registradores
    private JLabel addRegRow(JPanel parent, String name) {
        JLabel lName = new JLabel(name);
        lName.setFont(TITLE_F);
        lName.setForeground(new Color(140, 160, 180));

        JLabel lVal = new JLabel("0x00");
        lVal.setFont(MONO_BIG);
        lVal.setForeground(AMBER);
        lVal.setHorizontalAlignment(SwingConstants.RIGHT);

        parent.add(lName);
        parent.add(lVal);
        return lVal;
    }

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setBackground(BORDER);
        return sep;
    }

    private JPanel buildFlagsPanel() {
        // Mostra as flags de Status: S, Z, H, P/V, N, C
        JPanel p = darkPanel("🚩 Flags (Registrador F)", -1);

        JPanel flags = new JPanel(new GridLayout(2, 6, 4, 4));
        flags.setBackground(PANEL_BG);
        flags.setBorder(new EmptyBorder(6, 8, 8, 8));

        String[] names = {"S", "Z", "H", "P/V", "N", "C"};
        String[] tips  = {"Sign", "Zero", "Half Carry", "Parity/Overflow", "Subtract", "Carry"};
        JLabel[] indicators = new JLabel[6];

        for (int i = 0; i < names.length; i++) {
            JLabel lbl = new JLabel(names[i], SwingConstants.CENTER);
            lbl.setFont(new Font("Monospaced", Font.BOLD, 11));
            lbl.setForeground(new Color(140, 160, 180));
            lbl.setToolTipText(tips[i]);
            flags.add(lbl);
        }

        flagS  = makeFlagLight(); flags.add(flagS);
        flagZ  = makeFlagLight(); flags.add(flagZ);
        flagH  = makeFlagLight(); flags.add(flagH);
        flagPV = makeFlagLight(); flags.add(flagPV);
        flagN  = makeFlagLight(); flags.add(flagN);
        flagC  = makeFlagLight(); flags.add(flagC);

        p.add(flags, BorderLayout.CENTER);
        return p;
    }

    // Cria um "led" de flag (aquele retângulo que fica verde ou apagado)
    private JLabel makeFlagLight() {
        JLabel l = new JLabel("0", SwingConstants.CENTER);
        l.setFont(MONO_BIG);
        l.setOpaque(true);
        l.setBackground(new Color(30, 38, 48));
        l.setBorder(BorderFactory.createLineBorder(BORDER));
        return l;
    }

    private void setFlagLight(JLabel lbl, boolean on) {
        lbl.setText(on ? "1" : "0");
        lbl.setForeground(on ? GREEN : GREEN_DIM);
        lbl.setBackground(on ? new Color(0, 60, 30) : new Color(30, 38, 48));
    }

    private JPanel buildMemoryPanel() {
        // Painel da direita: mostra os primeiros 256 bytes da memória em hexa
        JPanel p = darkPanel("💾 Memória (primeiros 256 bytes)", 320);

        memArea = new JTextArea();
        memArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        memArea.setBackground(BG);
        memArea.setForeground(new Color(100, 160, 120));
        memArea.setEditable(false);
        memArea.setBorder(new EmptyBorder(4, 6, 4, 6));

        JScrollPane scroll = new JScrollPane(memArea);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildStatusBar() {
        // A barrinha cinza lá embaixo, que mostra mensagens pro usuário
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        p.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));

        lblStatus = new JLabel("  Aguardando arquivo... use '📂 Carregar .asm'");
        lblStatus.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(140, 160, 180));
        lblStatus.setBorder(new EmptyBorder(5, 6, 5, 6));
        p.add(lblStatus, BorderLayout.WEST);
        return p;
    }


    private JPanel darkPanel(String title, int preferredWidth) {
        // Cria um painel com fundo escuro e uma "cabeça" com título
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, BORDER),
            new EmptyBorder(0, 0, 0, 0)));

        JLabel header = new JLabel("  " + title);
        header.setFont(TITLE_F);
        header.setForeground(CYAN);
        header.setBackground(new Color(20, 25, 33));
        header.setOpaque(true);
        header.setBorder(new EmptyBorder(5, 6, 5, 6));
        p.add(header, BorderLayout.NORTH);

        if (preferredWidth > 0) {
            p.setPreferredSize(new Dimension(preferredWidth, 0));
        }
        return p;
    }

    private void onLoad() {
        // Carrega um arquivo .asm do computador
        if (autoRunTimer != null) autoRunTimer.stop();

        JFileChooser fc = new JFileChooser(".");
        fc.setFileFilter(new FileNameExtensionFilter("Assembly files (*.asm)", "asm"));
        fc.setDialogTitle("Selecione o arquivo .asm");

        if (loadedFilePath != null) {
            fc.setCurrentDirectory(new File(loadedFilePath).getParentFile());
        }

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                z80.loadFile(f.getAbsolutePath());
                loadedFilePath = f.getAbsolutePath();

                sourceModel.clear();
                for (String line : z80.sourceLines) {
                    sourceModel.addElement(line);
                }

                setStatus("✅ Arquivo carregado: " + f.getName() + " — " + z80.sourceLines.size() + " linha(s)");
                updateUI();
                sourceList.ensureIndexIsVisible(0);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Erro ao carregar arquivo:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
                setStatus("❌ Erro ao carregar: " + ex.getMessage());
            }
        }
    }

    private void onStep() {
        // Executa uma única instrução (step)
        if (z80.halted) {
            setStatus("🛑 Processador parado (HALT). Clique em ↺ Resetar para reiniciar.");
            return;
        }
        if (loadedFilePath == null) {
            setStatus("⚠ Nenhum arquivo carregado. Use 📂 Carregar .asm primeiro.");
            return;
        }

        int prevLine = z80.getCurrentSourceLine();
        boolean canContinue = z80.step();

        updateUI();
        scrollSourceToCurrentLine();

        if (!canContinue || z80.halted) {
            setStatus("🛑 HALT — Execução concluída. PC=" + String.format("0x%04X", z80.PC));
        } else {
            int curLine = z80.getCurrentSourceLine();
            String lineName = (curLine >= 0 && curLine < z80.sourceLines.size())
                ? z80.sourceLines.get(curLine).trim() : "?";
            setStatus("⏩ Step → linha " + (curLine + 1) + ": " + lineName + "  |  PC=" + String.format("0x%04X", z80.PC));
        }
    }

    private void onRun() {
        // Executa o programa inteiro, passo a passo com um timer
        if (z80.halted) {
            setStatus("🛑 Já parado. Use ↺ Resetar.");
            return;
        }
        if (loadedFilePath == null) {
            setStatus("⚠ Nenhum arquivo carregado.");
            return;
        }

        btnRun.setEnabled(false);
        btnStep.setEnabled(false);

        autoRunTimer = new Timer(120, null);
        autoRunTimer.addActionListener(e -> {
            if (z80.halted) {
                autoRunTimer.stop();
                btnRun.setEnabled(true);
                btnStep.setEnabled(true);
                setStatus("🛑 HALT — Execução concluída. PC=" + String.format("0x%04X", z80.PC));
                updateUI();
                return;
            }
            boolean ok = z80.step();
            updateUI();
            scrollSourceToCurrentLine();
            if (!ok || z80.halted) {
                autoRunTimer.stop();
                btnRun.setEnabled(true);
                btnStep.setEnabled(true);
                setStatus("🛑 HALT — Execução concluída. PC=" + String.format("0x%04X", z80.PC));
            } else {
                setStatus("▶ Executando... PC=" + String.format("0x%04X", z80.PC));
            }
        });
        autoRunTimer.start();
    }

    private void onReset() {
        // Reseta o processador e recarrega o arquivo se tiver
        if (autoRunTimer != null) autoRunTimer.stop();
        btnRun.setEnabled(true);
        btnStep.setEnabled(true);

        if (loadedFilePath != null) {
            try {
                z80.loadFile(loadedFilePath);
                setStatus("↺ Resetado — " + new File(loadedFilePath).getName());
            } catch (Exception ex) {
                setStatus("❌ Erro ao recarregar: " + ex.getMessage());
            }
        } else {
            z80.reset();
            setStatus("↺ Resetado.");
        }
        updateUI();
        sourceList.ensureIndexIsVisible(0);
    }

    private void updateUI() {
        // Pega todos os valores do Z80 e mostra nos componentes
        valA.setText(fmt8(z80.A));
        valB.setText(fmt8(z80.B));
        valC.setText(fmt8(z80.C));
        valD.setText(fmt8(z80.D));
        valE.setText(fmt8(z80.E));
        valH.setText(fmt8(z80.H));
        valL.setText(fmt8(z80.L));
        valF.setText(fmt8(z80.F));

        valPC.setText(String.format("0x%04X  (%d)", z80.PC, z80.PC));
        valSP.setText(String.format("0x%04X  (%d)", z80.SP & 0xFFFF, z80.SP & 0xFFFF));
        valIX.setText(String.format("0x%04X  (%d)", z80.IX, z80.IX));
        valIY.setText(String.format("0x%04X  (%d)", z80.IY, z80.IY));

        int f = z80.F & 0xFF;
        setFlagLight(flagS,  (f & 0x80) != 0);
        setFlagLight(flagZ,  (f & 0x40) != 0);
        setFlagLight(flagH,  (f & 0x10) != 0);
        setFlagLight(flagPV, (f & 0x04) != 0);
        setFlagLight(flagN,  (f & 0x02) != 0);
        setFlagLight(flagC,  (f & 0x01) != 0);

        updateMemoryView();
        sourceList.repaint(); // pra atualizar a setinha ▶
    }

    private void updateMemoryView() {
        // Monta a visualização hex dump da memória
        StringBuilder sb = new StringBuilder();
        sb.append("ADDR  00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n");
        sb.append("─────────────────────────────────────────────────────\n");
        for (int row = 0; row < 16; row++) {
            sb.append(String.format("%04X  ", row * 16));
            for (int col = 0; col < 16; col++) {
                int addr = row * 16 + col;
                sb.append(String.format("%02X ", z80.MEM[addr] & 0xFF));
            }
            sb.append("\n");
        }
        memArea.setText(sb.toString());
        memArea.setCaretPosition(0);
    }

    private void scrollSourceToCurrentLine() {
        // Rola a lista de código até a linha que está sendo executada
        int line = z80.getCurrentSourceLine();
        if (line >= 0 && line < sourceModel.size()) {
            sourceList.setSelectedIndex(line);
            sourceList.ensureIndexIsVisible(line);
        }
    }

    private void setStatus(String msg) {
        lblStatus.setText("  " + msg);
    }

    private String fmt8(byte b) {
        int v = b & 0xFF;
        return String.format("0x%02X  (%3d)", v, v);
    }

    public static void main(String[] args) {
        // Tenta deixar o visual mais bonitinho
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI();

            // Se passou um arquivo por argumento, carrega na mão
            if (args.length > 0) {
                try {
                    gui.z80.loadFile(args[0]);
                    gui.loadedFilePath = args[0];
                    gui.sourceModel.clear();
                    for (String line : gui.z80.sourceLines) gui.sourceModel.addElement(line);
                    gui.setStatus("✅ Arquivo carregado: " + new File(args[0]).getName());
                    gui.updateUI();
                } catch (Exception e) {
                    gui.setStatus("❌ Erro: " + e.getMessage());
                }
            }
        });
    }
}