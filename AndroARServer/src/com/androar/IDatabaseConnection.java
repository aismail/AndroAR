/**
 * Encapsulation of the connection to the database
 */
package com.androar;

import java.util.List;

import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;
import com.androar.comm.ImageFeaturesProtos.ObjectMetadata;

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
}
