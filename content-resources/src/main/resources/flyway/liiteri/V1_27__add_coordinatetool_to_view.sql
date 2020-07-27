INSERT INTO public.portti_view_bundle_seq (view_id, bundle_id, seqno, config, state, startup, bundleinstance)
	VALUES (
	1, 
	(SELECT id FROM portti_bundle WHERE name='coordinatetool'), 
	(SELECT max(seqno)+1 FROM portti_view_bundle_seq WHERE view_id=1), 
	'{}', 
	'{}', 
	(SELECT startup FROM portti_bundle WHERE name='coordinatetool'), 
	'coordinatetool'
	);