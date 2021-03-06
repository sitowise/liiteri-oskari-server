UPDATE portti_view_bundle_seq set config = '{
			"backendConfiguration": {
				"formatProducers": {
					"image/png": "?action_route=GetPreview&format=image/png&",
					"application/pdf": "?action_route=GetPreview&format=application/pdf&"
				}
			},
			"legend": {
				"printAreaDefault": {
					"fontWeight": "bold",	
					"fontColor": "#000000",
					"fillColor": "#FFFFFF",
					"fontSize": "12px",
					"strokeColor": "#00FF00",
					"fillOpacity": 0.2,
					"strokeWidth": 1,
					"strokeOpacity": 1
				},
				"colorBoxDefault": {
					"fillColor": "${color}",
					"labelXOffset": 0,
					"label": "${name}",
					"labelAlign": "l",
					"fontSize": "10px",
					"strokeColor": "#000000",
					"labelYOffset": 5,
					"fillOpacity": 1,
					"strokeWidth": 1,
					"strokeOpacity": 1
				},
				"legendBoxDefault": {
					"fontWeight": "bold",
					"fontColor": "#000000",
					"fillColor": "#FFFFFF",
					"label": "${name}",
					"labelAlign": "l",
					"fontSize": "12px",
					"strokeColor": "#00FF00",
					"fillOpacity": 0.7,
					"strokeWidth": 0,
					"strokeOpacity": 1
				},
				"general": {
					"legendRowHeight": 0.02,
					"legendWidth": 0.27,
					"charsInrow": 28
				}
			}
		}' WHERE bundle_id = (SELECT id FROM portti_bundle WHERE name = 'printout') 
AND view_id=(SELECT id FROM portti_view WHERE type='DEFAULT');