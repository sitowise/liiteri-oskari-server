--remove resources related to unassigned map layers
DELETE FROM oskari_resource r
WHERE r.resource_type = 'maplayer'
AND r.resource_mapping IN
    (
SELECT l.type || '+' || l.url || '+' || l.name AS resource_name FROM oskari_maplayer l
WHERE NOT EXISTS (SELECT * FROM oskari_maplayer_group_link WHERE maplayerid = l.id)
AND name <> 'oskari:my_places' AND name <> 'oskari:vuser_layer_data' AND name <> 'oskari:analysis_data'
AND type <> 'statslayer');

--remove unassigned map layers
DELETE FROM oskari_maplayer a
WHERE NOT EXISTS (SELECT * FROM oskari_maplayer_group_link WHERE maplayerid = a.id)
AND name <> 'oskari:my_places' AND name <> 'oskari:vuser_layer_data' AND name <> 'oskari:analysis_data'
AND type <> 'statslayer';

--remove permissions of not existing resources
DELETE FROM oskari_permission p
WHERE NOT EXISTS (SELECT * FROM oskari_resource r WHERE r.id = p.oskari_resource_id);

--remove data related to not existing wfs layers
DELETE FROM portti_wfs_layer w
WHERE NOT EXISTS (SELECT * FROM oskari_maplayer l WHERE l.id = w.maplayer_id);

--remove style assignments to not existing wfs layers
DELETE FROM portti_wfs_layers_styles s
WHERE NOT EXISTS (SELECT * FROM portti_wfs_layer w WHERE w.id = s.wfs_layer_id);

