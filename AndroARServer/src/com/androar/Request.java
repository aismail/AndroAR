package com.androar;

import java.util.ArrayList;
import java.util.List;

import com.androar.comm.CommunicationProtos.OpenCVRequest;
import com.androar.comm.CommunicationProtos.OpenCVRequest.RequestType;
import com.androar.comm.ImageFeaturesProtos.Image;

public class Request {
	
	private OpenCVRequest content;
	
	public Request(RequestType type, Image image_content) {
		this.content = OpenCVRequest.newBuilder().
				setRequestType(type).setImageContents(image_content).build();
	}
	
	public Image getImage() {
		return content.getImageContents();
	}

	public byte[] getRequestToByteArray() {
		// We will send the request content
		return content.toByteArray();
	}

	public List<String> callCallback(Object object) {
		RequestType request_type = content.getRequestType(); 
		if (request_type == RequestType.STORE) {
			// In case this is a store request, we will receive null and we won't do anything
			return null;
		} else if (request_type == RequestType.QUERY) {
			// In case this is a query request, we will have to pick the most probable objects that
			// appear in this image
			byte[] probabilities = (byte[]) object;
			List<String> ret = new ArrayList<String>();
			return ret;
		}
		return null;
	}

}
