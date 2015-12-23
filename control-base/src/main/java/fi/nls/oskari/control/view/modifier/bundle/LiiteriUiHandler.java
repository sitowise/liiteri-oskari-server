package fi.nls.oskari.control.view.modifier.bundle;

import fi.nls.oskari.annotation.OskariViewModifier;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.view.modifier.ModifierException;
import fi.nls.oskari.view.modifier.ModifierParams;
import org.json.JSONObject;

@OskariViewModifier("liiteri-ui")
public class LiiteriUiHandler extends BundleHandler
{
    private static final Logger log =
        LogFactory.getLogger(LiiteriUiHandler.class);

    public boolean modifyBundle(final ModifierParams params)
            throws ModifierException
    {
        final JSONObject bundleConfig = getBundleConfig(params.getConfig());

        if (bundleConfig == null) {
            return false;
        }

        String trackingId = PropertyUtil.get("google-analytics.tracking-id");
        JSONObject analyticsConfig = new JSONObject();
        JSONHelper.putValue(analyticsConfig, "trackingId", trackingId);
        JSONHelper.putValue(bundleConfig, "analytics", analyticsConfig);
        
        return false;
    }
}