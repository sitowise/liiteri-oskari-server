--for test db
insert into oskari_resource (resource_type, resource_mapping) values ('operation', 'statistics+grid'); --355

insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 5); --"liiteri_env_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 6); --"liiteri_groupings_admin"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 7); --"liiteri_admin"


insert into oskari_resource (resource_type, resource_mapping) values ('operation', 'statistics+restricted');

insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 5); --"liiteri_env_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 6); --"liiteri_groupings_admin"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 7); --"liiteri_admin"


-------------------
--for production db
insert into oskari_resource (resource_type, resource_mapping) values ('operation', 'statistics+grid'); --355

insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 7); --"liiteri_admin"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 12); --"liiteri_vira_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 5); --"liiteri_env_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 6); --"liiteri_groupings_admin"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 15); --"liiteri_authorized_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 16); --"liiteri_kunnat_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 11); --"liiteri_mkl_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+grid'),
'ROLE', 'EXECUTE', 18); --"liiteri_yri_user"


insert into oskari_resource (resource_type, resource_mapping) values ('operation', 'statistics+restricted');

insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 7); --"liiteri_admin"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 12); --"liiteri_vira_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 5); --"liiteri_env_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 6); --"liiteri_groupings_admin"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 15); --"liiteri_authorized_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 16); --"liiteri_kunnat_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 11); --"liiteri_mkl_user"
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+restricted'),
'ROLE', 'EXECUTE', 18); --"liiteri_yri_user"
