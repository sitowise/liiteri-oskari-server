
--------------------------------------------
--------------------------------------------
-- Creates a default map view for guest users
-- Notice these statements should be executed in the same order they are listed here
-- for startupsequence to work correctly
--------------------------------------------
--------------------------------------------

---- Checking bundle order ---
SELECT b.name, s.config, s.state, s.startup
    FROM portti_view_bundle_seq s, portti_bundle b 
    WHERE s.bundle_id = b.id AND s.view_id = 
        (SELECT max(id) FROM portti_view WHERE application = 'full-map_guest')
    ORDER BY s.view_id, s.seqno;

--------------------------------------------
-- View
--------------------------------------------

INSERT INTO portti_view (name, type, is_default, application, page, application_dev_prefix, is_public)
    VALUES ('Guest default view', 
            'USER', 
             false,
             'full-map_guest',
             'view',
             '/applications/paikkatietoikkuna.fi',
             true);



--------------------------------------------
-- QUERY FOR VIEW ID AND MODIFY THE FOLLOWING STATEMENTS TO USE IT INSTEAD OF [VIEW_ID]
--------------------------------------------

SELECT id FROM portti_view v WHERE application = 'full-map_guest';


--------------------------------------------
-- 1. Openlayers
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
       VALUES ([VIEW_ID], 
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
            "openlayers-full-map" : {
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
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 2. Mapfull
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
       VALUES ([VIEW_ID], 
        (SELECT id FROM portti_bundle WHERE name = 'mapfull'), 
        (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
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
            "mapwmts" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapstats" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapanalysis" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "oskariui" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "mapfull" : {
                "bundlePath" : "/Oskari/packages/framework/bundle/"
            },
            "maparcgis" : {
                "bundlePath" : "/Oskari/packages/arcgis/bundle/"
            },
            "ui-components": {
                "bundlePath": "/Oskari/packages/framework/bundle/"
            },
            "mapuserlayers" : {
              "bundlePath" : "/Oskari/packages/framework/bundle/"
            }
        },
        "Require-Bundle-Instance" : []
    },
    "instanceProps" : {}
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'mapfull') 
    AND view_id=[VIEW_ID];


-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
    "globalMapAjaxUrl": "[REPLACED BY HANDLER]",
    "imageLocation": "/Oskari/resources",
    "mapOptions" : {"srsName":"EPSG:3067","maxExtent":{"bottom":6291456,"left":-548576,"right":1548576,"top":8388608},"resolutions":[2048,1024,512,256,128,64,32,16,8,4,2,1,0.5,0.25]},
    "plugins" : [
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.LayersPlugin" },
       { "id" : "Oskari.mapframework.mapmodule.WmsLayerPlugin" },
       { "id" : "Oskari.mapframework.mapmodule.MarkersPlugin" },
       { "id" : "Oskari.mapframework.mapmodule.ControlsPlugin" },
       { "id" : "Oskari.mapframework.mapmodule.GetInfoPlugin",
         "config" : { 
            "ignoredLayerTypes" : ["WFS"], 
            "infoBox": false 
         }
       },
       { "id" : "Oskari.mapframework.bundle.mapwfs2.plugin.WfsLayerPlugin", 
         "config" : { 
           "contextPath" : "/transport-0.0.1", 
           "hostname" : "demo.paikkatietoikkuna.fi", 
           "port" : "80",
           "lazy" : true,
           "disconnectTime" : 30000,
           "backoffIncrement": 1000,
           "maxBackoff": 60000,
           "maxNetworkDelay": 10000
         }
       },
       { "id" : "Oskari.mapframework.wmts.mapmodule.plugin.WmtsLayerPlugin" } ,
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.ScaleBarPlugin" },
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.Portti2Zoombar" },
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.PanButtons" },
       { "id" : "Oskari.mapframework.bundle.mapstats.plugin.StatsLayerPlugin" },
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.GeoLocationPlugin" },
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.RealtimePlugin" },
       { "id" : "Oskari.mapframework.bundle.mapmodule.plugin.FullScreenPlugin" },
       { "id" : "Oskari.mapframework.mapmodule.VectorLayerPlugin" }
       { "id" : "Oskari.arcgis.bundle.maparcgis.plugin.ArcGisLayerPlugin" },
       {
            "id" : "Oskari.mapframework.bundle.mapmodule.plugin.BackgroundLayerSelectionPlugin",
            "config" : {
                "showAsDropdown" : false,
                "baseLayers" : ["base_2", "24", "base_35"]
            }
       },
       { 
        "id": "Oskari.mapframework.bundle.mapmodule.plugin.LogoPlugin",
        "config": {
            "mapUrlPrefix": {
                "en": "http://www.paikkatietoikkuna.fi/web/en/map-window?",
                "fi": "http://www.paikkatietoikkuna.fi/web/fi/kartta?",
                "sv": "http://www.paikkatietoikkuna.fi/web/sv/kartfonstret?"
            },
            "termsUrl": {
                "en": "http://www.paikkatietoikkuna.fi/web/en/terms-and-conditions",
                "fi": "http://www.paikkatietoikkuna.fi/web/fi/kayttoehdot",
                "sv": "http://www.paikkatietoikkuna.fi/web/sv/anvandningsvillkor"
            }
        } }
      ],
      "layers": [
      ]
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'mapfull') 
    AND view_id=[VIEW_ID];

-- update proper state for view
UPDATE portti_view_bundle_seq set state = '{
    "east": "520000",
    "north": "7250000",
    "selectedLayers": [{"id": "base_35"}],
    "zoom": 0
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'mapfull') 
    AND view_id=[VIEW_ID];


--------------------------------------------
-- 3. Divmanazer
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'divmanazer'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'divmanazer');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title": "Oskari DIV Manazer",
        "bundleinstancename": "divmanazer",
        "fi": "divmanazer",
        "sv": "divmanazer",
        "en": "divmanazer",
        "bundlename": "divmanazer",
        "metadata": {
            "Import-Bundle": {
                "divmanazer": {
                    "bundlePath": "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance": [ ]
        },
        "instanceProps": {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'divmanazer') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 4. Toolbar
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'toolbar'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'toolbar');

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
    AND  view_id=[VIEW_ID];


-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "viewtools": {
            "print" : false
        },
        "mapUrlPrefix": {
            "en": "http://www.paikkatietoikkuna.fi/web/en/map-window?",
            "fi": "http://www.paikkatietoikkuna.fi/web/fi/kartta?",
            "sv": "http://www.paikkatietoikkuna.fi/web/sv/kartfonstret?"
        }
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'toolbar') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 5.statehandler
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'statehandler'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'statehandler');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Statehandler",
        "fi" : "statehandler",
        "sv" : "statehandler",
        "en" : "statehandler",
        "bundlename" : "statehandler",
        "bundleinstancename" : "statehandler",
        "metadata" : {
            "Import-Bundle" : {
                "statehandler" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'statehandler') 
    AND  view_id=[VIEW_ID];


-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "logUrl" : "/log/maplink.png"
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'statehandler') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 6. Infobox
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'infobox'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
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
    AND  view_id=[VIEW_ID];


-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "adaptable": true
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'infobox') 
    AND view_id=[VIEW_ID];


--------------------------------------------
-- 7. Search
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'search'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'search');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Search",
        "fi" : "search",
        "sv" : "?",
        "en" : "?",
        "bundlename" : "search",
        "bundleinstancename" : "search",
        "metadata" : {
            "Import-Bundle" : {
                "search" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'search') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 8. LayerSelector
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'layerselector2'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'layerselector2');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Maplayer selection",
        "fi" : "layerselector2",
        "sv" : "layerselector2",
        "en" : "layerselector2",
        "bundlename" : "layerselector2",
        "bundleinstancename" : "layerselector2",
        "metadata" : {
            "Import-Bundle" : {
                "layerselector2" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'layerselector2') 
    AND view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config='{
   "showSearchSuggestions" : true
}'
WHERE bundle_id = (SELECT id from portti_bundle WHERE name = 'layerselector2') 
AND view_id=[VIEW_ID];

--------------------------------------------
-- 9. LayerSelection
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'layerselection2'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'layerselection2');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Chosen maplayers",
        "fi" : "layerselection2",
        "sv" : "layerselection2",
        "en" : "layerselection2",
        "bundlename" : "layerselection2",
        "bundleinstancename" : "layerselection2",
        "metadata" : {
            "Import-Bundle" : {
                "layerselection2" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'layerselection2') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 10. Feature data
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'featuredata'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'featuredata');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Feature data",
        "fi" : "featuredata",
        "sv" : "featuredata",
        "en" : "featuredata",
        "bundlename" : "featuredata",
        "bundleinstancename" : "featuredata",
        "metadata" : {
            "Import-Bundle" : {
                "featuredata" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'featuredata') 
    AND view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "selectionTools": true
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'featuredata') 
    AND view_id=[VIEW_ID];


--------------------------------------------
-- 11. Promote - Personal data
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'promote'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'personaldata');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Personal data",
        "fi" : "personaldata",
        "sv" : "personaldata",
        "en" : "personaldata",
        "bundlename" : "promote",
        "bundleinstancename" : "personaldata",
        "metadata" : {
            "Import-Bundle" : {
                "promote" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                 }
             },
             "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'promote')
    AND bundleinstance = 'personaldata'
    AND view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "__name": "PersonalData",
        "title": {
            "en": "My data",
            "fi": "Omat tiedot",
            "sv": "Mina uppgifter"
        },
        "desc": {
            "en": "You can save map views and browse maps that you have embedded on other websites in My data.",
            "fi": "Omiin tietoihin voit tallentaa omia karttanäkymiä ja kohteita sekä nähdä muille sivustoille julkaisemasi kartat.",
            "sv": "Du kan lagra dina egna kartvyer och titta på kartor som du har inbäddat på andra webbplatser i Mina uppgifter."
        },
        "signup": {
            "en": "Log in",
            "fi": "Kirjaudu palveluun",
            "sv": "Logga in"
        },
        "signupUrl": {
            "en": "/web/en/login",
            "fi": "/web/fi/login",
            "sv": "/web/sv/login"
        },
        "register": {
            "en": "Register",
            "fi": "Rekisteröidy",
            "sv": "Rekisteröidy"
        },
        "registerUrl": {
            "en": "/web/en/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account",
            "fi": "/web/fi/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account",
            "sv": "/web/fi/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account"

        },
        "toolbarButtons": {
            "myplaces": {
                "point": {
                    "iconCls": "myplaces-draw-point",
                    "tooltip": {
                        "fi": "Lisää piste - Kirjaudu sisään käyttääksesi",
                        "sv": "Tillägg punkt - Logga in för att använda",
                        "en": "Add point - Log in to use"
                    }
                },
                "line": {
                    "iconCls": "myplaces-draw-line",
                    "tooltip": {
                        "fi": "Lisää viiva - Kirjaudu sisään käyttääksesi",
                        "sv": "Tillägg linje - Logga in för att använda",
                        "en": "Add line - Log in to use"
                    }
                },
                "area": {
                    "iconCls": "myplaces-draw-area",
                    "tooltip": {
                        "fi": "Lisää alue - Kirjaudu sisään käyttääksesi",
                        "sv": "Tillägg område - Logga in för att använda",
                        "en": "Add area - Log in to use"
                    }
                },
                "import" : {
                    "iconCls": "upload-material",
                    "tooltip": {
                        "fi": "Tuo oma aineisto - Kirjaudu sisään käyttääksesi",
                        "sv": "Importera egen datamängd - Logga in för att använda",
                        "en": "Import your own dataset - Log in to use"
                    }
                }
            }
        }
    }' WHERE bundle_id = (SELECT max(id) FROM portti_bundle WHERE name = 'promote')
    AND bundleinstance = 'personaldata'
    AND view_id=[VIEW_ID];



--------------------------------------------
-- 12. Promote - Publisher
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'promote'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'publisher');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Map publisher",
        "fi" : "publisher",
        "sv" : "publisher",
        "en" : "publisher",
        "bundlename" : "promote",
        "bundleinstancename" : "publisher",
        "metadata" : {
            "Import-Bundle" : {
                "promote" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT max(id) FROM portti_bundle WHERE name = 'promote')
    AND bundleinstance = 'publisher'
    AND view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "__name": "Publisher",
        "title": {
            "en": "Create map",
            "fi": "Julkaise kartta",
            "sv": "Definiera karta"
        },
        "desc": {
            "en": "You need to log in before using the embedding function.",
            "fi": "Voit käyttää julkaisutoimintoa kirjauduttuasi palveluun.",
            "sv": "Logga in i tjänsten för att definiera en karta som ska inbäddas."
        },
        "signup": {
            "en": "Log in",
            "fi": "Kirjaudu sisään",
            "sv": "Logga in"
        },
        "signupUrl": {
            "en": "/web/en/login",
            "fi": "/web/fi/login",
            "sv": "/web/sv/login"
        },
        "register": {
            "en": "Register",
            "fi": "Rekisteröidy",
            "sv": "Registrera dig"
        },
        "registerUrl": {
            "en": "/web/en/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account",
            "fi": "/web/fi/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account",
            "sv": "/web/sv/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account"
        }
    }' WHERE bundle_id = (SELECT max(id) FROM portti_bundle WHERE name = 'promote')
    AND bundleinstance = 'publisher'
    AND view_id=[VIEW_ID];



--------------------------------------------
-- 13. Coordinate display
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'coordinatedisplay'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'coordinatedisplay');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Coordinate display",
        "fi" : "coordinatedisplay",
        "sv" : "coordinatedisplay",
        "en" : "coordinatedisplay",
        "bundlename" : "coordinatedisplay",
        "bundleinstancename" : "coordinatedisplay",
        "metadata" : {
            "Import-Bundle" : {
                "coordinatedisplay" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'coordinatedisplay') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 14. Map Legend
--------------------------------------------

-- add bundle to view 
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'maplegend'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'maplegend');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Map legend",
        "fi" : "maplegend",
        "sv" : "maplegend",
        "en" : "maplegend",
        "bundlename" : "maplegend",
        "bundleinstancename" : "maplegend",
        "metadata" : {
            "Import-Bundle" : {
                "maplegend" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'maplegend') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 15. User Guide
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'userguide'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'userguide');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "User Guide",
        "fi" : "userguide",
        "sv" : "userguide",
        "en" : "userguide",
        "bundlename" : "userguide",
        "bundleinstancename" : "userguide",
        "metadata" : {
            "Import-Bundle" : {
                "userguide" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'userguide') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 16. Metadata flyout
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'metadataflyout'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'metadataflyout');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Metadata Flyout",
        "fi" : "metadataflyout",
        "sv" : "metadataflyout",
        "en" : "metadataflyout",
        "bundlename" : "metadataflyout",
        "bundleinstancename" : "metadataflyout",
        "metadata" : {
            "Import-Bundle" : {
                "metadataflyout" : {
                    "bundlePath" : "/Oskari/packages/catalogue/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'metadataflyout') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 17. Guided tour
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'guidedtour'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'guidedtour');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Guided Tour",
        "fi" : "guidedtour",
        "sv" : "guidedtour",
        "en" : "guidedtour",
        "bundlename" : "guidedtour",
        "bundleinstancename" : "guidedtour",
        "metadata" : {
            "Import-Bundle" : {
                "guidedtour" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'guidedtour') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 18. Backend status
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'backendstatus'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'backendstatus');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title": "Backend status",
        "bundleinstancename": "backendstatus",
        "fi": "backendstatus",
        "sv": "backendstatus",
        "en": "backendstatus",
        "bundlename": "backendstatus",
        "metadata": {
            "Import-Bundle": {
                "backendstatus": {
                    "bundlePath": "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance": [ ]
        },
        "instanceProps": {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'backendstatus') 
    AND view_id=[VIEW_ID];



--------------------------------------------
-- 19. Printout
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'printout'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'printout');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title": "Printout",
        "bundleinstancename": "printout",
        "fi": "printout",
        "sv": "printout",
        "en": "printout",
        "bundlename": "printout",
        "metadata": {
            "Import-Bundle": {
                "printout": {
                    "bundlePath": "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance": [ ]
        },
        "instanceProps": {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'printout') 
    AND view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "backendConfiguration" : { 
            "formatProducers" : { 
                "application/pdf" : "http://wps.paikkatietoikkuna.fi/dataset/map/process/imaging/service/thumbnail/maplink.pdf?", 
                "image/png" : "http://wps.paikkatietoikkuna.fi/dataset/map/process/imaging/service/thumbnail/maplink.png?"
            }
        },
         "legend" : {
             "general" : {
                 "legendWidth" : 0.27,
                 "legendRowHeight" : 0.02,
                 "charsInrow" : 32
             },
             "printAreaDefault" : {
                 "strokeColor" : "#00FF00",
                 "strokeOpacity" : 1,
                 "strokeWidth" : 1,
                 "fillColor" : "#FFFFFF",
                 "fillOpacity" : 0.2,
                 "fontColor" : "#000000",
                 "fontSize" : "12px",
                 "fontFamily" : "Liberation Sans",
                 "fontWeight" : "bold"
             },
             "legendBoxDefault" : {
                 "strokeColor" : "#00FF00",
                 "strokeOpacity" : 1,
                 "strokeWidth" : 0,
                 "fillColor" : "#FFFFFF",
                 "fillOpacity" : 0.7,
                 "labelAlign" : "l",
                 "label" : "${name}",
                 "fontColor" : "#000000",
                 "fontSize" : "12px",
                 "fontFamily" : "Liberation Sans",
                 "fontWeight" : "bold"
             },
             "colorBoxDefault" : {
                 "strokeColor" : "#000000",
                 "strokeOpacity" : 1,
                 "strokeWidth" : 1,
                 "fillColor" : "${color}",
                 "fillOpacity" : 1.0,
                 "label" : "${name}",
                 "labelAlign" : "l",
                 "labelXOffset" : 0,
                 "labelYOffset" : 5,
                 "fontFamily" : "Arial",
                 "fontSize" : "10px"
             }
         }
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'printout') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 20. Stats grid
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup) 
       VALUES ([VIEW_ID], 
        (SELECT id FROM portti_bundle WHERE name = 'statsgrid'), 
        (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
        '{}','{}', '{}');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
    "title": "Statistics grid",
    "bundleinstancename": "statsgrid",
    "fi": "statsgrid",
    "sv": "statsgrid",
    "en": "statsgrid",
    "bundlename": "statsgrid",
    "metadata": {
        "Import-Bundle": {
            "statsgrid": {
                "bundlePath": "/Oskari/packages/statistics/bundle/"
            },
            "geostats": {
                "bundlePath": "/Oskari/packages/libraries/bundle/"
            }
        },
        "Require-Bundle-Instance": [ ]
    },
    "instanceProps": {}
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'statsgrid') 
    AND  view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "name": "StatsGrid",
        "sandbox": "sandbox",
        "stateful" : true,
        "viewClazz": "Oskari.statistics.bundle.statsgrid.StatsView"
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'statsgrid') 
    AND  view_id=[VIEW_ID];

--------------------------------------------
-- 22. Promote - Analyse
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'promote'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'analyse');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "Analyse",
        "fi" : "Analyysi",
        "sv" : "Analys",
        "en" : "Analyse",
        "bundlename" : "promote",
        "bundleinstancename" : "analyse",
        "metadata" : {
            "Import-Bundle" : {
                "promote" : {
                    "bundlePath" : "/Oskari/packages/framework/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT max(id) FROM portti_bundle WHERE name = 'promote')
    AND bundleinstance = 'analyse'
    AND view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
        "__name": "Analyse",
        "title": {
            "en": "Analyse <font color=red>(BETA)</font>",
            "fi": "Analyysi <font color=red>(BETA)</font>",
            "sv": "Analys <font color=red>(BETA)</font>"
        },
        "desc": {
            "en": "You need to log in before using the analysis function.",
            "fi": "Voit käyttää Analyysitoimintoa kirjauduttuasi palveluun.",
            "sv": "Logga in i tjänsten för att använda analys funktioner."
        },
        "signup": {
            "en": "Log in",
            "fi": "Kirjaudu sisään",
            "sv": "Logga in"
        },
        "signupUrl": {
            "en": "/web/en/login",
            "fi": "/web/fi/login",
            "sv": "/web/sv/login"
        },
        "register": {
            "en": "Register",
            "fi": "Rekisteröidy",
            "sv": "Registrera dig"
        },
        "registerUrl": {
            "en": "/web/en/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account",
            "fi": "/web/fi/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account",
            "sv": "/web/sv/login?p_p_id=58&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&saveLastPath=0&_58_struts_action=%2Flogin%2Fcreate_account"
        }
    }' WHERE bundle_id = (SELECT max(id) FROM portti_bundle WHERE name = 'promote')
    AND bundleinstance = 'analyse'
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 23. Metadata Catalogue
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
    VALUES ([VIEW_ID], 
    (SELECT id FROM portti_bundle WHERE name = 'metadatacatalogue'), 
    (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]), 
    '{}','{}', '{}', 'metadatacatalogue');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
        "title" : "metadatacatalogue",
        "fi" : "metadatacatalogue",
        "sv" : "metadatacatalogue",
        "en" : "metadatacatalogue",
        "bundlename" : "metadatacatalogue",
        "bundleinstancename" : "metadatacatalogue",
        "metadata" : {
            "Import-Bundle" : {
                "metadatacatalogue" : {
                    "bundlePath" : "/Oskari/packages/catalogue/bundle/"
                }
            },
            "Require-Bundle-Instance" : []
        },
        "instanceProps" : {}
    }' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'metadatacatalogue') 
    AND view_id=[VIEW_ID];

--------------------------------------------
-- 24. Route Search
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup) 
       VALUES ([VIEW_ID],
        (SELECT id FROM portti_bundle WHERE name = 'routesearch'), 
        (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]),
        '{}','{}', '{}');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
    "title": "Route Search",
    "bundleinstancename": "routesearch",
    "fi": "Reittihaku",
    "sv": "Ruttsök",
    "en": "Route Search",
    "bundlename": "routesearch",
    "metadata": {
        "Import-Bundle": {
            "routesearch": {
                "bundlePath": "/Oskari/packages/paikkatietoikkuna/bundle/"
            }
        },
        "Require-Bundle-Instance": [ ]
    },
    "instanceProps": {}
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'routesearch') 
    AND  view_id=[VIEW_ID];

-- update proper config for view
UPDATE portti_view_bundle_seq set config = '{
    "flyoutClazz": "Oskari.mapframework.bundle.routesearch.Flyout"
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'routesearch')
         AND  view_id=[VIEW_ID];

--------------------------------------------
-- 24. FindByCoordinates
--------------------------------------------

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup) 
       VALUES ([VIEW_ID],
        (SELECT id FROM portti_bundle WHERE name = 'findbycoordinates'), 
        (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]),
        '{}','{}', '{}');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
    "title" : "FindByCoordinates",
    "bundlename" : "findbycoordinates",
    "bundleinstancename" : "findbycoordinates",
    "metadata" : {
    "Import-Bundle" : {
    "findbycoordinates" : {
    "bundlePath" : "/Oskari/packages/framework/bundle/"
    }
    },
    "Require-Bundle-Instance" : []
    },
    "instanceProps" : {}
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'findbycoordinates') 
    AND  view_id=[VIEW_ID];

--------------------------------------------
-- 25. Heatmap
--------------------------------------------
INSERT INTO portti_view_bundle_seq (view_id, seqno, bundle_id, startup, config, state)
VALUES ([VIEW_ID],
        (SELECT (max(seqno) + 1) FROM portti_view_bundle_seq WHERE view_id = [VIEW_ID]),
        (SELECT id FROM portti_bundle WHERE name = 'heatmap'),
        (SELECT startup FROM portti_bundle WHERE name = 'heatmap'),
        (SELECT config FROM portti_bundle WHERE name = 'heatmap'),
        (SELECT state FROM portti_bundle WHERE name = 'heatmap'));