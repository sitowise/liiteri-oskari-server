-- Column: only_label

-- ALTER TABLE my_places DROP COLUMN only_label;

ALTER TABLE my_places ADD COLUMN only_label boolean;


-- Column: download_service_url

-- ALTER TABLE oskari_user_gis_data DROP COLUMN download_service_url;

ALTER TABLE oskari_user_gis_data ADD COLUMN download_service_url character varying(255);


-- Column: download_service_url

-- ALTER TABLE oskari_maplayer DROP COLUMN download_service_url;

ALTER TABLE oskari_maplayer ADD COLUMN download_service_url character varying(255);
