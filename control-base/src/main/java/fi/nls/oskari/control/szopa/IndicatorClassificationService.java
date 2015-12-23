package fi.nls.oskari.control.szopa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.filter.function.Classifier;

import pl.sito.liiteri.stats.domain.ClassificationDescription;
import pl.sito.liiteri.stats.domain.ClassificationParams;
import pl.sito.liiteri.stats.domain.ClassificationParams.ClassificationMode;
import pl.sito.liiteri.stats.domain.ClassificationParams.ClassificationType;
import pl.sito.liiteri.stats.domain.ClassificationParams.DataTransformationType;
import pl.sito.liiteri.stats.domain.GridStatsResult;
import pl.sito.liiteri.stats.domain.GridStatsResultItem;
import pl.sito.liiteri.stats.domain.GridStatsVisualization;

public class IndicatorClassificationService {
    private static class IndicatorClassificationServiceHolder {
        static final IndicatorClassificationService INSTANCE = new IndicatorClassificationService();
    }

    public static IndicatorClassificationService getInstance() {
        return IndicatorClassificationServiceHolder.INSTANCE;
    }

    protected IndicatorClassificationService() {

    }

    public GridStatsVisualization classify(GridStatsResult input,
            ClassificationParams params) {
        GridStatsVisualization result = new GridStatsVisualization();
        ArrayList<Double> data = new ArrayList<Double>();
        for (GridStatsResultItem item : input.getItems()) {
            data.add(item.getValue());
        }

        Classifier classifier = getClassifier(params.getClassificationType(),
                data, params.getNumberOfClasses());
        result.setDescriptions(params.getColors());

        for (GridStatsResultItem item : input.getItems()) {
            int index = classifier.classify(item.getValue());
            result.classify(index, item);
        }

        return result;
    }

    public GridStatsVisualization classify(GridStatsResult input,
            ClassificationDescription description) {
        GridStatsVisualization result = new GridStatsVisualization();
        int numberOfClasses = description.getNumberOfDescriptions();
        String[] descriptions = description.getDescriptions();
        result.setDescriptions(descriptions);

        List<Comparable> localMin = new ArrayList<Comparable>();
        List<Comparable> localMax = new ArrayList<Comparable>();

        for (int i = 0; i < numberOfClasses; i++) {
            String desc = descriptions[i];
            localMin.add(description.getMin(desc));
            localMax.add(description.getMax(desc));
        }

        Classifier classifier = new RangedClassifier(
                localMin.toArray(new Comparable[0]),
                localMax.toArray(new Comparable[0]));

        for (GridStatsResultItem item : input.getItems()) {
            double value = item.getValue();
            if (description.getDataTransformation() == DataTransformationType.AbsoluteValues
                    && value < 0)
                value = -value;

            int index = classifier.classify(value);
            if (index != -1)
                result.classify(index, item);
        }

        return result;
    }

    public ClassificationDescription getClassificationDescription(
            GridStatsResult input, ClassificationParams params) {
        ClassificationDescription result = new ClassificationDescription();
        ArrayList<Double> data = new ArrayList<Double>();
        int minPrecision = 0;
        for (GridStatsResultItem item : input.getItems()) {
            double value = item.getValue();
            if (params.getDataTransformation() == DataTransformationType.AbsoluteValues
                    && value < 0) {
                value = -value;
            }
            data.add(value);

            if (params.getClassificationMode() == ClassificationMode.Distinct) {
                String[] splitted = Double.toString(value).split("\\.");
                if (splitted.length > 1 && value != (int) value) {
                    int precision = splitted[1].length();
                    if (precision > minPrecision)
                        minPrecision = precision;
                }
            }
        }

        RangedClassifier classifier = getClassifier(
                params.getClassificationType(), data,
                params.getNumberOfClasses());
        String[] colors = params.getColors();

        double minPrecisionNormalized = 1 / Math.pow(10, minPrecision);
        int dataIdx = 0;

        if (params.getClassificationMode() == ClassificationMode.Discontinuous) {
            Collections.sort(data);
        }

        for (int i = 0; i < classifier.getSize(); i++) {
            Double min = (Double) classifier.getMin(i);
            Double max = (Double) classifier.getMax(i);
            String description = colors != null ? colors[i] : i + "";

            if (params.getClassificationMode() == ClassificationMode.Distinct) {
                if (i != 0) {
                    BigDecimal dec = new BigDecimal(min
                            + minPrecisionNormalized);
                    dec = dec.setScale(minPrecision, RoundingMode.HALF_UP);
                    min = dec.doubleValue();
                }
            } else if (params.getClassificationMode() == ClassificationMode.Discontinuous) {
                min = data.get(dataIdx);
                while ((dataIdx < data.size()) && data.get(dataIdx) <= max) {
                    dataIdx++;
                }
                max = data.get(dataIdx - 1);
            }

            result.add(description, min, max);
        }

        result.setDataTransformation(params.getDataTransformation());

        return result;
    }

    private RangedClassifier getClassifier(ClassificationType classType,
            List<Double> data, int numberOfClasses) {
        RangedClassifier result = null;
        switch (classType) {
        case Jenks:
            result = getJenks(data, numberOfClasses);
            break;
        case Equal:
            result = getEqual(data, numberOfClasses);
            break;
        case Quantile:
            result = getQuantiles(data, numberOfClasses);
            break;
        default:
            result = getJenks(data, numberOfClasses);
            break;
        }
        return result;
    }

    private RangedClassifier getQuantiles(List<Double> data, int numberOfClasses) {

        Collections.sort(data);
        final int dataSize = data.size();

        float step = dataSize / numberOfClasses;

        Comparable[] localMin = new Comparable[numberOfClasses];
        Comparable[] localMax = new Comparable[numberOfClasses];

        localMin[0] = data.get(0);
        for (int i = 1; i < numberOfClasses; i++) {
            int index = Math.round(i * step);
            localMin[i] = data.get(index);
            localMax[i - 1] = localMin[i];
        }
        localMax[numberOfClasses - 1] = data.get(dataSize - 1);

        return new RangedClassifier(localMin, localMax);
    }

    private RangedClassifier getEqual(List<Double> data, int numberOfClasses) {
        double max = Collections.max(data);
        double min = Collections.min(data);

        double interval = (max - min) / numberOfClasses;

        Comparable[] localMin = new Comparable[numberOfClasses];
        Comparable[] localMax = new Comparable[numberOfClasses];

        double val = min;
        for (int i = 0; i < numberOfClasses; i++) {
            localMin[i] = val;
            val += interval;
            localMax[i] = val;
        }

        return new RangedClassifier(localMin, localMax);
    }

    private RangedClassifier getJenks(List<Double> data, int numberOfClasses) {
        Collections.sort(data);
        final int dataSize = data.size();

        if (numberOfClasses == dataSize) {
            Comparable[] localMin = new Comparable[numberOfClasses];
            Comparable[] localMax = new Comparable[numberOfClasses];

            for (int id = 0; id < numberOfClasses - 1; id++) {

                localMax[id] = data.get(id + 1);
                localMin[id] = data.get(id);
            }
            localMax[numberOfClasses - 1] = data.get(numberOfClasses - 1);
            localMin[numberOfClasses - 1] = data.get(numberOfClasses - 1);
            return new RangedClassifier(localMin, localMax);
        }

        int[][] iwork = new int[dataSize + 1][numberOfClasses + 1];
        double[][] work = new double[dataSize + 1][numberOfClasses + 1];
        for (int j = 1; j <= numberOfClasses; j++) {
            // the first item is always in the first class!
            iwork[0][j] = 1;
            iwork[1][j] = 1;
            // initialize work matirix
            work[1][j] = 0;
            for (int i = 2; i <= dataSize; i++) {
                work[i][j] = Double.MAX_VALUE;
            }
        }

        // calculate the class for each data item
        for (int i = 1; i <= dataSize; i++) {
            // sum of data values
            double s1 = 0;
            // sum of squares of data values
            double s2 = 0;

            double var = 0.0;
            // consider all the previous values
            for (int ii = 1; ii <= i; ii++) {
                // index in to sorted data array
                int i3 = i - ii + 1;
                // remember to allow for 0 index
                double val = data.get(i3 - 1);
                // update running totals
                s2 = s2 + (val * val);
                s1 += val;
                double s0 = (double) ii;
                // calculate (square of) the variance
                // (http://secure.wikimedia.org/wikipedia/en/wiki/Standard_deviation#Rapid_calculation_methods)
                var = s2 - ((s1 * s1) / s0);
                // System.out.println(s0+" "+s1+" "+s2);
                // System.out.println(i+","+ii+" var "+var);
                int ik = i3 - 1;
                if (ik != 0) {
                    // not the last value
                    for (int j = 2; j <= numberOfClasses; j++) {
                        // for each class compare current value to var +
                        // previous value
                        // System.out.println("\tis "+work[i][j]+" >= "+(var +
                        // work[ik][j - 1]));
                        if (work[i][j] >= (var + work[ik][j - 1])) {
                            // if it is greater or equal update classification
                            iwork[i][j] = i3 - 1;
                            // System.out.println("\t\tiwork["+i+"]["+j+"] = "+i3);
                            work[i][j] = var + work[ik][j - 1];
                        }
                    }
                }
            }
            // store the latest variance!
            iwork[i][1] = 1;
            work[i][1] = var;
        }

        // go through matrix and extract class breaks
        int ik = dataSize - 1;

        Comparable[] localMin = new Comparable[numberOfClasses];
        Comparable[] localMax = new Comparable[numberOfClasses];
        localMax[numberOfClasses - 1] = data.get(ik);
        for (int j = numberOfClasses; j >= 2; j--) {
            int id = (int) iwork[ik][j] - 1; // subtract one as we want
                                             // inclusive breaks on the
                                             // left?

            localMax[j - 2] = data.get(id);
            localMin[j - 1] = data.get(id);
            ik = (int) iwork[ik][j] - 1;
        }
        localMin[0] = data.get(0);

        return new RangedClassifier(localMin, localMax);
    }
}
