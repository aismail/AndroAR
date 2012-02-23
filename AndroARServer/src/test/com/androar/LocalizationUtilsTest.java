package test.com.androar;

import junit.framework.Assert;

import org.junit.Test;

import com.androar.LocalizationUtils;
import com.androar.comm.ImageFeaturesProtos.GPSPosition;

public class LocalizationUtilsTest {

	@Test
	public void testGetDistanceBetweenSamePoints() {
		GPSPosition point = GPSPosition.newBuilder()
				.setLatitude((float) 1.23)
				.setLongitude((float) 12.3)
				.build();
		Assert.assertEquals(0, LocalizationUtils.getDistance(point, point), 0.001);
	}
	
	@Test
	public void testGetDistanceDifferentPoints() {
		//1.13938e+6 m
		GPSPosition first = GPSPosition.newBuilder()
				.setLatitude((float) 44.43620)
				.setLongitude((float) 26.1027)
				.build(); // Bucharest, Metrou Universitate
		GPSPosition second = GPSPosition.newBuilder()
				.setLatitude((float) 41.89016)
				.setLongitude((float) 12.49227)
				.build(); // Rome, Colloseum 
		Assert.assertEquals(1138, LocalizationUtils.getDistance(first, second), 1138 * 0.003);
		// This is using a spherical model, so we have errors of maximum .3% (since the Earth is
		// elliptical
	}

}
