package com.androar.caching;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.androar.comm.ImageFeaturesProtos.OpenCVFeatures;

public class ObjectFeatureCache {

	/*
	 * The getFeaturesForAllObjectsInRange works as follows:
	 * 1. get all the images in range
	 * 2. get all the rows for those images and the objects + features associated with them
	 * 3. merge them into a map<object_id, multiple_opencv_features>
	 * 
	 * We will try to improve this by using the following observation:
	 * o we don't need to query the existing images again, we only need to query new images
	 * 
	 * We need to store something like:
	 * object_id -> map of (features, image_id)
	 * image_id -> set of object_ids
	 * 
	 * Searching for the features for an object will be:
	 * o image_id -> object_ids -> features
	 * 
	 * Adding a new image to cache is simple.
	 * Removing from cache:
	 * o removing an image
	 * o removing an object feature
	 */
	private static ObjectFeatureCache cache = null;
	private Map<String, Set<String>> images = null; 
	// images[image_id] = list of (object_id);
	// This will act as a reverse index, to keep track of images that we checked so far
	private Map<String, Map<String, OpenCVFeatures>> objects = null;
	// objects[object_id] = map of (image_id -> features);
	// This will act as a main index
	
	
	private ObjectFeatureCache() {
		images = new HashMap<String, Set<String>>();
		objects = new HashMap<String, Map<String,OpenCVFeatures>>();
	}
	
	public static ObjectFeatureCache getInstance() {
		if (cache == null) {
			cache = new ObjectFeatureCache();
		}
		return cache;
	}
	
	public void addObjectFeaturesToCache(String image_id,
			Map<String, OpenCVFeatures> detected_objects) {
		flushOut(image_id);
		
		// Populate the objects index
		for (Entry<String, OpenCVFeatures> entry : detected_objects.entrySet()) {
			String object_id = entry.getKey();
			OpenCVFeatures features = entry.getValue();
			if (!objects.containsKey(object_id)) {
				objects.put(object_id, new HashMap<String, OpenCVFeatures>());
			}
			Map<String, OpenCVFeatures> features_map = objects.get(object_id);
			features_map.put(image_id, features);
			objects.put(object_id, features_map);
		}
		// Populate the reverse index
		images.put(image_id, detected_objects.keySet());
	}
	
	private void flushOut(String image_id) {
		if (images.containsKey(image_id) == false) {
			return;
		}
		for (String object_id : images.get(image_id)) {
			// Remove the features associated with the image_id from the object_id
			if (objects.containsKey(object_id) == false) {
				continue;
			}
			Map<String, OpenCVFeatures> features_map = objects.get(object_id);
			features_map.remove(image_id);
			if (features_map.entrySet().size() == 0) {
				objects.remove(object_id);
			} else {
				objects.put(object_id, features_map);
			}
		}
		images.remove(image_id);
	}

	public boolean parsedImageId(String image_id) {
		return images.containsKey(image_id);
	}

	public Map<String, OpenCVFeatures> getContainingObjects(String image_id) {
		Map<String, OpenCVFeatures> ret = new HashMap<String, OpenCVFeatures>();
		for (String object_id : images.get(image_id)) {
			 Map<String, OpenCVFeatures> all_features = objects.get(object_id);
			if (all_features == null) {
				continue;
			}
			if (all_features.containsKey(image_id)) {
				ret.put(object_id, all_features.get(image_id));
			}
		}
		return ret;
	}

	public static void clearCache() {
		cache.images.clear();
		cache.objects.clear();
	}

}
