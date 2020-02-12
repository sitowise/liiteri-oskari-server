package fi.nls.oskari.service;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.apache.commons.io.IOUtils;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ShapefileService {
	private final Logger LOGGER = LogFactory.getLogger(ShapefileService.class);
	private String outputFinalName;
	
	public ShapefileService(String outputFinalName) {
		this.outputFinalName = outputFinalName;
	}
	
	public void exportStatisticsToShp(OutputStream out, String featureCollectionParam) {
		try {
			// read input geojson to FeatureCollection object
			FeatureCollection<SimpleFeatureType, SimpleFeature> inputFeatureCollection = null;
			
			if (!featureCollectionParam.isEmpty()) {
				FeatureJSON featureJSON = new FeatureJSON();
				Reader reader = new StringReader(featureCollectionParam);
				inputFeatureCollection = featureJSON.readFeatureCollection(reader);
			}
			
			if (inputFeatureCollection == null) {
				//TODO add log and finish processing
			}
			
			// Create feature type
			final SimpleFeatureType TYPE_FOR_SHP = createFeatureType(inputFeatureCollection);
			
			// Create temporary file for shp and datastore
			File tempShapeFile = File.createTempFile("shpFile", ".shp");
			
			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
			
			Map<String, Serializable> shpParams = new HashMap<String, Serializable>();
			shpParams.put(ShapefileDataStoreFactory.URLP.key, tempShapeFile.toURI().toURL());
			shpParams.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
			
			ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(shpParams);
			dataStore.createSchema(TYPE_FOR_SHP);
			
			String typeName = dataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
			
			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				
				SimpleFeatureCollection collection = collectFeaturesForShp(inputFeatureCollection, TYPE_FOR_SHP);
				
				//add collection of features to datastore in transaction
				Transaction transaction = new DefaultTransaction("create");
				featureStore.setTransaction(transaction);
				try {
					featureStore.addFeatures(collection);
					transaction.commit();
				} catch (Exception problem) {
					problem.printStackTrace();
					transaction.rollback();
					//TODO log error
				} finally {
					transaction.close();
				}
				
				generateZipFile(out, tempShapeFile);
				
				out.flush();
				
			} else {
				//TODO log error
			}
		} catch (Exception e) {
			e.printStackTrace();
			//TODO log error
		}
	}
	
	/**
	 * Create feature type for feature collection to create shapefile
	 */
	private static SimpleFeatureType createFeatureType(FeatureCollection<SimpleFeatureType, SimpleFeature> inputFeatureCollection) throws Exception {
		
		SimpleFeatureType inSchema = inputFeatureCollection.getSchema();
		List<AttributeDescriptor> inAttributes = inSchema.getAttributeDescriptors();
		GeometryType inGeomType = null;
		List<AttributeDescriptor> outAttributes = new ArrayList<AttributeDescriptor>();
		
		// rewrite all attributes except geometry one
		for (AttributeDescriptor attribute : inAttributes) {
			
			AttributeType type = attribute.getType();
			if (type instanceof GeometryType) {
				inGeomType = (GeometryType) type;
			} else {
				outAttributes.add(attribute);
			}
		}
		
		// create geometry type and add it at first position in attribute list
		GeometryTypeImpl outGeomType = new GeometryTypeImpl(
				new NameImpl("the_geom"), inGeomType.getBinding(),
				inGeomType.getCoordinateReferenceSystem(),
				inGeomType.isIdentified(), inGeomType.isAbstract(),
				inGeomType.getRestrictions(), inGeomType.getSuper(),
				inGeomType.getDescription());
		
		GeometryDescriptor inGeomDesc = inSchema.getGeometryDescriptor();
		GeometryDescriptor outGeomDesc = new GeometryDescriptorImpl(
				outGeomType, new NameImpl("the_geom"),
				inGeomDesc.getMinOccurs(), inGeomDesc.getMaxOccurs(),
				inGeomDesc.isNillable(), inGeomDesc.getDefaultValue());
		
		outAttributes.add(0, outGeomDesc);
		
		//generate feature type for shapefile
		SimpleFeatureType shpType = new SimpleFeatureTypeImpl(
				inSchema.getName(), outAttributes, outGeomDesc,
				inSchema.isAbstract(), inSchema.getRestrictions(),
				inSchema.getSuper(), inSchema.getDescription());
		
		return shpType;
	}
	
	/**
	 * Generate ZIP file based on component files (SHP, SHX, DBF, PRJ)
	 */
	private void generateZipFile(OutputStream out, File tempShapefile) throws Exception {
		// create zip based on generated files
		ZipOutputStream os = new ZipOutputStream(out);
		
		final String fileNamePrefix = tempShapefile.getName().substring(0, tempShapefile.getName().lastIndexOf("."));
		
		File[] shpComponentFiles = new File(tempShapefile.getParent()).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.contains(fileNamePrefix);
			}
		});
		
		for (File file : shpComponentFiles) {
			os.putNextEntry(new ZipEntry(outputFinalName + file.getName().substring(file.getName().lastIndexOf("."))));
			IOUtils.copy(new FileInputStream(file), os);
			file.delete();
		}
		
		os.close();
	}
	
	/**
	 * Rewrite features from input FeatureCollection to collection for shapefile.
	 */
	private SimpleFeatureCollection collectFeaturesForShp(FeatureCollection<SimpleFeatureType, SimpleFeature> inputFeatureCollection, SimpleFeatureType TYPE_FOR_SHP) {
		// list to collect features from geojson input to shapefile
		List<SimpleFeature> shapefileFeatures = new ArrayList<SimpleFeature>();
		
		// Write the features to output collection
		FeatureIterator<SimpleFeature> inputFeaturesIterator = inputFeatureCollection.features();
		while (inputFeaturesIterator.hasNext()) {
			SimpleFeature inputFeature = inputFeaturesIterator.next();
			
			List<Object> attributeValues = new ArrayList<Object>();
			attributeValues.add(inputFeature.getAttribute("geometry"));
			for (int i = 0; i < inputFeature.getAttributes().size() - 1; i++) {
				attributeValues.add(inputFeature.getAttribute(i));
			}
			
			SimpleFeature outputFeature = SimpleFeatureBuilder.build(TYPE_FOR_SHP, attributeValues, null);
			shapefileFeatures.add(outputFeature);
		}
		inputFeaturesIterator.close();
		
		return new ListFeatureCollection(TYPE_FOR_SHP, shapefileFeatures);
	}
}
