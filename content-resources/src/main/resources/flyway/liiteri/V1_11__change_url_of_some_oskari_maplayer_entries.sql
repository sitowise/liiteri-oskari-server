UPDATE oskari_maplayer
	SET url = 'http://localhost:8002/geoserver/oskari/wfs'
	WHERE name = 'oskari:my_places' OR name = 'oskari:vuser_layer_data' OR name = 'oskari:analysis_data'