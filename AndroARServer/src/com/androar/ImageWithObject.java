package com.androar;

import com.androar.comm.ImageFeaturesProtos.ImageContents;

public class ImageWithObject {

	public ImageContents big_image;
	public byte[] cropped_image;
	
	public ImageWithObject(ImageContents big_image, byte[] cropped_image) {
		this.big_image = big_image;
		this.cropped_image = cropped_image;
	}
}
