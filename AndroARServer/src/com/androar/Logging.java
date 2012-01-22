package com.androar;

public final class Logging {
	
	private static int log_level = 2;
	
	public static synchronized void setLOGLevel(int i) {
		if (i < 0) {
			log_level = 0;
		} else {
			log_level = i;
		}
	}
	
	public static int getLOGLevel() {
		return log_level;
	}
	
	public static synchronized void LOG(int level, String message) {
		if (level <= log_level) {
			System.out.println(message);
		}
	}
}
