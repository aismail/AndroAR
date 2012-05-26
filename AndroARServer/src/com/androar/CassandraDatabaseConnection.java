package com.androar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.prettyprint.cassandra.model.BasicColumnDefinition;
import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.TypeInferringSerializer;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.beans.SuperRow;
import me.prettyprint.hector.api.beans.SuperRows;
import me.prettyprint.hector.api.beans.SuperSlice;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.MultigetSuperSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import me.prettyprint.hector.api.query.SubColumnQuery;
import me.prettyprint.hector.api.query.SuperColumnQuery;
import me.prettyprint.hector.api.query.SuperSliceQuery;

import com.androar.caching.ObjectFeatureCache;
import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.GPSPosition;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;
import com.androar.comm.ImageFeaturesProtos.MultipleOpenCVFeatures;
import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;
import com.androar.comm.ImageFeaturesProtos.OpenCVFeatures;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class CassandraDatabaseConnection implements IDatabaseConnection {

	private static StringSerializer string_serializer = StringSerializer.get();
	private static BytesArraySerializer bytearray_serializer = BytesArraySerializer.get();
	private static IntegerSerializer integer_serializer = IntegerSerializer.get();
	private static LongSerializer long_serializer = LongSerializer.get();
	
	private Cluster cluster;
	private Keyspace keyspace_operator;
	private String keyspace_name;
	
	// Features cache
	ObjectFeatureCache features_cache;
	
	public CassandraDatabaseConnection(String hostname, int port) {
		this(hostname, port, Constants.CASSANDRA_KEYSPACE);
	}
	
	public CassandraDatabaseConnection(String hostname, int port, String keyspace_name) {
		this.keyspace_name = keyspace_name;
		// Connect to a cluster
		// TODO(alex, andrei * 2): check if getOrCreateCluster is thread-safe
		cluster = HFactory.getOrCreateCluster(
				Constants.CASSANDRA_CLUSTER_NAME, hostname + ":" + Integer.toString(port));
		if (cluster.describeKeyspace(keyspace_name) == null) {
			createSchema(cluster, keyspace_name);
		}
		keyspace_operator = HFactory.createKeyspace(keyspace_name, cluster);
		// Caching
		features_cache = ObjectFeatureCache.getInstance();
	}

	public void deleteTables() {
		cluster.dropKeyspace(keyspace_name);
	}
	
	public void closeConnection() {
		Logging.LOG(3, "Closing Cassandra connection");
		//cluster.getConnectionManager().shutdown();
	}
	
	private static void createSchema(Cluster cluster, String keyspace_name) {
		ColumnFamilyDefinition image_features_column_family_definition =
				HFactory.createColumnFamilyDefinition(keyspace_name,
						Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY);
		image_features_column_family_definition.setColumnType(ColumnType.STANDARD);
		ColumnFamilyDefinition object_associations_column_family_definition =
				HFactory.createColumnFamilyDefinition(keyspace_name,
						Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY);
		object_associations_column_family_definition.setColumnType(ColumnType.SUPER);
		List<ColumnFamilyDefinition> all_column_families = new ArrayList<ColumnFamilyDefinition>();
		all_column_families.add(image_features_column_family_definition);
		all_column_families.add(object_associations_column_family_definition);
		
		KeyspaceDefinition new_keyspace = HFactory.createKeyspaceDefinition(
				keyspace_name,
				ThriftKsDef.DEF_STRATEGY_CLASS,
				Constants.CASSANDRA_REPLICATION_FACTOR,
				all_column_families);
		// Add the schema to the cluster.
		Logging.LOG(3, "Creating Cassandra keyspace " + keyspace_name);
		cluster.addKeyspace(new_keyspace, true);
		// Edit the column family to add secondary indexes
		KeyspaceDefinition from_cluster = cluster.describeKeyspace(keyspace_name);
		BasicColumnFamilyDefinition column_family_definition =
				new BasicColumnFamilyDefinition(from_cluster.getCfDefs().get(0));
		// Add the 2 secondary indexes: gps_latitude and gps_longitude
		BasicColumnDefinition gps_latitude_column = new BasicColumnDefinition();
		gps_latitude_column.setName(string_serializer.toByteBuffer("gps_latitude"));
		gps_latitude_column.setValidationClass(ComparatorType.LONGTYPE.getClassName());
		gps_latitude_column.setIndexName("gps_latitude_idx");
		gps_latitude_column.setIndexType(ColumnIndexType.KEYS);
		column_family_definition.addColumnDefinition(gps_latitude_column);
		BasicColumnDefinition gps_longitude_column = new BasicColumnDefinition();
		gps_longitude_column.setName(string_serializer.toByteBuffer("gps_longitude"));
		gps_longitude_column.setValidationClass(ComparatorType.LONGTYPE.getClassName());
		gps_longitude_column.setIndexName("gps_longitude_idx");
		gps_longitude_column.setIndexType(ColumnIndexType.KEYS);
		column_family_definition.addColumnDefinition(gps_longitude_column);
		BasicColumnDefinition dumb = new BasicColumnDefinition();
		dumb.setName(string_serializer.toByteBuffer("dumb"));
		dumb.setValidationClass(ComparatorType.LONGTYPE.getClassName());
		dumb.setIndexName("dumb_idx");
		dumb.setIndexType(ColumnIndexType.KEYS);
		column_family_definition.addColumnDefinition(dumb);
		cluster.updateColumnFamily(column_family_definition);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.androar.IDatabaseConnection#storeImage(com.androar.comm.ImageFeaturesProtos.Image)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean storeImage(Image image) {
		Mutator<String> mutator = HFactory.createMutator(keyspace_operator, string_serializer);
		// IMAGE_FEATURES column family
		// * image contents (Column)
		// * localization features (toString() of protocol buffer) (Column)
		//   - GPS latitude
		//   - GPS longitude
		//   - compass orientation 
		// * number of detected objects
		// * all detected objects' ids, in columns "objectX" (Column)
		// The row key is the image hash / id.
		String image_hash = ImageUtils.computeImageHash(image);
		Logging.LOG(10, "Image Hash is: " + image_hash);
		// Detected Objects
		for (int object = 0; object < image.getDetectedObjectsCount(); ++object) {
			DetectedObject detected_object = image.getDetectedObjects(object);
			Logging.LOG(10, "Storing detected object: " + detected_object.getId());
			mutator.insert(image_hash,
					Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
					HFactory.createStringColumn("object" + object, detected_object.getId()));
		}
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY, 
				HFactory.createColumn("num_objects", image.getDetectedObjectsCount(),
						string_serializer, integer_serializer));
		// Image Contents
		Logging.LOG(10, 
				"Storing image contents: " + image.getImage().getImageContents().toByteArray());
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createColumn("image_contents",
						image.getImage().getImageContents().toByteArray(),
						string_serializer, bytearray_serializer));
		// Localization Features
		Logging.LOG(10, 
				"Storing image features: " + image.getLocalizationFeatures().toString());
		float latitude = 0;
		float longitude = 0;
		if (image.getLocalizationFeatures().hasGpsPosition()) {
			GPSPosition gps_position = image.getLocalizationFeatures().getGpsPosition();
			latitude = gps_position.getLatitude();
			longitude = gps_position.getLongitude();
		}
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createColumn("gps_latitude", 
						(long) (latitude * Constants.CASSANDRA_GPS_POSITION_TOLERANCE), 
						string_serializer, long_serializer));
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createColumn("gps_longitude", 
						(long) (longitude * Constants.CASSANDRA_GPS_POSITION_TOLERANCE),
						string_serializer, long_serializer));
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createStringColumn("localization",
						image.getLocalizationFeatures().toString()));
		// TODO(alex): we can't parse from string, only from byte array
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY,
				HFactory.createColumn("dumb", 0L, string_serializer, long_serializer));
		// OBJECTS_TO_IMAGE_ASSOCIATIONS super column family
		// Since we can't put both standard columns (such as name, description) and super columns
		// (such as images that contain the objects (with gps position, cropped image, etc.), we're
		// using a hack: we're using the "metadata" row key to insert meta data, the "image_index"
		// row key to store the number of images and "imageX" row key to insert images
		// * metadata (SuperColumn)
		//   - object name (Column)
		//   - object description (Column)
		// * image_index (SuperColumn)
		//   - first available id for imageX (Column)
		// * images containing object, in key imageX (Super Column)
		//   - image hash / id (Column)
		//   - cropped image contents (Column)
		//   - distance to viewer (Column)
		//   - angle to viewer (Column)
		//   - inferred GPS position (Column)
		// The row key is the object hash / id / name.
		for (int object = 0; object < image.getDetectedObjectsCount(); ++object) {
			DetectedObject detected_object = image.getDetectedObjects(object);
			String object_id = detected_object.getId();
			int first_unused_image_id;
			boolean object_exists = detectedObjectExists(object_id);
			if (!object_exists) {
				// If the object doesn't exist, then we should also add the medatada supercolumn
				first_unused_image_id = 0;
				ArrayList<HColumn<String, String>> metadata_values =
						new ArrayList<HColumn<String,String>>();
				if (detected_object.hasMetadata()) {
					ObjectMetadata metadata = detected_object.getMetadata();
					String name = (metadata.hasName()) ? metadata.getName() : detected_object.getId();
					metadata_values.add(HFactory.createStringColumn("name", name));
					if (metadata.hasDescription()) {
						metadata_values.add(HFactory.createStringColumn(
								"description", metadata.getDescription()));
					}
				} else {
					metadata_values.add(
							HFactory.createStringColumn("name", detected_object.getId()));
				}
				mutator.insert(object_id,
						Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
						HFactory.createSuperColumn(
								"metadata", metadata_values, string_serializer, string_serializer,
								string_serializer));
			} else {
				first_unused_image_id = 
						getDetectedObjectFirstAvailableImageId(object_id);
				
			}
			// Add the image super column
			// TODO(alex, andrei): add support for using multiple serializers for column values
			ArrayList image_values = new ArrayList();
			// Add image_hash = String;
			image_values.add(HFactory.createStringColumn("image_hash", image_hash));
			// Add image_contents = ByteArray;
			image_values.add(HFactory.createColumn("image_contents",
					image.getImage().getImageContents().toByteArray(), string_serializer,
					bytearray_serializer));
			// Add cropped_image_contents = ByteArray;
			ByteString cropped_image = null;
			if (detected_object.hasCroppedImage()) {
				cropped_image = detected_object.getCroppedImage();
			} else {
				cropped_image = 
						ImageUtils.getCroppedImageContents(image.getImage(), detected_object);
			}
			image_values.add(HFactory.createColumn("cropped_image_contents",
					cropped_image.toByteArray(), string_serializer, bytearray_serializer));
			// Add distance_to_viewer = Integer;
			// TODO(alex) see if we need this and if not, remove it
			if (detected_object.hasDistanceToViewer()) {
				image_values.add(HFactory.createColumn("distance_to_viewer",
						detected_object.getDistanceToViewer(),
						string_serializer, integer_serializer));
			}
			// Add angle = Integer;
			// TODO(alex) see if we need this and if not, remove it
			if (detected_object.hasAngleToViewer()) {
				image_values.add(HFactory.createColumn("angle", detected_object.getAngleToViewer(),
						string_serializer, integer_serializer));
			}
			// Add inferred_gps_position = String;
			image_values.add(HFactory.createStringColumn("inferred_gps_position",
					ImageUtils.inferGPSPosition(image.getLocalizationFeatures(), 
							detected_object).toString()));
			mutator.insert(object_id,
					Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
					HFactory.createSuperColumn(
							"image" + first_unused_image_id, image_values, string_serializer, 
							string_serializer, TypeInferringSerializer.get()));
			Logging.LOG(10, "Storing image <" + image_hash + "> under key: " + object_id +
					", super column: " + "image" + first_unused_image_id);
			// We change the value of the first available image id column
			ArrayList<HColumn<String, Integer>> image_index = 
					new ArrayList<HColumn<String,Integer>>();
			image_index.add(HFactory.createColumn("first_available_image_id",
					first_unused_image_id + 1, string_serializer, integer_serializer));
			mutator.insert(object_id,
					Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY,
					HFactory.createSuperColumn("image_index", image_index, 
							string_serializer, string_serializer, integer_serializer));
		}
		Logging.LOG(10, "Done storing image");
		return true;
	}
	
	@Override
	public Map<String, ObjectMetadata> getObjectsMetadata(List<String> object_ids) {
		// The ObjectMetadata object contains:
		//  * name
		//  * description
		Map<String, ObjectMetadata> ret = new HashMap<String, ObjectMetadata>();

		MultigetSuperSliceQuery<String, String, String, String> multiget_query =
				HFactory.createMultigetSuperSliceQuery(keyspace_operator, string_serializer,
						string_serializer, string_serializer, string_serializer);
		multiget_query.setColumnFamily(
				Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY);
		multiget_query.setKeys(object_ids);
		multiget_query.setRange(null, null, false, 1000);
		multiget_query.setColumnNames("metadata");
		QueryResult<SuperRows<String, String, String, String>> result = multiget_query.execute();
        SuperRows<String, String, String, String> ordered_rows = result.get();
        
        for (SuperRow<String, String, String, String> row : ordered_rows) {
        	String key = row.getKey();
        	HSuperColumn<String, String, String> super_column = 
        			row.getSuperSlice().getColumnByName("metadata");
        	if (super_column == null) {
        		continue;
        	}
        	HColumn<String, String> name_column = super_column.getSubColumnByName("name");
        	HColumn<String, String> description_column = 
        			super_column.getSubColumnByName("description");
        	ObjectMetadata.Builder builder = ObjectMetadata.newBuilder();
        	if (name_column != null) {
    			builder.setName(name_column.getValue());
    		} else {
    			builder.setName(key);
    		}
    		if (description_column != null) {
    			builder.setDescription(description_column.getValue());
    		}
    		ret.put(key, builder.build());
        	
        }
		return ret;
	}
	
	@Override
	public ObjectMetadata getObjectMetadata(String object_id) {
		// The ObjectMetadata object contains:
		//  * name
		//  * description
		ObjectMetadata.Builder builder = ObjectMetadata.newBuilder();
		SuperColumnQuery<String, String, String, String> super_column_query =
				HFactory.createSuperColumnQuery(keyspace_operator, string_serializer,
						string_serializer, string_serializer, string_serializer);
		super_column_query
				.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
				.setKey(object_id)
				.setSuperName("metadata");
		HSuperColumn<String, String, String> result = super_column_query.execute().get();
		if (result == null) {
			return null;
		}
		HColumn<String, String> name_column = result.getSubColumnByName("name");
		if (name_column != null) {
			builder.setName(name_column.getValue());
		} else {
			builder.setName(object_id);
		}
		HColumn<String, String> description_column = result.getSubColumnByName("description");
		if (description_column != null) {
			builder.setDescription(description_column.getValue());
		}
		
		return builder.build();
	}
	
	@Override
	public List<ImageWithObject> getAllImagesContainingObject(String object_id) {
		// We need to return only the big image and the small, cropped image, since we don't care
		// about the GPS position of the image or the inferred gps position of the object
		List<ImageWithObject> all_image_contents = new ArrayList<ImageWithObject>();
		SuperSliceQuery<String, String, String, byte[]> query = HFactory.createSuperSliceQuery(
				keyspace_operator, string_serializer, string_serializer, string_serializer,
				bytearray_serializer);
		query.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
				.setKey(object_id)
				.setRange("image0", "imageX", false, 1000);
		SuperSlice<String, String, byte[]> result_columns = query.execute().get();
		List<HSuperColumn<String, String, byte[]>> all_super_columns =
				result_columns.getSuperColumns();
		for (HSuperColumn<String, String, byte[]> super_column : all_super_columns) {
			if (super_column.getName().contains("image")) {
				HColumn<String, byte[]> image_contents_column = 
						super_column.getSubColumnByName("image_contents");
				ByteString image_contents_bytes =
						ByteString.copyFrom(image_contents_column.getValue());
				HColumn<String, byte[]> cropped_image_contents_column =
						super_column.getSubColumnByName("cropped_image_contents");
				byte[] cropped_image_contents = cropped_image_contents_column.getValue();
				String image_hash = 
						new String(super_column.getSubColumnByName("image_hash").getValue());
				ImageContents image_contents = ImageContents.newBuilder().
						setImageContents(image_contents_bytes).
						setImageHash(image_hash).
						build();
				all_image_contents.add(new ImageWithObject(image_contents, cropped_image_contents));
			}
		}
		return all_image_contents;
	}
	
	private List<String> getRowKeysForImagesInRange(LocalizationFeatures position, double range) {
		List<String> row_keys = new ArrayList<String>();
		long big_range = (long) (range * Constants.CASSANDRA_GPS_POSITION_TOLERANCE);
		long latitude = 0;
		long longitude = 0;
		if (!position.hasGpsPosition()) {
			return row_keys;
		}
		latitude = (long) (position.getGpsPosition().getLatitude() * 
				Constants.CASSANDRA_GPS_POSITION_TOLERANCE);
		longitude = (long) (position.getGpsPosition().getLongitude() *
				Constants.CASSANDRA_GPS_POSITION_TOLERANCE);
		IndexedSlicesQuery<String, String, Long> indexed_query = 
				HFactory.createIndexedSlicesQuery(keyspace_operator, string_serializer,
						string_serializer, long_serializer);
		
		// Get all keys since f****ing Hector can't work with more serializers
		indexed_query.addEqualsExpression("dumb", 0L);
		indexed_query.addLteExpression("gps_latitude", latitude + big_range);
		indexed_query.addGteExpression("gps_latitude", latitude - big_range);
		indexed_query.addLteExpression("gps_longitude", longitude + big_range);
		indexed_query.addGteExpression("gps_longitude", longitude - big_range);
		indexed_query.setColumnFamily(Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY);
		indexed_query.setColumnNames("gps_latitude", "gps_longitude");
		indexed_query.setStartKey("");
		
		QueryResult<OrderedRows<String, String, Long>> indexed_result = indexed_query.execute();
		List<Row<String, String, Long>> all_rows = indexed_result.get().getList();
		for (Row<String, String, Long> row : all_rows) {
			row_keys.add(row.getKey());
		}
		return row_keys;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.androar.IDatabaseConnection#getImagesInRange(com.androar.comm.ImageFeaturesProtos.LocalizationFeatures, double)
	 */
	@Override
	public List<Image> getImagesInRange(LocalizationFeatures position, double range) {
		List<Image> ret = new ArrayList<Image>();
		List<String> row_keys = getRowKeysForImagesInRange(position, range);
		// Multiget ALL the keys!
		MultigetSliceQuery<String, String, byte[]> multiget_query =
				HFactory.createMultigetSliceQuery(keyspace_operator, string_serializer,
						string_serializer, bytearray_serializer);
		multiget_query.setColumnFamily(Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY);
		multiget_query.setKeys(row_keys);
		multiget_query.setRange(null, null, false, 10000);
		QueryResult<Rows<String, String, byte[]>> multiget_result = multiget_query.execute();
        Rows<String, String, byte[]> ordered_rows = multiget_result.get();
        // Construct images
        for (Row<String, String, byte[]> row : ordered_rows) {
        	byte[] image_contents = 
        			row.getColumnSlice().getColumnByName("image_contents").getValue();
        	String image_hash = row.getKey();
        	Image image = Image.newBuilder().setImage(
        			ImageContents.newBuilder().setImageHash(image_hash)
        			.setImageContents(ByteString.copyFrom(image_contents)).build()).build();
        	ret.add(image);
        }
		return ret;
	}
	
	private int getDetectedObjectFirstAvailableImageId(String object_id) {
		SubColumnQuery<String, String, String, Integer> sub_column_query =
				HFactory.createSubColumnQuery(keyspace_operator, string_serializer,
						string_serializer, string_serializer, integer_serializer);
		sub_column_query
				.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
				.setKey(object_id)
				.setSuperColumn("image_index")
				.setColumn("first_available_image_id");
        QueryResult<HColumn<String, Integer>> result = sub_column_query.execute();
        return result.get().getValue();
	}

	private boolean detectedObjectExists(String object_id) {
		SubColumnQuery<String, String, String, Integer> sub_column_query =
				HFactory.createSubColumnQuery(keyspace_operator, string_serializer,
						string_serializer, string_serializer, integer_serializer);
		sub_column_query
				.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
				.setKey(object_id)
				.setSuperColumn("image_index")
				.setColumn("first_available_image_id");
        QueryResult<HColumn<String, Integer>> result = sub_column_query.execute();
        return (result.get() != null);
	}

	@Override
	public boolean storeFeatures(String image_hash, OpenCVFeatures opencv_features) {
		Mutator<String> mutator = HFactory.createMutator(keyspace_operator, string_serializer);
		Logging.LOG(10, "Image Hash is: " + image_hash + ", storing features for original image.");
		// OpenCVFeatures
		mutator.insert(image_hash,
				Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY, 
				HFactory.createColumn("opencv_features", opencv_features.toByteArray(),
						string_serializer, bytearray_serializer));
		return true;
	}

	@Override
	public boolean storeFeatures(String image_hash, MultipleOpenCVFeatures opencv_features) {
		Mutator<String> mutator = HFactory.createMutator(keyspace_operator, string_serializer);
		Logging.LOG(10, "Image Hash is: " + image_hash + ", storing features for original image " +
				"and for cropped images.");
		Map<String, Integer> object_ids_map = new HashMap<String, Integer>();
		// object_ids_map[K] = V where there exists column "objectV" = "K"
		
		SliceQuery<String, String, String> query = HFactory.createSliceQuery(keyspace_operator,
				string_serializer, string_serializer, string_serializer);
		query.setColumnFamily(Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY);
		query.setKey(image_hash);
		query.setRange(null, null, false, 1000);
		QueryResult<ColumnSlice<String, String>> result = query.execute();
		List<HColumn<String, String>> all_columns = result.get().getColumns();
		for (HColumn<String, String> column : all_columns) {
			String column_name = column.getName();
			if (column_name.startsWith("object")) {
				int object_num = Integer.parseInt(column_name.substring(6));
				object_ids_map.put(column.getValue(), object_num);
			}
		}
		
		for (int i = 0; i < opencv_features.getFeaturesCount(); ++i) {
			OpenCVFeatures features = opencv_features.getFeatures(i);
			if (features.hasObjectId()) {
				// If the features have the object_id field set, then these are features for a
				// cropped image.
				String object_id = features.getObjectId();
				if (!object_ids_map.containsKey(object_id)) {
					continue;
				}
				Logging.LOG(10, "Storing features for object " + object_id);
				mutator.insert(image_hash,
						Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY, 
						HFactory.createColumn("cv" + object_ids_map.get(object_id),
								features.toByteArray(),	string_serializer, bytearray_serializer));
			} else {
				// Otherwise, they are the features for the big image
				if (storeFeatures(image_hash, features) == false) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	@Override
	public List<OpenCVFeatures> getFeaturesForObject(String object_id) {
		// Get all the image hashes associated with this object
		List<String> row_keys = new ArrayList<String>();
		SuperSliceQuery<String, String, String, byte[]> query = HFactory.createSuperSliceQuery(
				keyspace_operator, string_serializer, string_serializer, string_serializer,
				bytearray_serializer);
		query.setColumnFamily(Constants.CASSANDRA_OBJECT_TO_IMAGE_ASSOCIATIONS_COLUMN_FAMILY)
				.setKey(object_id)
				.setRange("image0", "imageX", false, 1000);
		SuperSlice<String, String, byte[]> result_columns = query.execute().get();
		List<HSuperColumn<String, String, byte[]>> all_super_columns =
				result_columns.getSuperColumns();
		for (HSuperColumn<String, String, byte[]> super_column : all_super_columns) {
			if (super_column.getName().contains("image")) {
				String image_hash = 
						new String(super_column.getSubColumnByName("image_hash").getValue());
				row_keys.add(image_hash);
			}
		}
		// Get all the "opencv_features" columns for these images
		// Multiget ALL the keys!
		MultigetSliceQuery<String, String, byte[]> multiget_query =
				HFactory.createMultigetSliceQuery(keyspace_operator, string_serializer,
						string_serializer, bytearray_serializer);
		multiget_query.setColumnFamily(Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY);
		multiget_query.setKeys(row_keys);
		multiget_query.setRange(null, null, false, 10000);
		QueryResult<Rows<String, String, byte[]>> multiget_result = multiget_query.execute();
		Rows<String, String, byte[]> ordered_rows = multiget_result.get();
		// Get OpenCVFeatures
		List<OpenCVFeatures> ret = new ArrayList<OpenCVFeatures>();
		for (Row<String, String, byte[]> row : ordered_rows) {
			OpenCVFeatures features;
			try {
				features = OpenCVFeatures.parseFrom(
						row.getColumnSlice().getColumnByName("opencv_features").getValue());
				ret.add(features);
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
				continue;
			}
		}
		return ret;
	}

	@Override
	public Map<String, List<OpenCVFeatures>> getFeaturesForAllObjectsInRange(
			LocalizationFeatures position, double range) {
		Map<String, List<OpenCVFeatures>> ret = new HashMap<String, List<OpenCVFeatures>>();
		List<String> row_keys = getRowKeysForImagesInRange(position, range);
		// We need to get the subset of row keys that aren't in the cache and query only those.
		List<String> uncached_row_keys = new ArrayList<String>();
		for (String key : row_keys) {
			if (features_cache.parsedImageId(key)) {
				Map<String, OpenCVFeatures> containing_objects = features_cache.getContainingObjects(key);
				for (Entry<String, OpenCVFeatures> entry : containing_objects.entrySet()) {
					String object_id = entry.getKey();
					OpenCVFeatures features = entry.getValue();
					List<OpenCVFeatures> value;
					if (ret.containsKey(object_id)) {
						value = ret.get(object_id);
					} else {
						value = new ArrayList<OpenCVFeatures>();
					}
					value.add(features);
					ret.put(object_id, value);
				}
			} else {
				uncached_row_keys.add(key);
			}
		}
		// Multiget the uncached keys
		Logging.LOG(10, "Multigetting only " + uncached_row_keys.size() + " out of " +
				row_keys.size() + " keys. The rest are cached.");
		MultigetSliceQuery<String, String, byte[]> multiget_query =
				HFactory.createMultigetSliceQuery(keyspace_operator, string_serializer,
						string_serializer, bytearray_serializer);
		multiget_query.setColumnFamily(Constants.CASSANDRA_IMAGE_FEATURES_COLUMN_FAMILY);
		multiget_query.setKeys(uncached_row_keys);
		multiget_query.setRange(null, null, false, 10000);
		QueryResult<Rows<String, String, byte[]>> multiget_result = multiget_query.execute();
		Rows<String, String, byte[]> ordered_rows = multiget_result.get();
		// Put opencv features for each image in the keys associated with objects inside the image
		for (Row<String, String, byte[]> row : ordered_rows) {
			String image_id = row.getKey();
			Map<String, OpenCVFeatures> containing_objects = new HashMap<String, OpenCVFeatures>();
			Map<Integer, String> object_ids_map = new HashMap<Integer, String>();
			// object_ids_map[K] = V where there exists column "objectK" = "V"
			List<HColumn<String, byte[]>> all_columns = row.getColumnSlice().getColumns();
			for (HColumn<String, byte[]> column : all_columns) {
				String column_name = column.getName();
				if (column_name.startsWith("object")) {
					int object_num = Integer.parseInt(column_name.substring(6));
					object_ids_map.put(object_num, string_serializer.fromBytes(column.getValue()));
				}
			}
			
			for (HColumn<String, byte[]> column : all_columns) {
				if (column.getName().startsWith("cv")) {
					int object_num = Integer.parseInt(column.getName().substring(2));
					if (!object_ids_map.containsKey(object_num)) {
						continue;
					}
					String object_id = object_ids_map.get(object_num);
					OpenCVFeatures features;
					try {
						features = OpenCVFeatures.parseFrom(column.getValue());
					} catch (InvalidProtocolBufferException e) {
						continue;
					}
					containing_objects.put(object_id, features);
					List<OpenCVFeatures> value;
					if (ret.containsKey(object_id)) {
						value = ret.get(object_id);
					} else {
						value = new ArrayList<OpenCVFeatures>();
					}
					value.add(features);
					ret.put(object_id, value);
				}
			}
			features_cache.addObjectFeaturesToCache(image_id, containing_objects);
		}
		return ret;
	}
}
