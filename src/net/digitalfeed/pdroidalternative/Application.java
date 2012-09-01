package net.digitalfeed.pdroidalternative;

import android.graphics.drawable.Drawable;

public class Application {
	String packageName;
	String name;
	String label;
	int versionCode;
	String versionName;
	boolean isSystemApp;
	int uid;
	Drawable icon;
	String[] permissions;
	
	Application(String packageName, String name, String label, int versionCode, String versionName, boolean isSystemApp, int uid, Drawable icon, String[] permissions) {
		this.packageName = packageName;
		this.name = name;
		this.label = label;
		this.versionCode = versionCode;
		this.isSystemApp = isSystemApp;
		this.versionName = versionName;
		this.uid = uid;
		this.icon = icon;
		if (this.permissions != null) { 
			this.permissions = permissions.clone();
		} else {
			this.permissions = null;
		}
		
	}	
}
