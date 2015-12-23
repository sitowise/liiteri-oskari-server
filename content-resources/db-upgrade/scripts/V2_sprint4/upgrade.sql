-- Table: oskari_user_gis_data

-- DROP TABLE oskari_user_gis_data;

CREATE TABLE oskari_user_gis_data
(
  id serial NOT NULL,
  data_id integer NOT NULL,
  data_type character varying(255) NOT NULL,
  expirationdate date NOT NULL,
  userid integer NOT NULL,
  status character varying(255),
  CONSTRAINT "PK_OSKARI_USER_GIS_DATA" PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE oskari_user_gis_data
  OWNER TO oskari;
