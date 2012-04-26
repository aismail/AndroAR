package com.androar;

import java.io.DataOutputStream;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.OpenCVRequest;
import com.androar.comm.CommunicationProtos.OpenCVRequest.RequestType;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.OpenCVFeatures;
import com.google.protobuf.InvalidProtocolBufferException;

public class Request {
	
	private OpenCVRequest content;
	private DataOutputStream out;
	private IDatabaseConnection database_connection;
	
	public Request(RequestType type, Image image_content, DataOutputStream out,
			IDatabaseConnection database_connection) {
		this.out = out;
		this.database_connection = database_connection;
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
			// In case this is a store request, we will receive the image features and we will have
			// to store them
			try {
				OpenCVFeatures features = OpenCVFeatures.parseFrom(reply);
				String image_hash = ImageUtils.computeImageHash(content.getImageContents());
				database_connection.storeFeatures(image_hash, features);
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
			return;
		} else if (request_type == RequestType.QUERY) {
			// In case this is a query request, we will get the detected objects
			Communication.sendByteArrayMessage(reply, out);
			//TODO(alex, andrei): Let's see if we should also store it.
			return;
		}
		return;
	}

}
