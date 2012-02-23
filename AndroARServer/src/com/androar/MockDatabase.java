/**
 * Mock implementation of the Database Interface
 */
package com.androar;

import java.util.ArrayList;
import java.util.List;

import com.androar.comm.ImageFeaturesProtos.GPSPosition;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;

/**
 * @author Alexandru Damian (alex.m.damian@gmail.com)
 *
 */
public class MockDatabase implements IDatabaseConnection {

	private List<Image> all_images;
	
	public MockDatabase(String hostname, int port) {
		all_images = new ArrayList<Image>();
	}
	
	@Override
	public boolean storeImage(Image image) {
		all_images.add(image);
		return true;
	}

	@Override
	public List<Image> getImagesInRange(LocalizationFeatures position, double range) {
		List<Image> good_images = new ArrayList<Image>();
		// Return all images if we don't know our GPS position.
		if (position.hasGpsPosition() == false) {
			return all_images;
		}
		for (int image_index = 0; image_index < all_images.size(); ++image_index) {
			Image current_image = all_images.get(image_index);
			if (current_image.hasLocalizationFeatures()) {
				LocalizationFeatures localization_features = 
						current_image.getLocalizationFeatures();
				GPSPosition current_position = null;
				if (localization_features.hasGpsPosition()) {
					current_position = localization_features.getGpsPosition();
				}
				// Return all images that don't have a GPS position (they might have originated from
				// a phone w/o GPS) and all the images that have a GPS position and are in range
				if (current_position == null) {
					good_images.add(current_image);
				} else if (LocalizationUtils.
						getDistance(current_position, position.getGpsPosition()) < range) {
					good_images.add(current_image);
				}
			}
		}
		return good_images;
	}

}
