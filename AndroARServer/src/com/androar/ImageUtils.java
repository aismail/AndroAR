package com.androar;

import com.androar.comm.ImageFeaturesProtos.DetectedObject;
import com.androar.comm.ImageFeaturesProtos.GPSPosition;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.ImageContents;
import com.androar.comm.ImageFeaturesProtos.LocalizationFeatures;
import com.google.protobuf.ByteString;

public class ImageUtils {

	public static String computeImageHash(Image image) {
		String image_hash = image.getImage().getImageHash();
		return image_hash;
	}

	public static ByteString getCroppedImageContents(ImageContents image,
			DetectedObject detected_object) {
		//TODO(alex) implement this
		return image.getImageContents();
	}

	public static GPSPosition inferGPSPosition(
			LocalizationFeatures localization_features,
			DetectedObject detected_object) {
		//TODO(alex) implement this
		return localization_features.getGpsPosition();
	}
}
