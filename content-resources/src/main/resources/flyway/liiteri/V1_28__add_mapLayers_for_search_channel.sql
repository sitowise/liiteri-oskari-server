UPDATE public.portti_view_bundle_seq
SET config = '{
	"channels": {
		"KTJ_KII_CHANNEL": {
			"mapLayers": [555, 556]
		}
	}
}'

WHERE bundle_id = (SELECT id FROM public.portti_bundle WHERE name = 'search')
