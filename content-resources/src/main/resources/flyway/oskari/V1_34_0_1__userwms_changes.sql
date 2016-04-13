-- add columns for username and password;
ALTER TABLE oskari_user_maplayer ADD COLUMN username character varying(256);
ALTER TABLE oskari_user_maplayer ADD COLUMN password character varying(256);

ALTER TABLE oskari_user_maplayer
   ADD  srs_name character varying,
   ADD  version character varying(64);

ALTER TABLE oskari_user_maplayer
   ADD attributes text DEFAULT '{}';

ALTER TABLE oskari_user_maplayer
  ADD COLUMN capabilities TEXT DEFAULT '{}';

update oskari_user_maplayer set srs_name = 'EPSG:3067', version = '1.1.1';
