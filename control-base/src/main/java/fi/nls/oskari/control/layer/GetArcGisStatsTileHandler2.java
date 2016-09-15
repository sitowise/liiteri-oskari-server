package fi.nls.oskari.control.layer;

import fi.nls.oskari.annotation.OskariActionRoute;

// ArcGIS93Rest layers send action_route parameter twice when using post
// to URL with params and route could be
// "GetArcGisStatsTile,GetArcGisStatsTile"
// This is a dummy class to register two action routes for one handler
@OskariActionRoute("GetArcGisStatsTile,GetArcGisStatsTile")
public class GetArcGisStatsTileHandler2 extends GetArcGisStatsTileHandler {

}