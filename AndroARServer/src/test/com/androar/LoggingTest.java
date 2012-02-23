package test.com.androar;

import org.junit.Assert;
import org.junit.Test;

import com.androar.Logging;

public class LoggingTest {
	
	@Test
	public void testSetAndGetLOGLevel() {
		Logging.setLOGLevel(100);
		Assert.assertEquals(100, Logging.getLOGLevel());
	}

	@Test
	public void testSetAndGetLOGLevelIllegalValues() {
		Logging.setLOGLevel(-1);
		Assert.assertEquals(0, Logging.getLOGLevel());
	}
}
