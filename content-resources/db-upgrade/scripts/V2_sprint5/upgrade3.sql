insert into oskari_resource (resource_type, resource_mapping) values ('operation', 'statistics+functional_intersection');

insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+functional_intersection'),
'ROLE', 'EXECUTE', 5);
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+functional_intersection'),
'ROLE', 'EXECUTE', 6);
insert into oskari_permission (oskari_resource_id, external_type, permission, external_id) values (
(select id from oskari_resource where resource_type = 'operation' AND resource_mapping = 'statistics+functional_intersection'),
'ROLE', 'EXECUTE', 7);