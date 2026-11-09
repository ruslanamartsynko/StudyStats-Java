package ua.studystats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Основні методи описової статистики без сторонніх бібліотек. */
public final class Statistics {
    private Statistics() {}

    public record Summary(int n, double min, double max, double mean, double median,
                          double q1, double q3, double sampleVariance,
                          double sampleStandardDeviation, List<Double> modes) {}

    public record Regression(int n, double slope, double intercept, double rSquared) {
        public double predict(double x) {
            return intercept + slope * x;
        }
    }

    public record Bounds(double lower, double upper) {
        public boolean isOutlier(double value) {
            return value < lower || value > upper;
        }
    }

    public static Summary summarize(double[] values) {
        requireNonEmpty(values);
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        double median = medianRange(sorted, 0, n);
        double q1;
        double q3;
        if (n == 1) {
            q1 = median;
            q3 = median;
        } else {
            // Метод Тьюкі: при непарному n центральний елемент виключається.
            q1 = medianRange(sorted, 0, n / 2);
            q3 = medianRange(sorted, (n + 1) / 2, n);
        }
        double mean = mean(values);
        double variance = sampleVariance(values);
        return new Summary(n, sorted[0], sorted[n - 1], mean, median, q1, q3,
                variance, Math.sqrt(variance), modes(sorted));
    }

    public static double mean(double[] values) {
        requireNonEmpty(values);
        double sum = 0.0;
        for (double value : values) sum += value;
        return sum / values.length;
    }

    /** Вибіркова дисперсія із поправкою Бесселя (ділимо на n - 1). */
    public static double sampleVariance(double[] values) {
        requireNonEmpty(values);
        if (values.length < 2) return Double.NaN;
        double mean = mean(values);
        double squaredDeviations = 0.0;
        for (double value : values) {
            double deviation = value - mean;
            squaredDeviations += deviation * deviation;
        }
        return squaredDeviations / (values.length - 1);
    }

    /** Кореляція Пірсона для пар спостережень x[i], y[i]. */
    public static double pearson(double[] x, double[] y) {
        requirePairs(x, y);
        double mx = mean(x);
        double my = mean(y);
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - mx;
            double dy = y[i] - my;
            sxx += dx * dx;
            syy += dy * dy;
            sxy += dx * dy;
        }
        if (sxx == 0.0 || syy == 0.0) {
            throw new IllegalArgumentException("Кореляція не визначена: одна зі змінних стала.");
        }
        return Math.max(-1.0, Math.min(1.0, sxy / Math.sqrt(sxx * syy)));
    }

    /** Проста лінійна регресія y = a + b*x, метод найменших квадратів. */
    public static Regression regression(double[] x, double[] y) {
        requirePairs(x, y);
        double mx = mean(x);
        double my = mean(y);
        double sxx = 0.0;
        double sxy = 0.0;
        for (int i = 0; i < x.length; i++) {
            sxx += (x[i] - mx) * (x[i] - mx);
            sxy += (x[i] - mx) * (y[i] - my);
        }
        if (sxx == 0.0) {
            throw new IllegalArgumentException("Регресія не визначена: X має нульову дисперсію.");
        }
        double slope = sxy / sxx;
        double intercept = my - slope * mx;
        double sse = 0.0;
        double sst = 0.0;
        for (int i = 0; i < x.length; i++) {
            double error = y[i] - (intercept + slope * x[i]);
            sse += error * error;
            sst += (y[i] - my) * (y[i] - my);
        }
        double rSquared = sst == 0.0 ? 1.0 : 1.0 - sse / sst;
        return new Regression(x.length, slope, intercept,
                Math.max(0.0, Math.min(1.0, rSquared)));
    }

    /** Викиди за правилом 1.5*IQR. */
    public static Bounds outlierBounds(double[] values) {
        Summary s = summarize(values);
        double iqr = s.q3() - s.q1();
        return new Bounds(s.q1() - 1.5 * iqr, s.q3() + 1.5 * iqr);
    }

    private static double medianRange(double[] sorted, int fromInclusive, int toExclusive) {
        int n = toExclusive - fromInclusive;
        if (n <= 0) throw new IllegalArgumentException("Порожній діапазон для медіани.");
        int middle = fromInclusive + n / 2;
        if (n % 2 == 1) return sorted[middle];
        return sorted[middle - 1] / 2.0 + sorted[middle] / 2.0;
    }

    private static List<Double> modes(double[] sorted) {
        Map<Double, Integer> counts = new TreeMap<>();
        for (double value : sorted) counts.merge(value, 1, Integer::sum);
        int maxCount = 0;
        for (int frequency : counts.values()) maxCount = Math.max(maxCount, frequency);
        if (maxCount <= 1) return List.of();
        List<Double> result = new ArrayList<>();
        for (var entry : counts.entrySet()) {
            if (entry.getValue() == maxCount) result.add(entry.getKey());
        }
        return List.copyOf(result);
    }

    private static void requireNonEmpty(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Потрібне хоча б одне числове значення.");
        }
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Некоректне число: NaN або нескінченність.");
            }
        }
    }

    private static void requirePairs(double[] x, double[] y) {
        requireNonEmpty(x);
        requireNonEmpty(y);
        if (x.length != y.length || x.length < 2) {
            throw new IllegalArgumentException("Для аналізу потрібні щонайменше дві повні пари X і Y.");
        }
    }
}
