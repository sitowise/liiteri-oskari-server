 update oskari_maplayer set metadataid=replace(metadataid, 'geonetwork/srv/en/main.home?uuid=','geonetwork?uuid=') where metadataid like '%paikkatietohakemisto%';
 update oskari_maplayer set metadataid=replace(metadataid, 'geonetwork/srv/fi/iso19139.xml?uuid=','geonetwork?uuid=') where metadataid like '%paikkatietohakemisto%';
 update oskari_maplayer set metadataid=replace(metadataid, 'geonetwork/srv/fi/main.home?uuid=','geonetwork?uuid=') where metadataid like '%paikkatietohakemisto%';
 update oskari_maplayer set metadataid=replace(metadataid, 'geonetwork/srv/fi/metadata.show.portti.metaMultiLingual?uuid=','geonetwork?uuid=') where metadataid like '%paikkatietohakemisto%';
 update oskari_maplayer set metadataid=replace(metadataid, 'geonetwork/srv/fi/metadata.show?uuid=','geonetwork?uuid=') where metadataid like '%paikkatietohakemisto%';
 
 update oskari_maplayer set metadataid=replace(metadataid, 'uuid=%20','uuid=') where metadataid like '%paikkatietohakemisto%';
 
 update oskari_maplayer set metadataid=replace(metadataid, 'uuid= ','uuid=') where metadataid like '%paikkatietohakemisto%';