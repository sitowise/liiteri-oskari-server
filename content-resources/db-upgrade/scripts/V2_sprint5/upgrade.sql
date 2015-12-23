ALTER TABLE oskari_maplayer
   ADD COLUMN download_service_url character varying(255);

-- Table: oskari_user_gis_data_sharing

-- DROP TABLE oskari_user_gis_data_sharing;

CREATE TABLE oskari_user_gis_data_sharing
(
  id serial NOT NULL,
  dataset_id integer NOT NULL,
  external_id integer NOT NULL,
  external_type character varying(255),
  email character varying(255),
  email_sent boolean,
  CONSTRAINT "PK_OSKARI_USER_GIS_DATA_SHARING" PRIMARY KEY (id),
  CONSTRAINT "FK_OSKARI_USER_GIS_DATA_SHARING" FOREIGN KEY (dataset_id)
      REFERENCES oskari_user_gis_data (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE oskari_user_gis_data_sharing
  OWNER TO oskari;

-- Index: "FKI_OSKARI_USER_GIS_DATA_SHARING"

-- DROP INDEX "FKI_OSKARI_USER_GIS_DATA_SHARING";

CREATE INDEX "FKI_OSKARI_USER_GIS_DATA_SHARING"
  ON oskari_user_gis_data_sharing
  USING btree
  (dataset_id);

insert into oskari_permission (oskari_resource_id, external_type, permission, external_id)  (select distinct oskari_resource_id, external_type, 'VIEW_PUBLISHED', external_id from oskari_permission where permission = 'VIEW_LAYER');

insert into oskari_permission (oskari_resource_id, external_type, permission, external_id)  (select distinct oskari_resource_id, external_type, 'PUBLISH', external_id from oskari_permission where permission = 'VIEW_LAYER');

delete from portti_view_bundle_seq where view_id = 3;
-------------------------------------------
-- 1. Openlayers
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
       VALUES ((SELECT id FROM portti_view WHERE type='PUBLISH'), 
        (SELECT id FROM portti_bundle WHERE name = 'openlayers-default-theme'), 
        1, '{}','{}', '{}', 'openlayers-default-theme');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
    "title" : "OpenLayers",
    "fi" : "OpenLayers",
    "sv" : "OpenLayers",
    "en" : "OpenLayers",
    "bundlename" : "openlayers-default-theme",
    "bundleinstancename" : "openlayers-default-theme",
    "metadata" : {
        "Import-Bundle" : {
            "openlayers-single-full" : {
                "bundlePath" : "/Oskari/packages/openlayers/bundle/"
            },
            "openlayers-default-theme" : {
                "bundlePath" : "/Oskari/packages/openlayers/bundle/"
            }
        },
        "Require-Bundle-Instance" : []
     },
     "instanceProps" : {}
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'openlayers-default-theme') 
    AND view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');

--------------------------------------------
-- 2. Mapfull
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
       VALUES ((SELECT id FROM portti_view WHERE type='PUBLISH'), 
        (SELECT id FROM portti_bundle WHERE name = 'mapfull'), 
        (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = (SELECT id FROM portti_view WHERE type='PUBLISH')), 
        '{}','{}', '{}', 'mapfull');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
    "title" : "Map",
    "fi" : "mapfull",
    "sv" : "mapfull",
    "en" : "mapfull",
    "bundlename" : "mapfull",
    "bundleinstancename" : "mapfull",
    "metadata" : {
        "Import-Bundle" : {
            "core-base" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "core-map" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "sandbox-base" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "sandbox-map" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "event-base" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "event-map" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "event-map-layer" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "request-base" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "request-map" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "request-map-layer" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "service-base" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "service-map" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "domain" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapmodule-plugin" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapwfs2" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapanalysis" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapstats" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapwmts" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapuserlayers" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "maparcgis" : {
                "bundlePath" : "/Oskari/packages/arcgis/bundle/"
            },
            "oskariui" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapfull" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "ui-components": {
                "bundlePath": "/Oskari/packages/framework/bundle/"
            }
        },
        "Require-Bundle-Instance" : []
    },
    "instanceProps" : {
}}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'mapfull') 
    AND view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');


-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
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
            "mapUrlPrefix": {
                "en": "//www.paikkatietoikkuna.fi/web/en/map-window?",
                "fi": "//www.paikkatietoikkuna.fi/web/fi/kartta?",
                "sv": "//www.paikkatietoikkuna.fi/web/sv/kartfonstret?"
            },
            "termsUrl": {
                "en": "//www.paikkatietoikkuna.fi/web/en/terms-and-conditions",
                "fi": "//www.paikkatietoikkuna.fi/web/fi/kayttoehdot",
                "sv": "//www.paikkatietoikkuna.fi/web/sv/anvandningsvillkor"
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
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'mapfull') 
    AND view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');


-- update proper state for view
UPDATE portti_view_bundle_seq set state = '{
    "east": "517620",
    "north": "6874042",
    "selectedLayers": [],
    "zoom": 1
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'mapfull') 
    AND view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');

--------------------------------------------
-- 3. Toolbar
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup) 
    VALUES ((SELECT id FROM portti_view WHERE type='PUBLISH'), 
    (SELECT id FROM portti_bundle WHERE name = 'toolbar'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = (SELECT id FROM portti_view WHERE type='PUBLISH')), 
    '{}','{}', '{}');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Toolbar",
        "fi" : "toolbar",
        "sv" : "toolbar",
        "en" : "toolbar",
        "bundlename" : "toolbar",
        "bundleinstancename" : "toolbar",
        "metadata" : {
            "Import-Bundle" : {
                "toolbar" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                 }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'toolbar') 
    AND  view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');

UPDATE portti_view_bundle_seq set config =
  '{
      "basictools": {
          "measurearea": false,
          "measureline": false,
          "select": false,
          "zoombox": false
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
WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'toolbar')
      AND  view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');

--------------------------------------------
-- 4. Infobox
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ((SELECT id FROM portti_view WHERE type='PUBLISH'), 
    (SELECT id FROM portti_bundle WHERE name = 'infobox'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = (SELECT id FROM portti_view WHERE type='PUBLISH')), 
    '{}','{}', '{}', 'infobox');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title": "Infobox",
        "bundleinstancename": "infobox",
        "fi": "infobox",
        "sv": "infobox",
        "en": "infobox",
        "bundlename": "infobox",
        "metadata": {
            "Import-Bundle": {
                "infobox": {
                    "bundlePath": "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance": [ ]
        },
        "instanceProps": {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'infobox') 
    AND  view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');


-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "adaptable": true
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'infobox') 
    AND view_id=(SELECT id FROM portti_view WHERE type='PUBLISH');
	
UPDATE portti_bundle set config = '{
		"publishedMapUrl": "http://liiteri-test.sito.fi/?viewId="
	}' WHERE name = 'publisher';
	