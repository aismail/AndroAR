package com.androar;

import java.io.DataOutputStream;

import com.androar.comm.Communication;
import com.androar.comm.CommunicationProtos.OpenCVRequest;
import com.androar.comm.CommunicationProtos.OpenCVRequest.RequestType;
import com.androar.comm.ImageFeaturesProtos.Image;
import com.androar.comm.ImageFeaturesProtos.MultipleOpenCVFeatures;

public class Request {
	
	private OpenCVRequest content;
	private DataOutputStream out;
	private long start_time;
	
	public Request(RequestType type, Image image_content, DataOutputStream out) {
		this.out = out;
		this.content = OpenCVRequest.newBuilder().
				setRequestType(type).setImageContents(image_content).build();
		start_time = System.currentTimeMillis();
	}
	
	public Image getImage() {
		return content.getImageContents();
	}

	public byte[] getRequestToByteArray() {
		// We will send the request content
		return content.toByteArray();
	}

	public void callCallback(byte[] reply) {
		Logging.LOG(0, "Got reply from OpenCV for image " + 
				ImageUtils.computeImageHash(getImage()) + " of size " + reply.length +
				". Reply took " + (System.currentTimeMillis() - start_time));
		RequestType request_type = content.getRequestType(); 
		if (request_type == RequestType.STORE) {
			// In case this is a store request, we will receive the image features and we will have
			// to store them
			try {
				MultipleOpenCVFeatures features = MultipleOpenCVFeatures.parseFrom(reply);
				String image_hash = ImageUtils.computeImageHash(content.getImageContents());
				IDatabaseConnection database_connection =
						DatabaseConnectionPool.getDatabaseConnectionPool().borrowConnection();
				database_connection.storeFeatures(image_hash, features);
				DatabaseConnectionPool.getDatabaseConnectionPool().returnConnection(
						database_connection);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (request_type == RequestType.QUERY) {
			// In case this is a query request, we will get the detected objects
			Communication.sendByteArrayMessage(reply, out);
			//TODO(alex, andrei): Let's see if we should also store it.
		}
		return;
	}

}
