/* Replace NULL config value or/and NULL state value with empty object to avoid null reference errors
*/
 
UPDATE public.portti_bundle
	SET config = '{}'
	WHERE name = 'admin-hierarchical-layerlist' and config is NULL;

UPDATE public.portti_bundle
	SET state = '{}'
	WHERE name = 'admin-hierarchical-layerlist' and state is NULL;