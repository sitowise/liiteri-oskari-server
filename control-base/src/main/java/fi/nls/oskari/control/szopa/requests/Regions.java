package fi.nls.oskari.control.szopa.requests;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.szopa.FilterParser;
import fi.nls.oskari.control.szopa.RegionDefinition;
import fi.nls.oskari.control.szopa.RegionDefinition.RegionType;
import fi.nls.oskari.control.szopa.RegionService;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONCopyHelper;
import fi.nls.oskari.util.JSONHelper;

public class Regions extends SzopaRequest {

    private static Logger log = LogFactory.getLogger(Regions.class);

    private List<SzopaRequestPart> _partRequests = new ArrayList<SzopaRequestPart>();
    private RegionService _regionService = RegionService.getInstance();
    private FilterParser _filterParser = FilterParser.getInstance();

    public Regions() {
        List<RegionDefinition> definition = _regionService.GetRegionsOfType(
                RegionType.ADMINISTRATIVE, RegionType.FUNCTIONAL);
        for (RegionDefinition regionDefinition : definition) {
            String requestUrl = "/areaTypes/" + regionDefinition.getApiid()
                    + "/areas";
            if (getStandardFilterParam().length() > 0) {
                try {
                    requestUrl += "?filter="
                            + URLEncoder
                                    .encode(_filterParser
                                            .getFilterFromAreas(_filterParser
                                                    .getAreasFromFilter(
                                                            getStandardFilterParam(),
                                                            RegionType.ADMINISTRATIVE)),
                                            "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn(e, "Could not encode filter param");
                }
            }
            _partRequests.add(new SzopaRequestPart(regionDefinition.getId(),
                    requestUrl));
        }
    }

    @Override
    public String getName() {
        return "regions";
    }

    @Override
    public String getCacheKey() {
        return "liiteri_szopa_regions_list";
    }

    @Override
    public boolean getUseCache() {
        return true;
    }

    @Override
    public String getData() throws ActionException {
        try {
            final String cachedData = TryGetCacheDataIfDesired();
            if (cachedData != null) {
                return cachedData;
            }

            final JSONArray result = new JSONArray();

            for (SzopaRequestPart partRequest : _partRequests) {
                partRequest.setVersion(getVersion());
                StopWatch sw = new StopWatch();
                sw.start();
                String data = partRequest.getData();
                sw.stop();
                log.info(partRequest.getName() + " " + sw.getTime());
                final JSONArray array = ConvertDataToJSON(data,
                        partRequest.getName());
                addRange(result, array);
            }

            String resultStr = result.toString();

            CacheDataIfDesired(resultStr);

            return resultStr;
        } catch (Exception e) {
            throw new ActionException("Couldn't proxy request to Szopa server",
                    e);
        }
    }

    private void addRange(JSONArray arr1, JSONArray arr2) throws JSONException {
        for (int i = 0; i < arr2.length(); i++) {
            arr1.put(arr2.get(i));
        }
    }

    private JSONArray ConvertDataToJSON(String data, String categoryName)
            throws ActionException {
        try {
            JSONArray array = JSONHelper.createJSONArray(data);
            JSONArray resultArray = new JSONArray();

            log.info("Got #%s regions", array.length());

            for (int i = 0; i < array.length(); i++) {
                JSONObject inputItem = array.getJSONObject(i);
                JSONObject resultItem = new JSONObject();

                JSONHelper.putValue(resultItem, "id", inputItem.get("AreaType")
                        + ":" + inputItem.get("Id"));

                JSONCopyHelper.Copy(inputItem, "AlternativeId", resultItem,
                        "code");
                JSONCopyHelper.Copy(inputItem, "OrderNumber", resultItem,
                        "orderNumber");
                JSONCopyHelper.LanguageAwareCopy(inputItem, "Name", resultItem,
                        "title");
                resultItem.put("category", categoryName);

                JSONArray childrenArray = new JSONArray();

                JSONArray parentArray = inputItem.optJSONArray("ParentAreas");
                if (parentArray != null) {
                    for (int j = 0; j < parentArray.length(); j++) {
                        JSONObject inputParentItem = parentArray
                                .getJSONObject(j);
                        int parentId = inputParentItem.optInt("Id", -1);
                        if (parentId != -1) {
                            childrenArray.put(inputParentItem.get("AreaType")
                                    + ":" + parentId);
                        }
                    }
                }
                resultItem.put("memberOf", childrenArray);

                if (_regionService.getType(categoryName) == RegionType.FUNCTIONAL) {
                    final SzopaRequest req = SzopaRequest
                            .getInstance("availability");
                    req.setAreaType(_regionService.getAPIId(categoryName));
                    req.setVersion("v1");

                    JSONArray yearsArray = new JSONArray(req.getData());
                    resultItem.put("availability", yearsArray);
                }

                resultArray.put(resultItem);
            }

            return resultArray;
        } catch (JSONException e) {
            log.error("Cannot convert data", e);
            throw new ActionException("Cannot convert data", e);
        }
    }

}
