package com.androar;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.OpenCVRequest;
import com.androar.comm.CommunicationProtos.OpenCVRequest.RequestType;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.google.protobuf.InvalidProtocolBufferException;

public class Request {
	
	private OpenCVRequest content;
	private DataOutputStream out;
	
	public Request(RequestType type, Image image_content, DataOutputStream out) {
		this.out = out;
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

	public void callCallback(byte[] reply) {
		RequestType request_type = content.getRequestType(); 
		if (request_type == RequestType.STORE) {
			// In case this is a store request, we will receive null and we won't do anything
			return;
		} else if (request_type == RequestType.QUERY) {
			// In case this is a query request, we will get the detected objects
			Communication.sendByteArrayMessage(reply, out);
			return;
		}
		return;
	}

}
