package com.androar;

public final class Logging {
	
	private static int log_level = 2;
	
	/*
	 * Sets the log level. All the logging with log levels higher than this will not be shown.
	 * 
	 */
	public static synchronized void setLOGLevel(int logLevel) {
		if (logLevel < 0) {
			log_level = 0;
		} else {
			log_level = logLevel;
		}
	}
	
	/*
	 * Returns the current logging level.
	 */
	public static int getLOGLevel() {
		return log_level;
	}
	
	/*
	 * Logs a message to stdout.
	 * @param level logging level
	 * @param message message that will be logged
	 */
	public static synchronized void LOG(int level, String message) {
		if (level <= log_level) {
			System.out.println(message);
		}
	}
}
