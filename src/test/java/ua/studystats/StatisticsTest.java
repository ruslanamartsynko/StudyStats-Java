package ua.studystats;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Тести без JUnit: достатньо стандартного JDK. */
public final class StatisticsTest {
    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        double[] values = {2, 4, 4, 4, 5, 5, 7, 9};
        Statistics.Summary s = Statistics.summarize(values);
        check(s.n() == 8, "Кількість");
        close(s.mean(), 5, "Середнє");
        close(s.median(), 4.5, "Медіана");
        close(s.sampleVariance(), 32.0 / 7, "Вибіркова дисперсія");
        close(s.q1(), 4.0, "Q1");
        close(s.q3(), 6.0, "Q3");
        check(s.modes().equals(java.util.List.of(4.0)), "Мода");

        double[] x = {1, 2, 3, 4};
        double[] y = {3, 5, 7, 9};
        close(Statistics.pearson(x, y), 1, "Кореляція");
        Statistics.Regression model = Statistics.regression(x, y);
        close(model.intercept(), 1, "Вільний член регресії");
        close(model.slope(), 2, "Нахил регресії");
        close(model.rSquared(), 1, "R²");
        close(model.predict(6), 13, "Прогноз");

        Statistics.Bounds bounds = Statistics.outlierBounds(new double[]{1, 2, 3, 4, 5, 6, 7, 30});
        check(bounds.isOutlier(30), "Викид IQR");
        check(!bounds.isOutlier(5), "Не викид IQR");

        Path temp = Files.createTempFile("studystats-test", ".csv");
        try {
            Files.writeString(temp, "student_id,x,y,note\nS1,1,2,ok\nS2,,4,ok\nS3,3,6,ok\n",
                    StandardCharsets.UTF_8);
            CsvDataset csv = CsvDataset.load(temp);
            check(csv.size() == 3, "CSV рядки");
            check(csv.numericColumns().equals(java.util.List.of("x", "y")), "Числові колонки");
            check(csv.numbers("x").length == 2, "Пропуски у колонці");
            check(csv.pairs("x", "y").size() == 2, "Попарна обробка пропусків");
            check(csv.rowLabel(1).equals("S1"), "Ідентифікатор");
        } finally {
            Files.deleteIfExists(temp);
        }

        System.out.println("Усі перевірки пройдені: " + passed);
    }

    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError("Тест не пройдено: " + name);
        passed++;
    }

    private static void close(double actual, double expected, String name) {
        check(Math.abs(actual - expected) < 1e-9, name + " (" + actual + " != " + expected + ")");
    }
}
