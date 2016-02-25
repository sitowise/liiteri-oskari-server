# Users database

## To enable geometry editor in the analyse bundle:

Analyse bundle startup has changed:
oskari-server/content-resources/src/main/resources/sql/views/02-default-view.sql
Note: only the startup has changed, only run update analysis startup.

oskari-server/content-resources/src/main/resources/sql/views/01-bundles/analysis/001-analyse.sql

# Keyword tables

admin-layerselector requires layer keyword tables to work correctly. Add them by running:

oskari-server/content-resources/src/main/resources/sql/PostgreSQL/create-keyword-tables.sql

# External role mappings

Add oskari_role_external_mapping table from:

oskari-server/content-resources/src/main/resources/sql/PostgreSQL/create-user-tables.sql

For LDAP or other external authentication system to provide roles we need to have mapping from external role names to oskari roles.
This table can be used to do such mapping.