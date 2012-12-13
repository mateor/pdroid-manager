package net.digitalfeed.pdroidalternative;

public class GlobalConstants {

	public static final boolean LOG_DEBUG = true;
	public static final boolean LOG_FUNCTION_TRACE = false;
	public static final String LOG_TAG = "PDroidAlternative";
	public static final double MINIMUM_PDROID_VERSION = 1.48;
	public static final String PDROIDMANAGER_XDA_THREAD_URL = "http://forum.xda-developers.com/showthread.php?p=34190204";
	
	// If crash-happy is true, exceptions will be preferred over logging of errors.
	// this is basically to make problems easier to see when testing. It should NEVER be true in releases.
	public static final boolean CRASH_HAPPY = false; 
	private GlobalConstants() {}

}
