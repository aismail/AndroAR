/**
 * Encapsulation of the connection to the database
 */
package com.androar;

import java.util.List;

import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;

/**
 * @author Alexandru Damian (alex.m.damian@gmail.com)
 *
 */
public interface IDatabaseConnection {

	/*
	 * Attempts to store an image in the database, along with its metadata.
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
	
}
