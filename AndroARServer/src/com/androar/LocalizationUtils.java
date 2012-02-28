package com.androar;

import com.androar.comm.ImageFeaturesProtos.GPSPosition;

public class LocalizationUtils {

	private static final double kEarthRadius = 6371; 
	
	/*
	 * Computes the distance between two points on the globe, using the harvesine distance.
	 * @param first GPS position (latitude and longitude) of the first point
	 * @param second GPS position (latitude and longitude) of the second point
	 * @returns distance between the two points, in kilometers
	 */
	public static double getDistance(GPSPosition first, GPSPosition second) {
		double distance = 0;
		
		double latitude_distance_in_radians = 
				Math.toRadians(second.getLatitude() - first.getLatitude());
		double longitude_distance_in_radians = 
				Math.toRadians(second.getLongitude() - first.getLongitude());
		// harvesin(d / R) = harvesin(lat1 - lat2) + cos(lat1) * cos(lat2) * harvesin(lng1 - lng2);
		// harvesin(angle) = sin(angle / 2) ^ 2;
		// d = 2 * R * arcsin(sqrt(harvesin));
		double harvesin_diff_lat = Math.pow(Math.sin(latitude_distance_in_radians / 2), 2);
		double harvesin_diff_lng = Math.pow(Math.sin(longitude_distance_in_radians / 2), 2);
		double cos_lat1 = Math.cos(Math.toRadians(first.getLatitude()));
		double cos_lat2 = Math.cos(Math.toRadians(second.getLatitude()));
		double harvesin = harvesin_diff_lat + cos_lat1 * cos_lat2 * harvesin_diff_lng;

		distance = 2 * kEarthRadius * Math.atan2(Math.sqrt(harvesin), Math.sqrt(1 - harvesin));
		return distance;
	}
}
