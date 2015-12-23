package fi.nls.oskari.control.szopa;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import pl.sito.liiteri.stats.domain.ClassificationDescription;
import pl.sito.liiteri.stats.domain.ClassificationParams;
import pl.sito.liiteri.stats.domain.ClassificationParams.ClassificationMode;
import pl.sito.liiteri.stats.domain.ClassificationParams.ClassificationType;
import pl.sito.liiteri.stats.domain.ClassificationParams.DataTransformationType;
import pl.sito.liiteri.stats.domain.GridStatsResult;

import com.esri.core.geometry.Envelope;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.twowaystats.TwowayIndicatorService;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("GetStatsClassification")
public class GetStatsClassificationHandler extends ActionHandler {

    private final IndicatorService indicatorService = IndicatorService
            .getInstance();
    private final TwowayIndicatorService twowayIndicatorService = TwowayIndicatorService
            .getInstance();
    private final IndicatorClassificationService indicatorClassificationService = IndicatorClassificationService
            .getInstance();

    private static final String PARAM_GRID_SIZE = "SIZE";
    final private static String PARAM_INDICATOR_DATA = "INDICATORDATA";
    final private static String PARAM_BBOX = "BBOX";
    final private static String PARAM_METHOD = "METHOD";
    final private static String PARAM_NUMBER_OF_CLASSES = "NUMBEROFCLASSES";
    final private static String PARAM_CLASSIFICATION_MODE = "MODE";
    final private static String PARAM_DATA_TRANSFORMATION = "DATATRANSFORMATION";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String VALUE_CONTENT_TYPE_JSON = "application/json";
    private static final String OSKARI_ENCODING = "UTF-8";

    @Override
    public void handleAction(final ActionParameters params)
            throws ActionException {

        try {

            String actionResult = "{ \"items\": [] }";
            final int gridSize = parseGridSize(params);
            String indicatorData = params.getHttpParam(PARAM_INDICATOR_DATA,
                    null);
            JSONObject indicatorDataJSON = new JSONObject(indicatorData);
            String indicatorId = indicatorDataJSON.getString("id");
            String indicatorYear = indicatorDataJSON.getString("year");
            String indicatorGeometryFilter = indicatorDataJSON
                    .isNull("geometry") ? "" : indicatorDataJSON
                    .getString("geometry");
            String indicatorFilter = indicatorDataJSON.isNull("filter") ? ""
                    : indicatorDataJSON.getString("filter");
            String indicatorType = indicatorDataJSON.isNull("type") ? ""
                    : indicatorDataJSON.getString("type");
            String indicatorDirection = indicatorDataJSON.isNull("direction") ? ""
                    : indicatorDataJSON.getString("direction");
            String indicatorGender = indicatorDataJSON.isNull("gender") ? ""
                    : indicatorDataJSON.getString("gender");

            Envelope bbox = parseBoundingBox(params);
            if (bbox != null)
                bbox.inflate(gridSize, gridSize);

            GridStatsResult result = null;
            int id = Integer.parseInt(indicatorId);
            if (id >= 0) {
                result = indicatorService.getGridIndicatorData(indicatorId,
                        new String[] { indicatorYear }, gridSize,
                        indicatorFilter, indicatorGeometryFilter, bbox,
                        params.getUser());
            } else {
                result = twowayIndicatorService.getGridIndicatorData(
                        indicatorId, new String[] { indicatorYear }, gridSize,
                        indicatorFilter, indicatorGeometryFilter, bbox,
                        indicatorType, indicatorDirection, indicatorGender,
                        params.getUser());
            }

            if (!result.IsEmpty()) {
                ClassificationParams classificationParams = parseClassficiationParams(params);
                ClassificationDescription description = indicatorClassificationService
                        .getClassificationDescription(result,
                                classificationParams);
                actionResult = description.toJSONString();
            }

            final HttpServletResponse response = params.getResponse();
            response.addHeader(HEADER_CONTENT_TYPE, VALUE_CONTENT_TYPE_JSON);
            response.setCharacterEncoding(OSKARI_ENCODING);
            ResponseHelper.writeResponse(params, actionResult);

        } catch (Exception e) {
            throw new ActionException("Couldn't generate stats", e);
        }
    }

    private ClassificationParams parseClassficiationParams(
            final ActionParameters params) {
        ClassificationParams result = new ClassificationParams();

        int numberOfClasses = params.getHttpParam(PARAM_NUMBER_OF_CLASSES, -1);
        result.setNumberOfClasses(numberOfClasses);
        int methodCode = params.getHttpParam(PARAM_METHOD, -1);
        ClassificationType classType;
        switch (methodCode) {
        case 1:
            classType = ClassificationType.Jenks;
            break;
        case 2:
            classType = ClassificationType.Quantile;
            break;
        case 3:
            classType = ClassificationType.Equal;
            break;
        case 4:
            classType = ClassificationType.Manual;
            break;
        default:
            classType = ClassificationType.Jenks;
            break;
        }
        result.setClassificationType(classType);
        int dataTransformationCode = params.getHttpParam(
                PARAM_DATA_TRANSFORMATION, -1);
        DataTransformationType dataTransformation;
        switch (dataTransformationCode) {
        case 1:
            dataTransformation = DataTransformationType.AbsoluteValues;
            break;
        default:
            dataTransformation = DataTransformationType.None;
            break;
        }
        result.setDataTransformation(dataTransformation);

        String classificationModeCode = params.getHttpParam(
                PARAM_CLASSIFICATION_MODE, "");
        ClassificationMode classificationMode = ClassificationMode.Default;
        if (classificationModeCode.equals("discontinuous"))
            classificationMode = ClassificationMode.Discontinuous;
        else if (classificationModeCode.equals("distinct"))
            classificationMode = ClassificationMode.Distinct;
        result.setClassificationMode(classificationMode);

        return result;
    }

    private int parseGridSize(final ActionParameters params) {
        int result = 250;
        int factor = 1;
        String visualizationMethod = params.getHttpParam(PARAM_GRID_SIZE, null);
        if (visualizationMethod != null
                && visualizationMethod.startsWith("grid")) {
            if (visualizationMethod.contains("km")) {
                factor = 1000;
            }
        }
        result = Integer.parseInt(visualizationMethod.replace("grid", "")
                .replace("km", "").replace("m", ""));
        result *= factor;
        return result;
    }

    private Envelope parseBoundingBox(final ActionParameters params) {
        Envelope bbox = null;

        String bboxString = params.getHttpParam(PARAM_BBOX, null);
        if (bboxString != null) {
            String[] bboxArray = bboxString.split(",");
            if (bboxArray.length == 4) {
                bbox = new Envelope();
                bbox.setXMin(Double.valueOf(bboxArray[0]));
                bbox.setYMin(Double.valueOf(bboxArray[1]));
                bbox.setXMax(Double.valueOf(bboxArray[2]));
                bbox.setYMax(Double.valueOf(bboxArray[3]));
            }
        }
        return bbox;
    }
}
