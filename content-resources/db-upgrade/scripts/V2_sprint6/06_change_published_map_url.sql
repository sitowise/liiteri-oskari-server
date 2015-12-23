--for test server
UPDATE portti_bundle set config = '{
		"publishedMapUrl": "http://liiteritesti.ymparisto.fi/?viewId="
	}' WHERE name = 'publisher';
	
UPDATE portti_view_bundle_seq set config = '{
		"publishedMapUrl":{
			"fi":"http://liiteritesti.ymparisto.fi/?viewId=",
			"sv":"http://liiteritesti.ymparisto.fi/?viewId=",
			"en":"http://liiteritesti.ymparisto.fi/?viewId="}
	}' WHERE bundleinstance = 'publisher';
	
--for prod server
UPDATE portti_bundle set config = '{
		"publishedMapUrl": "http://liiteri.ymparisto.fi/?viewId="
	}' WHERE name = 'publisher';

UPDATE portti_view_bundle_seq set config = '{
		"publishedMapUrl":{
			"fi":"http://liiteri.ymparisto.fi/?viewId=",
			"sv":"http://liiteri.ymparisto.fi/?viewId=",
			"en":"http://liiteri.ymparisto.fi/?viewId="}
	}' WHERE bundleinstance = 'publisher';