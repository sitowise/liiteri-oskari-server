INSERT INTO oskari_permission (oskari_resource_id, external_type, permission, external_id)
(SELECT oskari_resource_id, external_type, permission, (SELECT id::varchar FROM oskari_roles WHERE name = 'syke_admin') 
 FROM oskari_permission
 WHERE external_type = 'ROLE' AND external_id = (SELECT id::varchar FROM oskari_roles WHERE name = 'liiteri_admin'));