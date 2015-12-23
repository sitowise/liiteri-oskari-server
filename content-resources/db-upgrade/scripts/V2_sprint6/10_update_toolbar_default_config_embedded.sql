update portti_view_bundle_seq set config = '
{
      "basictools": {
          "measurearea": false,
          "measureline": false,
          "select": false,
          "zoombox": false,
		  "clear": false,
		  "featureinfo": false
      },
      "history": {
          "history_back": false,
          "history_forward": false,
          "reset": false
      },
      "viewtools": {
          "link": false,
          "print": false
      }
  }'
where view_id = 3 and bundle_id = 7;