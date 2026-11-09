package ua.studystats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Зчитує CSV з UTF-8 (роздільник — кома; підтримує значення у лапках). */
public final class CsvDataset {
    public record Observation(int rowNumber, double value) {}
    public record Pairs(double[] x, double[] y) {
        public int size() { return x.length; }
    }

    private final Path path;
    private final List<String> headers;
    private final List<List<String>> rows;

    private CsvDataset(Path path, List<String> headers, List<List<String>> rows) {
        this.path = path;
        this.headers = List.copyOf(headers);
        this.rows = List.copyOf(rows);
    }

    public static CsvDataset load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IllegalArgumentException("CSV порожній.");
        String first = lines.get(0).replaceFirst("^\\uFEFF", "");
        List<String> headers = splitCsv(first);
        Set<String> unique = new HashSet<>();
        for (String header : headers) {
            if (header.isBlank() || !unique.add(header)) {
                throw new IllegalArgumentException("Некоректна або повторна назва стовпця: " + header);
            }
        }
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            List<String> cells = splitCsv(line);
            if (cells.size() != headers.size()) {
                throw new IllegalArgumentException("Рядок " + (i + 1)
                        + ": очікується " + headers.size() + " стовпців, знайдено " + cells.size());
            }
            rows.add(List.copyOf(cells));
        }
        if (rows.isEmpty()) throw new IllegalArgumentException("CSV не містить рядків із даними.");
        return new CsvDataset(path, headers, rows);
    }

    public Path path() { return path; }
    public List<String> headers() { return headers; }
    public int size() { return rows.size(); }
    /** Immutable table data for desktop preview. */
    public List<List<String>> rows() { return rows; }

    public List<String> numericColumns() {
        List<String> result = new ArrayList<>();
        for (int column = 0; column < headers.size(); column++) {
            boolean hasValue = false;
            boolean isNumeric = true;
            for (List<String> row : rows) {
                String raw = row.get(column).trim();
                if (raw.isEmpty()) continue;
                hasValue = true;
                try {
                    double value = Double.parseDouble(raw);
                    if (!Double.isFinite(value)) isNumeric = false;
                } catch (NumberFormatException exception) {
                    isNumeric = false;
                }
                if (!isNumeric) break;
            }
            if (hasValue && isNumeric) result.add(headers.get(column));
        }
        return List.copyOf(result);
    }

    public List<Observation> observations(String name) {
        int column = numericColumnIndex(name);
        List<Observation> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String raw = rows.get(i).get(column);
            if (!raw.isBlank()) {
                result.add(new Observation(i + 1, Double.parseDouble(raw)));
            }
        }
        return result;
    }

    public double[] numbers(String name) {
        List<Observation> observations = observations(name);
        double[] result = new double[observations.size()];
        for (int i = 0; i < observations.size(); i++) result[i] = observations.get(i).value();
        return result;
    }

    /** Пропущений X або Y вилучає саме пару, а не тільки окрему колонку. */
    public Pairs pairs(String xName, String yName) {
        int xi = numericColumnIndex(xName);
        int yi = numericColumnIndex(yName);
        List<Double> xx = new ArrayList<>();
        List<Double> yy = new ArrayList<>();
        for (List<String> row : rows) {
            if (row.get(xi).isBlank() || row.get(yi).isBlank()) continue;
            xx.add(Double.parseDouble(row.get(xi)));
            yy.add(Double.parseDouble(row.get(yi)));
        }
        double[] x = new double[xx.size()];
        double[] y = new double[yy.size()];
        for (int i = 0; i < x.length; i++) {
            x[i] = xx.get(i);
            y[i] = yy.get(i);
        }
        return new Pairs(x, y);
    }

    public String rowLabel(int rowNumber) {
        int idColumn = headers.indexOf("student_id");
        return idColumn < 0 ? "рядок " + rowNumber : rows.get(rowNumber - 1).get(idColumn);
    }

    public String preview(int limit) {
        StringBuilder result = new StringBuilder(String.join(" | ", headers));
        for (int i = 0; i < Math.min(limit, rows.size()); i++) {
            result.append(System.lineSeparator()).append(String.join(" | ", rows.get(i)));
        }
        return result.toString();
    }

    private int numericColumnIndex(String name) {
        int index = headers.indexOf(name);
        if (index < 0) throw new IllegalArgumentException("Стовпець не знайдено: " + name);
        if (!numericColumns().contains(name)) {
            throw new IllegalArgumentException("Стовпець не є числовим: " + name);
        }
        return index;
    }

    private static List<String> splitCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                result.add(value.toString().trim());
                value.setLength(0);
            } else {
                value.append(c);
            }
        }
        if (quoted) throw new IllegalArgumentException("Незакриті лапки у CSV.");
        result.add(value.toString().trim());
        return result;
    }
}
