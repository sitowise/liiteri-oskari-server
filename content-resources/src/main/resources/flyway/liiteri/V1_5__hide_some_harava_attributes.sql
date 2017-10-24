update portti_wfs_layer set selected_feature_params = '{"default": ["paikka_nimi","kyselyn_nimi","kysely_id","harava_organisaatio_nimi","url"]}'
where layer_name like 'oskari:v_harava%';