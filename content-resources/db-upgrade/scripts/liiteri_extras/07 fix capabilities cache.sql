/*add new column for primary key*/
ALTER TABLE portti_capabilities_cache DROP CONSTRAINT portti_capabilities_cache_pkey;
ALTER TABLE portti_capabilities_cache ADD COLUMN id SERIAL;
UPDATE portti_capabilities_cache SET id = nextval(pg_get_serial_sequence('portti_capabilities_cache','id'));
ALTER TABLE portti_capabilities_cache ADD CONSTRAINT portti_capabilities_cache_pkey PRIMARY KEY(id);

/*add new column for check if the layer is a user own WMS layer*/
ALTER TABLE portti_capabilities_cache ADD COLUMN user_wms boolean;

/*update existing rows*/
UPDATE portti_capabilities_cache SET user_wms = FALSE WHERE user_wms IS NULL;