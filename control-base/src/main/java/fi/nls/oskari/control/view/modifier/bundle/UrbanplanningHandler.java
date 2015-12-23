package fi.nls.oskari.control.view.modifier.bundle;

import fi.nls.oskari.annotation.OskariViewModifier;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.view.modifier.ModifierException;
import fi.nls.oskari.view.modifier.ModifierParams;
import org.json.JSONObject;

/**
 * Created by urho.tamminen on 4.3.2015.
 *
 * based on MyPlaces2Handler.java
 */

@OskariViewModifier("liiteri-urbanplanning")
public class UrbanplanningHandler extends BundleHandler
{
    private static final Logger log =
        LogFactory.getLogger(UrbanplanningHandler.class);

    public boolean modifyBundle(final ModifierParams params)
            throws ModifierException
    {
        final JSONObject loginConfig = getBundleConfig(params.getConfig());

        if (loginConfig == null) {
            return false;
        }

        String location = PropertyUtil.get(
            "liiteri.urbanplanning.location", "/Oskari/applications/sample/urbanplanning/index.html");
        JSONHelper.putValue(loginConfig, "location", location);

        return false;
    }
}