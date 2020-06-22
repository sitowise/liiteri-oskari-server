INSERT INTO liiteri_sharing (credentialid, credentialtype, resourceid, resourcetype, email, status, token)
(SELECT (SELECT id FROM oskari_roles WHERE name = 'liiteri_admin_light'), credentialtype, resourceid, resourcetype, email, status, token 
 FROM liiteri_sharing
 WHERE credentialtype = 'ROLE' AND credentialid = (SELECT id FROM oskari_roles WHERE name = 'liiteri_admin'));