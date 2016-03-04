update portti_view_bundle_seq set config='{
  "queryUrl" : "[REPLACED BY HANDLER]",
  "featureNS" : "http://www.oskari.org",
  "layerDefaults" : {
    "wmsName" : "oskari:my_places_categories"
  },
  "wmsUrl" : "/oskari-map/action?action_route=MyPlacesTile&myCat="
}' where view_id=1 and bundleinstance='myplaces2';