/* Flyway showed error: No value provided for placeholder expressions: ${color}, ${name}.  Check your configuration!
To avoid this error here is done work-around to replacing '#{' to '${' during execution of this script.

Fix for V1_10_change_printout_legend_config.sql

 */
 
UPDATE public.portti_view_bundle_seq
	SET config=
	REPLACE('{
"backendConfiguration": {
"formatProducers": {
"image/png": "action?action_route=GetPreview&format=image/png&",
"application/pdf": "action?action_route=GetPreview&format=application/pdf&"
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
"fillColor": "#{color}",
"labelXOffset": 20,
"label": "#{name}",
"labelAlign": "l",
"fontSize": "10px",
"strokeColor": "#000000",
"labelYOffset": -3,
"fillOpacity": 1,
"strokeWidth": 1,
"strokeOpacity": 1
},
"legendBoxDefault": {
"fontWeight": "bold",
"fontColor": "#000000",
"fillColor": "#FFFFFF",
"label": "#{name}",
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
}', '#{', '${')
	
	WHERE bundle_id = (SELECT id FROM public.portti_bundle WHERE name = 'printout')