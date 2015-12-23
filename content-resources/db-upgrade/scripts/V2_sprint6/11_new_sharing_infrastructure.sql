BEGIN TRANSACTION;
CREATE TABLE liiteri_sharing
(
  id serial NOT NULL,  
  credentialId integer NOT NULL,
  credentialType character varying(255),
  resourceId integer NOT NULL,
  resourceType character varying(255),
  email character varying(255),
  status character varying(255),
  token character varying(255),
  CONSTRAINT "PK_LIITERI_SHARING" PRIMARY KEY (id)
);

INSERT INTO liiteri_sharing
(resourceId, resourcetype, credentialId, credentialType, email, status)
(SELECT workspaceid, 'WORKSPACE', externalid, externaltype, email, 'SHARED' FROM oskari_workspaces_sharing where externalid <> 0);

DELETE from oskari_workspaces_sharing;

DROP TABLE oskari_workspaces_sharing;

INSERT INTO liiteri_sharing
(resourceId, resourcetype, credentialId, credentialType, email, status)
(SELECT dataset_id, 'LAYER', external_id, external_type, email, 'SHARED' FROM oskari_user_gis_data_sharing where external_id <> 0);

DELETE from oskari_user_gis_data_sharing;

DROP TABLE oskari_user_gis_data_sharing;

INSERT INTO liiteri_sharing
(resourceId, resourcetype, credentialId, credentialType, email, status)
(SELECT oskarigroupingid, 'THEME', externalid, externaltype, email, 'SHARED' FROM oskari_groupings_permissions where externalid <> 0 and is_theme = true);

INSERT INTO liiteri_sharing
(resourceId, resourcetype, credentialId, credentialType, email, status)
(SELECT oskarigroupingid, 'SERVICE_PACKAGE', externalid, externaltype, email, 'SHARED' FROM oskari_groupings_permissions where externalid <> 0 and is_theme = false);

DELETE from oskari_groupings_permissions;

DROP TABLE oskari_groupings_permissions;

COMMIT;