package net.digitalfeed.pdroidalternative;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.privacy.PrivacySettingsManager;
import android.util.Log;

/**
 * Helper class to provide PDroid-related checks
 * @author smorgan
 *
 */
class PDroidInterfaceHelper {
	private static final String PDROID_WRITE_PERMISSION = "android.privacy.WRITE_PRIVACY_SETTINGS";
	
	private PDroidInterfaceHelper() {
		throw new RuntimeException("The PDroidInterfaceHelper class cannot be instantiated");
	}
	
	/**
	 * Check if the current PDroid core supports the PAC functions: getIsAuthorizedManager, authorizeManager, and deauthorizeManager
	 * Only getIsAuthorizedManager is actually checked
	 * @return
	 */
	public static boolean supportsPAC(Context context) throws PDroidNotPresentException {
		if (!isPDroidInstalled(context)) {
			throw new PDroidNotPresentException();
		}

		try {
			PrivacySettingsManager.class.getMethod("getIsAuthorizedManager", String.class);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
	
	/**
	 * Check if the current app has the privileges to write settings to the PDroid core
	 * @param context
	 * @return
	 */
	public static boolean isAuthorised(Context context) throws PDroidNotPresentException {
		if (!isPDroidInstalled(context)) {
			throw new PDroidNotPresentException();
		}

		PackageManager pkgMgr = context.getPackageManager();
		PackageInfo pkgInfo;
		try {
			pkgInfo = pkgMgr.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
		} catch (NameNotFoundException e) {
			// if the current package is not found, then there is a serious error. Don't try to handle it - just crash.
			e.printStackTrace();
			throw new RuntimeException();
		}
		if (pkgInfo == null) {
			// If we get nothing back from the package manager, something has gone wrong
			// outside our control. Die.
			throw new RuntimeException("PDroidInterfaceHelper:isAuthorised:pkgMgr returned null");
		}

		boolean hasWritePermission = false;
		if (pkgInfo.permissions != null && pkgInfo.permissions.length != 0) {
			for (PermissionInfo permission : pkgInfo.permissions) {
				if (permission.name.equals(PDROID_WRITE_PERMISSION)) {
					hasWritePermission = true;
					break;
				}
			}
		}
		Method authMethod;
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		try {
			authMethod = PrivacySettingsManager.class.getMethod("getIsAuthorizedManager", String.class);
			return (hasWritePermission && (Boolean)authMethod.invoke(privacySettingsManager, context.getPackageName()));
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
			Log.d("PDroidManager","PDroidInterfaceHelper:isAuthorised:getIsAuthorizedManager exists but with unexpected signature");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.d("PDroidManager","PDroidInterfaceHelper:isAuthorised:getIsAuthorizedManager exists but with unexpected permissions");
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Log.d("PDroidManager","PDroidInterfaceHelper:isAuthorised:getIsAuthorizedManager exists but something went wrong using it");
			e.printStackTrace();
		}
		// if PDroid does not have the 'getIsAuthorizedManager' function,
		// and thus does not have PAC installed, the all that determines the ability
		// to update the core is the permission
		// Likewise, in other cases our best guess is still the 'hasWritePermission'
		return hasWritePermission;

	}
	
	public static double getPDroidVersion (Context context) throws PDroidNotPresentException {
		if (!isPDroidInstalled(context)) {
			throw new PDroidNotPresentException();
		}
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		return privacySettingsManager.getVersion();
	}
	
	/**
	 * Checks if the PDroid is installed, by checking that the system service
	 * "privacy" returns something, and that the PrivacySettingsManager class is present
	 * @return
	 */
	public static boolean isPDroidInstalled(Context context) {
		try {
			Object privacySettingsManager = context.getSystemService("privacy");
			if (privacySettingsManager == null) {
				return false;
			}
			Class.forName("PrivacySettingsManager");
		} catch (ClassNotFoundException e) {
			return false;
		}
		return true;
	}

}
