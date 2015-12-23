CREATE TABLE oskari_user_maplayer
(
  id serial NOT NULL,
  user_id integer NOT NULL,
  parentid integer NOT NULL DEFAULT (-1),
  externalid character varying(50),
  type character varying(50) NOT NULL,
  base_map boolean NOT NULL DEFAULT false,
  groupid integer,
  name character varying(2000),
  url character varying(2000),
  locale text,
  opacity integer DEFAULT 100,
  style character varying(100),
  minscale double precision DEFAULT (-1),
  maxscale double precision DEFAULT (-1),
  legend_image character varying(2000),
  metadataid character varying(200),
  tile_matrix_set_id character varying(200),
  tile_matrix_set_data text,
  params text DEFAULT '{}'::text,
  options text DEFAULT '{}'::text,
  gfi_type character varying(200),
  gfi_xslt text,
  gfi_content text,
  realtime boolean DEFAULT false,
  refresh_rate integer DEFAULT 0,
  created timestamp with time zone,
  updated timestamp with time zone,
  download_service_url character varying(255),
  copyright_info character varying(255),
  CONSTRAINT oskari_user_maplayer_pkey PRIMARY KEY (id)
)

INSERT INTO portti_bundle (name, startup) 
       VALUES ('user-layers','{}');

UPDATE portti_bundle set startup = '{
    "title" : "user-layers",
    "fi" : "user-layers",
    "sv" : "user-layers",
    "en" : "user-layers",
    "bundlename" : "user-layers",
    "bundleinstancename" : "user-layers",
    "metadata" : {
        "Import-Bundle" : {
            "user-layers" : {
                "bundlePath" : "/Oskari/packages/liiteri/bundle/"
            }
        },
        "Require-Bundle-Instance" : []
    },
    "instanceProps" : {}
}' WHERE name = 'user-layers';

-- add bundle to view
INSERT INTO portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance) 
       VALUES ((SELECT id FROM portti_view WHERE type='DEFAULT'), 
        (SELECT id FROM portti_bundle WHERE name = 'user-layers'), 
        290, '{}','{}', '{}', 'user-layers');

-- update proper startup for view
UPDATE portti_view_bundle_seq set startup = '{
    "title" : "user-layers",
    "fi" : "user-layers",
    "sv" : "user-layers",
    "en" : "user-layers",
    "bundlename" : "user-layers",
    "bundleinstancename" : "user-layers",
    "metadata" : {
        "Import-Bundle" : {
            "user-layers" : {
                "bundlePath" : "/Oskari/packages/liiteri/bundle/"
            }
        },
        "Require-Bundle-Instance" : []
    },
    "instanceProps" : {}
}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'user-layers') 
    AND view_id=(SELECT id FROM portti_view WHERE type='DEFAULT');
