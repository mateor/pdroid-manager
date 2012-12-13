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
	
	public static final String WRITE_SETTINGS_PERMISSION = "android.privacy.WRITE_PRIVACY_SETTINGS";
	public static final String READ_SETTINGS_PERMISSION = "android.privacy.READ_PRIVACY_SETTINGS";
	
	public static final String CORE_SUPPORTS_EXTENSION_FUNCTION = "supportsExtension"; //used to check, using reflection, if the 'extension support' checking function is available
	public static final String CORE_EXTENSION_BATCH = "batch"; //name of the 'batch' support extension
	private GlobalConstants() {}

}
