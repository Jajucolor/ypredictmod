package io.github.jajucolor.github.ai;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class LinearRegression {
    private final List<mineral_AI.DataPoint> dataPoints;
    private double amountY; // Y축 가중치
    private double bias;

    public LinearRegression(List<mineral_AI.DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
        train();
    }

    // 선형 회귀 모델 학습 메서드
    private void train() {
        int n = dataPoints.size();
        if (n == 0) {
            throw new IllegalStateException("No data points to train the model.");
        }

        double sumY = 0, sumFreq = 0;
        double sumYFreq = 0, sumY2 = 0;

        // 데이터 합 계산
        for (mineral_AI.DataPoint dp : dataPoints) {
            int freq = dp.frequency;
            sumY += dp.y;
            sumFreq += freq;

            sumYFreq += dp.y * freq;
            sumY2 += dp.y * dp.y;
        }

        // 중앙값 기반으로 bias 계산
        double[] yValues = dataPoints.stream().mapToDouble(dp -> dp.y).toArray();
        Arrays.sort(yValues);
        bias = yValues[n / 2]; // 중앙값
    }

    // 위치 예측 메서드
    public String predictOptimalLocation() {
        double predictedY = 0;
        int n = dataPoints.size();

        for (mineral_AI.DataPoint dp : dataPoints) {
            predictedY += dp.y;
        }

        predictedY = amountY * (predictedY / n) + bias;

        return String.valueOf(predictedY);
    }

    public void printHistogram() {
        Map<Integer, Integer> histogram = new HashMap<>();
        for (mineral_AI.DataPoint dp : dataPoints) {
            histogram.put(dp.y, histogram.getOrDefault(dp.y, 0) + dp.frequency);
        }

        for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            System.out.println("Y=" + entry.getKey() + ": " + entry.getValue());
        }
    }
}