package com.androar;

import com.androar.comm.ImageFeaturesProtos.Image;

public class ImageUtils {

	public static String computeImageHash(Image image) {
		String image_hash = image.getImage().getImageHash();
		return image_hash;
	}
}
