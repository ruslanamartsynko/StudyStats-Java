package ua.studystats;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Консольний застосунок для дослідження табличних даних. */
public final class Main {
    private static final Scanner INPUT = new Scanner(System.in);
    private static CsvDataset dataset;

    private Main() {}

    public static void main(String[] args) {
        // Єдина кодування виведення для української мови в консолі.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  StudyStats — статистика на Java        ║");
        System.out.println("╚════════════════════════════════════════╝");
        if (args.length > 0 && "--help".equals(args[0])) {
            System.out.println("Запуск: java ... ua.studystats.Main [шлях/до/файлу.csv]");
            return;
        }
        Path initialPath = args.length > 0
                ? Path.of(args[0]) : Path.of("data", "student_performance.csv");
        try {
            load(initialPath);
        } catch (IOException | IllegalArgumentException error) {
            System.out.println("Не вдалося відкрити стартовий CSV: " + error.getMessage());
            System.out.println("Вибери пункт 6, щоб указати шлях до іншого файлу.");
        }
        while (true) {
            System.out.println("\n— Головне меню —");
            System.out.println("1. Переглянути дані та числові стовпці");
            System.out.println("2. Описова статистика для стовпця");
            System.out.println("3. Кореляція Пірсона між двома стовпцями");
            System.out.println("4. Лінійна регресія та прогноз");
            System.out.println("5. Знайти викиди за правилом 1.5 × IQR");
            System.out.println("6. Завантажити власний CSV");
            System.out.println("0. Вихід");
            String choice = ask("Твій вибір: ");
            if (choice == null || choice.equals("0")) {
                System.out.println("До зустрічі!");
                return;
            }
            try {
                if (!choice.equals("6") && dataset == null) {
                    System.out.println("Спочатку завантаж CSV через пункт 6.");
                    continue;
                }
                switch (choice) {
                    case "1" -> overview();
                    case "2" -> description();
                    case "3" -> correlation();
                    case "4" -> regression();
                    case "5" -> outliers();
                    case "6" -> changeFile();
                    default -> System.out.println("Немає такого пункту меню.");
                }
            } catch (IOException | IllegalArgumentException error) {
                System.out.println("Помилка: " + error.getMessage());
            }
        }
    }

    private static void load(Path path) throws IOException {
        CsvDataset candidate = CsvDataset.load(path);
        dataset = candidate;
        System.out.println("Завантажено: " + path + " (рядків: " + dataset.size() + ")");
    }

    private static void overview() {
        System.out.println("\nФайл: " + dataset.path());
        System.out.println("Спостережень: " + dataset.size());
        System.out.println("Усі стовпці: " + String.join(", ", dataset.headers()));
        System.out.println("Числові стовпці: " + String.join(", ", dataset.numericColumns()));
        System.out.println("\nПерші п'ять записів:");
        System.out.println(dataset.preview(5));
    }

    private static void description() {
        String name = chooseColumn("Який стовпець дослідити?");
        Statistics.Summary s = Statistics.summarize(dataset.numbers(name));
        System.out.println("\nОписова статистика: " + name);
        System.out.println("Кількість чисел: " + s.n());
        printf("Мінімум", s.min());
        printf("Максимум", s.max());
        printf("Середнє арифметичне", s.mean());
        printf("Медіана", s.median());
        System.out.println("Мода: " + (s.modes().isEmpty() ? "немає повторюваних значень" : s.modes()));
        printf("Q1 (нижній квартиль)", s.q1());
        printf("Q3 (верхній квартиль)", s.q3());
        printf("Вибіркова дисперсія (n−1)", s.sampleVariance());
        printf("Вибіркове стандартне відхилення", s.sampleStandardDeviation());
    }

    private static void correlation() {
        String xName = chooseColumn("Вибери X:");
        String yName = chooseColumn("Вибери Y:");
        CsvDataset.Pairs pairs = dataset.pairs(xName, yName);
        double r = Statistics.pearson(pairs.x(), pairs.y());
        System.out.printf(Locale.US, "\nКореляція Пірсона (%s, %s): r = %.4f%n", xName, yName, r);
        System.out.println("Повних пар спостережень: " + pairs.size());
        String strength = Math.abs(r) < 0.3 ? "слабкий" :
                Math.abs(r) < 0.7 ? "помірний" : "сильний";
        System.out.println("Зв'язок: " + strength + ", " + (r < 0 ? "від'ємний" : "додатний"));
        System.out.println("Увага: кореляція не означає причинно-наслідковий зв'язок.");
    }

    private static void regression() {
        String xName = chooseColumn("Незалежна змінна X:");
        String yName = chooseColumn("Залежна змінна Y:");
        CsvDataset.Pairs pairs = dataset.pairs(xName, yName);
        Statistics.Regression model = Statistics.regression(pairs.x(), pairs.y());
        System.out.printf(Locale.US, "\nРівняння: %s = %.4f + %.4f × %s%n",
                yName, model.intercept(), model.slope(), xName);
        System.out.printf(Locale.US, "R² = %.4f (пар: %d)%n", model.rSquared(), model.n());
        String raw = ask("Введи X для прогнозу (Enter — пропустити): ");
        if (raw != null && !raw.isBlank()) {
            double x = Double.parseDouble(raw.replace(',', '.'));
            if (!Double.isFinite(x)) throw new IllegalArgumentException("Введи скінченне число.");
            System.out.printf(Locale.US, "Прогноз %s при X = %.2f: %.2f%n",
                    yName, x, model.predict(x));
            System.out.println("Прогноз є демонстраційним; екстраполяція може бути ненадійною.");
        }
    }

    private static void outliers() {
        String name = chooseColumn("Вибери стовпець:");
        double[] values = dataset.numbers(name);
        Statistics.Bounds bounds = Statistics.outlierBounds(values);
        System.out.printf(Locale.US, "\nМежі для %s: [%.2f; %.2f]%n",
                name, bounds.lower(), bounds.upper());
        int count = 0;
        for (CsvDataset.Observation observation : dataset.observations(name)) {
            if (bounds.isOutlier(observation.value())) {
                System.out.printf(Locale.US, "  %s: %.2f%n",
                        dataset.rowLabel(observation.rowNumber()), observation.value());
                count++;
            }
        }
        if (count == 0) System.out.println("Викидів не знайдено.");
        else System.out.println("Усього викидів: " + count);
    }

    private static void changeFile() throws IOException {
        String raw = ask("Шлях до CSV (наприклад, data/student_performance.csv): ");
        if (raw != null && !raw.isBlank()) load(Path.of(raw.strip().replace("\"", "")));
    }

    private static String chooseColumn(String prompt) {
        List<String> columns = dataset.numericColumns();
        if (columns.isEmpty()) throw new IllegalArgumentException("У файлі немає числових стовпців.");
        System.out.println(prompt);
        for (int i = 0; i < columns.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, columns.get(i));
        }
        String input = ask("Номер стовпця: ");
        if (input == null) throw new IllegalArgumentException("Введення завершено.");
        int number;
        try {
            number = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Потрібно ввести номер стовпця.");
        }
        if (number < 1 || number > columns.size()) {
            throw new IllegalArgumentException("Номер стовпця поза межами списку.");
        }
        return columns.get(number - 1);
    }

    private static void printf(String label, double value) {
        System.out.printf(Locale.US, "%-37s %s%n", label + ":",
                Double.isNaN(value) ? "не визначено (n < 2)" : String.format(Locale.US, "%.4f", value));
    }

    private static String ask(String prompt) {
        System.out.print(prompt);
        return INPUT.hasNextLine() ? INPUT.nextLine().trim() : null;
    }
}
