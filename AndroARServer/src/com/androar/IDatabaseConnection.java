/**
 * Encapsulation of the connection to the database
 */
package com.androar;

import java.util.List;
import java.util.Map;

import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;
import com.androar.comm.ImageFeaturesProtos.MultipleOpenCVFeatures;
import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;
import com.androar.comm.ImageFeaturesProtos.OpenCVFeatures;

/**
 * @author Alexandru Damian (alex.m.damian@gmail.com)
 *
 */
public interface IDatabaseConnection {

	/*
	 * Attempts to store an image in the database, along with its metadata. This will store the
	 * image along with all the objects associated with it. It will also add the image's metadata
	 * to each object row.
	 * @param image Image to be stored
	 * @returns true if the operation was successful
	 */
	public boolean storeImage(Image image);
	/*
	 * Fetches all images that are near to the current position
	 * @param position the position and direction the current user is in
	 * @param range the range to search in (in kilometers)
	 * @returns a list of Image objects that are in the current range
	 */
	public List<Image> getImagesInRange(LocalizationFeatures position, double range);

	/*
	 * Fetches the medata for a list of objects
	 * @param object_ids a list with the objects' ids
	 * @returns a mapping between object ids and their metadata
	 */
	public Map<String, ObjectMetadata> getObjectsMetadata(List<String> object_ids);
	/*
	 * Fetches an object's metadata
	 * @param object_id the object's id
	 * @returns the object's metadata
	 */
	public ObjectMetadata getObjectMetadata(String object_id);
	
	/*
	 * Fetches all images that contain an object_id
	 * @param object_id the object's id
	 * @returns a list of images that contain the object.
	 */
	public List<ImageWithObject> getAllImagesContainingObject(String object_id);
	
	/*
	 * Stores the opencv features of the image to cassandra. Features are:
	 *  o vector of keypoints
	 *  o descriptor map
	 * @param image_hash the row key (image hash)
	 * @param opencv_features the features that should be stored
	 * @returns true if the operation succeeded, false otherwise 
	 */
	public boolean storeFeatures(String image_hash, OpenCVFeatures opencv_features);
	public boolean storeFeatures(String image_hash, MultipleOpenCVFeatures opencv_features);
	
	/*
	 * Fetches the features for all the images that contain the specified object.
	 * @param object_id the objects' id
	 * @returns a list of OpenCVFeatures protocol buffers.
	 */
	public List<OpenCVFeatures> getFeaturesForObject(String object_id);
	
	/*
	 * Fetches all the features for the objects in an area surrounding this position.
	 * @param position a LocalizationFeatures protocol buffer containing information about the current position
	 * @param range the rage to search in
	 */
	public Map<String, List<OpenCVFeatures>> getFeaturesForAllObjectsInRange(LocalizationFeatures position,
			double range);
}
