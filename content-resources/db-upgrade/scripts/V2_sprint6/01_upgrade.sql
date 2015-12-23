update portti_view_bundle_seq set state = '{"selectedLayers":[{"id":10}],"zoom":0,"north":"7200000","east":"500000"}' where view_id = 1 and bundleinstance = 'mapfull';

ALTER TABLE oskari_maplayer ADD COLUMN copyright_info character varying(255);