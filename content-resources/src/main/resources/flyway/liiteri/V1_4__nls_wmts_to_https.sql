update oskari_maplayer set url = replace(url, 'http://liiteri.ymparisto.fi', 'https://liiteri.ymparisto.fi') where url like '%http://liiteri.ymparisto.fi%';

update oskari_resource set resource_mapping = replace(resource_mapping, 'http://liiteri.ymparisto.fi', 'https://liiteri.ymparisto.fi') where resource_mapping like '%http://liiteri.ymparisto.fi%';

update oskari_capabilities_cache set url = replace(url, 'http://liiteri.ymparisto.fi', 'https://liiteri.ymparisto.fi') where url like '%http://liiteri.ymparisto.fi%';
