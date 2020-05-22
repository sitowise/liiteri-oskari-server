INSERT INTO oskari_roles(
	id, name, is_guest)
	VALUES ((select max(id)+1 from oskari_roles), 'syke_admin', false);