package org.geoserver.wps.oskari.oskari;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

@DescribeProcess(title = "buffer2", description = "test -- buffer")
public class BufferFeatureCollection2 implements GSProcess {

	@DescribeResult(description = "buffer with or without original feature")
	public SimpleFeatureCollection execute(
			@DescribeParameter(name = "features", description = "features") SimpleFeatureCollection features,
			@DescribeParameter(name = "distance", description = "buffer size") Double distance,
			@DescribeParameter(name = "attributeName", description = "attributeName", min = 0) String attributeName,			
			@DescribeParameter(name = "includeOriginal", description = "includeOriginal", min = 0, defaultValue = "True") Boolean includeOriginal,
			@DescribeParameter(name = "mergeBuffers", description = "mergeBuffers", min = 0, defaultValue = "False") Boolean mergeBuffers) {

		if (includeOriginal == null) {
			includeOriginal = true;
		}
		
		List<SimpleFeature> ret = new ArrayList<SimpleFeature>();
		
		SimpleFeatureTypeBuilder mergedFtb = new SimpleFeatureTypeBuilder();
		Map<String, String> attributeMap = new HashMap<String, String>();
		Geometry mergedGeometry = null;
		
		SimpleFeatureIterator iter = features.features();

		while (iter.hasNext()) {

			SimpleFeature feature = iter.next();
			
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor descriptor : feature.getFeatureType().getAttributeDescriptors()) {
                if (!(descriptor.getType() instanceof GeometryTypeImpl)
                        || (!feature.getFeatureType().getGeometryDescriptor().equals(descriptor))) {
                    tb.add(descriptor);
					
					if (mergeBuffers == true) {
						mergedFtb.add(descriptor);
					}
					
                } else {
                    AttributeTypeBuilder builder = new AttributeTypeBuilder();
                    builder.setBinding(MultiPolygon.class);
                    AttributeDescriptor attributeDescriptor = builder.buildDescriptor(descriptor
                            .getLocalName(), builder.buildType());
                    tb.add(attributeDescriptor);
                    if(tb.getDefaultGeometry() == null) {
                        tb.setDefaultGeometry(descriptor.getLocalName());
                    }
					
					if (mergeBuffers == true) {
						mergedFtb.add(descriptor);
						
						if(mergedFtb.getDefaultGeometry() == null) {
							mergedFtb.setDefaultGeometry(descriptor.getLocalName());
						}
					}
                }
            }
            tb.setDescription(feature.getFeatureType().getDescription());
            tb.setCRS(feature.getFeatureType().getCoordinateReferenceSystem());
            tb.setName(feature.getFeatureType().getName());
            
			if (mergeBuffers == true) {
				mergedFtb.setDescription(feature.getFeatureType().getDescription());
				mergedFtb.setCRS(feature.getFeatureType().getCoordinateReferenceSystem());
				mergedFtb.setName(feature.getFeatureType().getName());
			}
			
            SimpleFeatureType sft  = tb.buildFeatureType();
            SimpleFeatureBuilder fb = new SimpleFeatureBuilder(sft);
            
			Geometry g = (Geometry) feature.getDefaultGeometry();
			
			Double bufferSize = distance;
			
			Iterator<AttributeDescriptor> firstIterator = feature.getType().getAttributeDescriptors()
                    .iterator();
            while (firstIterator.hasNext()) {
                AttributeDescriptor ad = firstIterator.next();
                Object firstAttribute = feature.getAttribute(ad.getLocalName());
                if (!(firstAttribute instanceof Geometry)) {
                    fb.set(ad.getLocalName(), feature.getAttribute(ad.getLocalName()));
					
					if (mergeBuffers == true) {
						String attrValue = "";
						String oldValue = attributeMap.get(ad.getLocalName());
						if (oldValue != null) {
							attrValue = oldValue + " ";
						}
						if (feature.getAttribute(ad.getLocalName()) != null) {
							attrValue += feature.getAttribute(ad.getLocalName()).toString();
						}
						
						attributeMap.put(ad.getLocalName(), attrValue);
					}
                }
    			if (attributeName != null && ad.getLocalName().equals(attributeName)) {
    				try{
    					bufferSize = Double.parseDouble(feature.getAttribute(ad.getLocalName()).toString());
    				} catch (NumberFormatException ex) {
    					//use default value
    					bufferSize = distance;
    				}    				
    			}
            }		
			
			Geometry buffered = g.buffer(bufferSize);

			if (!includeOriginal) {
				buffered = buffered.symDifference(g);
			}

			if (buffered instanceof Polygon) {
				buffered = buffered.getFactory().createMultiPolygon(
						new Polygon[] { (Polygon) buffered });
			}
            
            SimpleFeature sf = fb.buildFeature(feature.getID());
			
			sf.setDefaultGeometry(buffered);
			
			if (mergeBuffers == true) {
				if (mergedGeometry == null) {
					mergedGeometry = buffered;
				} else {
					mergedGeometry = mergedGeometry.union(buffered);
				}
			}
			
			ret.add(sf);
		}
		
		if (mergeBuffers == true) {
				
				ret = new ArrayList<SimpleFeature>();
				
				SimpleFeatureType sft  = mergedFtb.buildFeatureType();
				SimpleFeatureBuilder fb = new SimpleFeatureBuilder(sft);
				
				Iterator it = attributeMap.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry attributeEntry = (Map.Entry)it.next();
					fb.set(attributeEntry.getKey().toString(), attributeEntry.getValue());
				}
				
				SimpleFeature sf = fb.buildFeature(null);
				
				sf.setDefaultGeometry(mergedGeometry);
				
				ret.add(sf);
			
		}
		
		return DataUtilities.collection(ret);
	}

    protected SimpleFeatureType getTargetSchema(SimpleFeatureType sourceSchema) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor ad : sourceSchema.getAttributeDescriptors()) {
            GeometryDescriptor defaultGeometry = sourceSchema.getGeometryDescriptor();
            if(ad == defaultGeometry) {
                tb.add(ad.getName().getLocalPart(), MultiPolygon.class, defaultGeometry.getCoordinateReferenceSystem());
            } else {
                tb.add(ad);
            }
        }
        tb.setName(sourceSchema.getName());
        return tb.buildFeatureType();
    }
}