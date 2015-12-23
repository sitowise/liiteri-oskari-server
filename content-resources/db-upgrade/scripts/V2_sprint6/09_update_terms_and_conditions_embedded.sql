update portti_view_bundle_seq set config = '
{
    "globalMapAjaxUrl": "[REPLACED BY HANDLER]",
    "imageLocation": "/Oskari/resources",
    "plugins" : [
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.LayersPlugin" },
       { "id" : "Oskari.mapframework.mapmodule.WmsLayerPlugin" },
       { "id" : "Oskari.mapframework.bundle.mapwfs2.plugin.WfsLayerPlugin" },
       { "id" : "Oskari.mapframework.wmts.mapmodule.plugin.WmtsLayerPlugin" },
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.RealtimePlugin" },
       {
        "id": "Oskari.mapframework.bundle.mapmodule.plugin.LogoPlugin",
        "config": {
            "termsUrl":{
               "fi":"http://www.ymparisto.fi/fi-FI/Elinympariston_tietopalvelu_Liiteri/Kayttoehdot",
               "sv":"http://www.ymparisto.fi/fi-FI/Elinympariston_tietopalvelu_Liiteri/Kayttoehdot",
               "en":"http://www.ymparisto.fi/fi-FI/Elinympariston_tietopalvelu_Liiteri/Kayttoehdot"
            }
        }
       },
       { 
	    "id" : "Oskari.mapframework.bundle.mapstats.plugin.StatsLayerPlugin",
	    "config": {
			"useArcGis": true
		}
	},
       { "id" : "Oskari.mapframework.mapmodule.MarkersPlugin" },
       { "id" : "Oskari.mapframework.bundle.mapanalysis.plugin.AnalysisLayerPlugin" },
       { "id" : "Oskari.mapframework.bundle.myplacesimport.plugin.UserLayersLayerPlugin" },
       { "id" : "Oskari.arcgis.bundle.maparcgis.plugin.ArcGisLayerPlugin" }
      ],
      "layers": [
      ],
      "mapOptions" : {
          "resolutions": [2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 0.5],
          "maxExtent": {
              "left": -548576.000000,
              "bottom": 6291456.000000,
              "right": 1548576.000000,
              "top": 8388608.000000
          },
          "srsName": "EPSG:3067"
      }
}'
where view_id = 3 and bundleinstance = 'mapfull'