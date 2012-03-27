package com.androar;

import java.util.ArrayList;
import java.util.List;

import com.androar.comm.ImageFeaturesProtos.Image;

public class Request {
	
	public static enum RequestType {
		STORE_REQUEST,
		QUERY_REQUEST
	}

	private Image image;
	private RequestType type;
	
	public Request(RequestType type, Image request_contents) {
		this.type = type;
		this.image = request_contents;
	}
	
	public Image getImage() {
		return image;
	}

	public byte[] getRequestToByteArray() {
		// We will send the image, assuming it contains the detected objects
		return image.toByteArray();
	}

	public List<String> callCallback(Object object) {
		
		if (this.type == RequestType.STORE_REQUEST) {
			// In case this is a store request, we will receive null and we won't do anything
			return null;
		} else if (this.type == RequestType.QUERY_REQUEST) {
			// In case this is a query request, we will have to pick the most probable objects that
			// appear in this image
			byte[] probabilities = (byte[]) object;
			List<String> ret = new ArrayList<String>();
			return ret;
		}
		return null;
	}

}
