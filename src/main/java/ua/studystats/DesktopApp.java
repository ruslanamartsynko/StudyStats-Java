package ua.studystats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Graphical desktop edition of StudyStats, built only with Java Swing. */
public final class DesktopApp {
    private static final Color NAVY = new Color(26, 40, 78);
    private static final Color TEXT = new Color(45, 56, 82);
    private static final Color MUTED = new Color(108, 122, 146);
    private static final Color ACCENT = new Color(51, 94, 208);
    private static final Color BG = new Color(244, 247, 252);
    private static final Color BORDER = new Color(219, 226, 238);

    private final JFrame window = new JFrame("StudyStats | Statistical Data Analyzer");
    private final JLabel fileLabel = new JLabel("Немає завантаженого файлу");
    private final JLabel countLabel = new JLabel("");
    private final DefaultTableModel dataModel = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable dataTable = new JTable(dataModel);
    private final JComboBox<String> summaryColumn = new JComboBox<>();
    private final JComboBox<String> histColumn = new JComboBox<>();
    private final JComboBox<Integer> bins = new JComboBox<>(new Integer[]{5, 8, 10, 12, 16, 20});
    private final JComboBox<String> xColumn = new JComboBox<>();
    private final JComboBox<String> yColumn = new JComboBox<>();
    private final JCheckBox regressionToggle = new JCheckBox("Показати лінію регресії", true);
    private final JTextArea summaryText = infoText();
    private final JTextArea histText = infoText();
    private final JTextArea relationText = infoText();
    private final JTextField predictX = new JTextField(10);
    private final JLabel prediction = new JLabel(" ");
    private final ChartPanel histogram = new ChartPanel();
    private final ChartPanel scatter = new ChartPanel();
    private final JTabbedPane tabs = new JTabbedPane();
    private CsvDataset dataset;
    private boolean loading;

    private DesktopApp() {
        initUi();
        try {
            loadFile(Path.of("data", "student_performance.csv"));
        } catch (IOException | IllegalArgumentException failure) {
            showError("Не вдалося завантажити демодані: " + failure.getMessage());
        }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* Windows/Linux default is sufficient. */ }
            DesktopApp app = new DesktopApp();
            if (args.length > 0) {
                try { app.loadFile(Path.of(args[0])); }
                catch (IOException | IllegalArgumentException ex) { app.showError(ex.getMessage()); }
            }
            app.window.setVisible(true);
        });
    }

    private void initUi() {
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(1040, 690));
        window.setSize(1240, 800);
        window.setLocationRelativeTo(null);
        window.getContentPane().setBackground(BG);
        window.setLayout(new BorderLayout());
        window.add(header(), BorderLayout.NORTH);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBackground(BG);
        tabs.addTab("  Дані та статистика  ", overviewTab());
        tabs.addTab("  Розподіл і викиди  ", histogramTab());
        tabs.addTab("  Кореляція та регресія  ", relationTab());
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(16, 24, 14, 24));
        wrap.add(tabs);
        window.add(wrap, BorderLayout.CENTER);
        JLabel footer = new JLabel("  Навчальний проєкт • Java 17+ • CSV • Без сторонніх бібліотек  |  Демодані синтетичні");
        footer.setForeground(MUTED);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setBorder(new EmptyBorder(0, 24, 13, 24));
        window.add(footer, BorderLayout.SOUTH);
    }

    private JPanel header() {
        JPanel pane = new JPanel(new BorderLayout(18, 0));
        pane.setBackground(NAVY);
        pane.setBorder(new EmptyBorder(20, 26, 20, 26));
        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel("StudyStats  /  Statistical Data Analyzer");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        fileLabel.setForeground(new Color(196, 207, 231));
        fileLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleBlock.add(title);
        titleBlock.add(fileLabel);
        pane.add(titleBlock, BorderLayout.CENTER);
        JButton open = button("  Відкрити CSV…  ");
        open.setBackground(new Color(255, 255, 255));
        open.setForeground(NAVY);
        open.addActionListener(e -> chooseFile());
        pane.add(open, BorderLayout.EAST);
        return pane;
    }

    private JPanel overviewTab() {
        JPanel outer = basePanel();
        JPanel top = new JPanel(new BorderLayout(10, 8));
        top.setOpaque(false);
        JLabel intro = section("Перегляд даних");
        countLabel.setForeground(MUTED);
        top.add(intro, BorderLayout.WEST);
        top.add(countLabel, BorderLayout.EAST);
        outer.add(top, BorderLayout.NORTH);

        dataTable.setRowHeight(27);
        dataTable.setGridColor(new Color(236, 239, 247));
        dataTable.setShowVerticalLines(false);
        dataTable.setAutoCreateRowSorter(true);
        dataTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dataTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        dataTable.getTableHeader().setBackground(new Color(229, 236, 251));
        dataTable.getTableHeader().setForeground(NAVY);
        JScrollPane scroll = new JScrollPane(dataTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel right = card(new BorderLayout(10, 12));
        right.setPreferredSize(new Dimension(330, 300));
        JPanel choose = new JPanel(new GridLayout(3, 1, 0, 7));
        choose.setOpaque(false);
        choose.add(section("Описова статистика"));
        choose.add(hint("Числовий стовпець"));
        choose.add(summaryColumn);
        right.add(choose, BorderLayout.NORTH);
        right.add(summaryText, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, right);
        split.setResizeWeight(.69);
        split.setDividerSize(12);
        split.setBorder(null);
        outer.add(split, BorderLayout.CENTER);
        summaryColumn.addActionListener(e -> updateSummary());
        return outer;
    }

    private JPanel histogramTab() {
        JPanel outer = basePanel();
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        toolbar.setOpaque(false);
        toolbar.add(section("Розподіл показника"));
        toolbar.add(hint("Стовпець:"));
        toolbar.add(histColumn);
        toolbar.add(hint("Інтервалів:"));
        toolbar.add(bins);
        outer.add(toolbar, BorderLayout.NORTH);
        JPanel chartCard = card(new BorderLayout(6, 0));
        chartCard.add(histogram, BorderLayout.CENTER);
        JPanel infoCard = card(new BorderLayout(12, 10));
        infoCard.setPreferredSize(new Dimension(300, 360));
        infoCard.add(section("Викиди та квартилі"), BorderLayout.NORTH);
        infoCard.add(histText, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartCard, infoCard);
        split.setResizeWeight(.73);
        split.setDividerSize(12);
        split.setBorder(null);
        outer.add(split, BorderLayout.CENTER);
        histColumn.addActionListener(e -> updateHistogram());
        bins.addActionListener(e -> updateHistogram());
        bins.setSelectedItem(10);
        return outer;
    }

    private JPanel relationTab() {
        JPanel outer = basePanel();
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 13, 0));
        toolbar.setOpaque(false);
        toolbar.add(section("Залежність між показниками"));
        toolbar.add(hint("X:"));
        toolbar.add(xColumn);
        toolbar.add(hint("Y:"));
        toolbar.add(yColumn);
        regressionToggle.setBackground(BG);
        regressionToggle.setForeground(TEXT);
        toolbar.add(regressionToggle);
        outer.add(toolbar, BorderLayout.NORTH);

        JPanel chartCard = card(new BorderLayout());
        chartCard.add(scatter, BorderLayout.CENTER);
        JPanel infoCard = card(new BorderLayout(12, 10));
        infoCard.setPreferredSize(new Dimension(305, 400));
        infoCard.add(section("Модель та прогноз"), BorderLayout.NORTH);
        JPanel rightBody = new JPanel(new BorderLayout(8, 14));
        rightBody.setOpaque(false);
        rightBody.add(relationText, BorderLayout.CENTER);
        JPanel predict = new JPanel(new GridLayout(4, 1, 0, 7));
        predict.setOpaque(false);
        predict.add(hint("Введи X для прогнозу Y:"));
        predict.add(predictX);
        JButton predictButton = button("Розрахувати прогноз");
        predictButton.addActionListener(e -> calculatePrediction());
        predict.add(predictButton);
        prediction.setForeground(ACCENT);
        prediction.setFont(new Font("SansSerif", Font.BOLD, 12));
        predict.add(prediction);
        rightBody.add(predict, BorderLayout.SOUTH);
        infoCard.add(rightBody, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartCard, infoCard);
        split.setResizeWeight(.73);
        split.setDividerSize(12);
        split.setBorder(null);
        outer.add(split, BorderLayout.CENTER);
        xColumn.addActionListener(e -> updateRelation());
        yColumn.addActionListener(e -> updateRelation());
        regressionToggle.addActionListener(e -> updateRelation());
        return outer;
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Оберіть CSV-файл");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV (*.csv)", "csv"));
        if (dataset != null) chooser.setCurrentDirectory(dataset.path().toAbsolutePath().getParent().toFile());
        if (chooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
            try { loadFile(chooser.getSelectedFile().toPath()); }
            catch (IOException | IllegalArgumentException ex) { showError(ex.getMessage()); }
        }
    }

    private void loadFile(Path path) throws IOException {
        CsvDataset candidate = CsvDataset.load(path);
        // Apply only after validating data so a bad import doesn't destroy the previous view.
        dataset = candidate;
        loading = true;
        try {
            fileLabel.setText("Файл: " + path.getFileName().toString() + "   •   " + dataset.size() + " спостережень");
            countLabel.setText(dataset.size() + " рядків  ·  " + dataset.headers().size() + " стовпців");
            List<String> headers = dataset.headers();
            dataModel.setRowCount(0);
            dataModel.setColumnIdentifiers(headers.toArray());
            for (List<String> cells : dataset.rows()) dataModel.addRow(cells.toArray());
            String[] numeric = dataset.numericColumns().toArray(String[]::new);
            for (JComboBox<String> combo : List.of(summaryColumn, histColumn, xColumn, yColumn)) {
                combo.setModel(new DefaultComboBoxModel<>(numeric));
            }
            if (numeric.length > 0) {
                int xIndex = findColumn(numeric, "study_hours", 0);
                int yIndex = findColumn(numeric, "math_score", Math.min(1, numeric.length - 1));
                xColumn.setSelectedIndex(xIndex);
                yColumn.setSelectedIndex(yIndex);
                histColumn.setSelectedIndex(xIndex);
                summaryColumn.setSelectedIndex(yIndex);
            }
        } finally {
            loading = false;
        }
        updateSummary();
        updateHistogram();
        updateRelation();
    }

    private static int findColumn(String[] names, String target, int fallback) {
        for (int i = 0; i < names.length; i++) if (target.equals(names[i])) return i;
        return fallback;
    }

    private void updateSummary() {
        if (loading || dataset == null) return;
        String name = (String) summaryColumn.getSelectedItem();
        if (name == null) { summaryText.setText("Немає числових стовпців."); return; }
        try {
            Statistics.Summary s = Statistics.summarize(dataset.numbers(name));
            summaryText.setText("Кількість:       " + s.n() + "\n\n"
                    + "Середнє:        " + fmt(s.mean()) + "\n"
                    + "Медіана:        " + fmt(s.median()) + "\n"
                    + "Мода:           " + (s.modes().isEmpty() ? "немає" : s.modes()) + "\n\n"
                    + "Мінімум:        " + fmt(s.min()) + "\n"
                    + "Максимум:       " + fmt(s.max()) + "\n"
                    + "Q1 / Q3:        " + fmt(s.q1()) + " / " + fmt(s.q3()) + "\n\n"
                    + "Дисперсія:      " + fmt(s.sampleVariance()) + "\n"
                    + "Ст. відхилення: " + fmt(s.sampleStandardDeviation()));
        } catch (IllegalArgumentException ex) { summaryText.setText(ex.getMessage()); }
    }

    private void updateHistogram() {
        if (loading || dataset == null) return;
        String name = (String) histColumn.getSelectedItem();
        if (name == null) { histogram.clear(); histText.setText("Немає числових стовпців."); return; }
        try {
            double[] values = dataset.numbers(name);
            Statistics.Summary s = Statistics.summarize(values);
            Statistics.Bounds bounds = Statistics.outlierBounds(values);
            int count = 0;
            StringBuilder examples = new StringBuilder();
            for (CsvDataset.Observation o : dataset.observations(name)) {
                if (bounds.isOutlier(o.value())) {
                    count++;
                    if (count <= 8) examples.append("\n • ").append(dataset.rowLabel(o.rowNumber()))
                            .append(": ").append(fmt(o.value()));
                }
            }
            histText.setText("Спостережень: " + s.n() + "\n\n"
                    + "Медіана: " + fmt(s.median()) + "\n"
                    + "Q1: " + fmt(s.q1()) + "\n"
                    + "Q3: " + fmt(s.q3()) + "\n"
                    + "IQR: " + fmt(s.q3() - s.q1()) + "\n\n"
                    + "Межі 1.5 × IQR:\n[" + fmt(bounds.lower()) + "; " + fmt(bounds.upper()) + "]\n\n"
                    + "Викидів: " + count + examples
                    + (count > 8 ? "\n • …" : "")
                    + "\n\nКвартилі: метод Тьюкі.");
            histogram.showHistogram(values, (Integer) bins.getSelectedItem(), name);
        } catch (IllegalArgumentException ex) { histText.setText(ex.getMessage()); histogram.clear(); }
    }

    private void updateRelation() {
        if (loading || dataset == null) return;
        String xn = (String) xColumn.getSelectedItem();
        String yn = (String) yColumn.getSelectedItem();
        prediction.setText(" ");
        if (xn == null || yn == null) {
            relationText.setText("Потрібні два числові стовпці."); scatter.clear(); return;
        }
        try {
            CsvDataset.Pairs pairs = dataset.pairs(xn, yn);
            double r = Statistics.pearson(pairs.x(), pairs.y());
            Statistics.Regression model = Statistics.regression(pairs.x(), pairs.y());
            relationText.setText("Повних пар: " + pairs.size() + "\n\n"
                    + "Кореляція Пірсона:\nr = " + fmt(r) + "\n\n"
                    + "Лінійна регресія:\nY = " + fmt(model.intercept()) + " + (" + fmt(model.slope()) + ") · X\n\n"
                    + "R² = " + fmt(model.rSquared()) + "\n\n"
                    + "Кореляція не означає\nпричинності. Прогноз\nпоза діапазоном даних\nможе бути ненадійним.");
            scatter.showScatter(pairs.x(), pairs.y(), xn, yn,
                    regressionToggle.isSelected(), model);
        } catch (IllegalArgumentException ex) {
            relationText.setText("Неможливо обчислити модель:\n" + ex.getMessage());
            scatter.clear();
        }
    }

    private void calculatePrediction() {
        if (dataset == null || xColumn.getSelectedItem() == null || yColumn.getSelectedItem() == null) return;
        try {
            String raw = predictX.getText().strip().replace(',', '.');
            if (raw.isEmpty()) throw new IllegalArgumentException("Введи числове значення X.");
            double value = Double.parseDouble(raw);
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Введи скінченне число.");
            CsvDataset.Pairs pairs = dataset.pairs((String) xColumn.getSelectedItem(), (String) yColumn.getSelectedItem());
            Statistics.Regression reg = Statistics.regression(pairs.x(), pairs.y());
            prediction.setText("Прогноз Y ≈ " + fmt(reg.predict(value)));
        } catch (NumberFormatException ex) {
            prediction.setText("Помилка: некоректне число.");
        } catch (IllegalArgumentException ex) {
            prediction.setText("Помилка: " + ex.getMessage());
        }
    }

    private static String fmt(double n) {
        return Double.isNaN(n) ? "не визначено" : String.format(Locale.US, "%.3f", n);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(window, message, "Помилка StudyStats", JOptionPane.ERROR_MESSAGE);
    }

    private static JTextArea infoText() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setForeground(TEXT);
        area.setBackground(Color.WHITE);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(10, 5, 2, 0));
        return area;
    }

    private static JLabel section(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(NAVY);
        return label;
    }

    private static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(MUTED);
        return label;
    }

    private static JPanel basePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(22, 18, 20, 18));
        return panel;
    }

    private static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    private static JButton button(String label) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(new EmptyBorder(12, 15, 12, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
