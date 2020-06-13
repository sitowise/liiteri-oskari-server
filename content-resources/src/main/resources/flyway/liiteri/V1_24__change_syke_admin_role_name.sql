-- Delete 'syke_admin' role and related permissions
DELETE FROM oskari_permission WHERE external_type = 'ROLE' AND external_id = (SELECT id::varchar FROM oskari_roles WHERE name = 'syke_admin');
DELETE FROM oskari_roles WHERE name = 'syke_admin';

-- Add 'liiteri_admin_light' role if not exists and assign to it the same permissions as 'liiteri_admin' has
INSERT INTO oskari_roles(id, name, is_guest)
	SELECT (select max(id)+1 from oskari_roles), 'liiteri_admin_light', false
	WHERE NOT EXISTS (SELECT * FROM oskari_roles WHERE name = 'liiteri_admin_light');
	
INSERT INTO oskari_permission (oskari_resource_id, external_type, permission, external_id)
(SELECT oskari_resource_id, external_type, permission, (SELECT id::varchar FROM oskari_roles WHERE name = 'liiteri_admin_light') 
 FROM oskari_permission
 WHERE external_type = 'ROLE' AND external_id = (SELECT id::varchar FROM oskari_roles WHERE name = 'liiteri_admin'));