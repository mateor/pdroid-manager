package net.digitalfeed.pdroidalternative;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class AppListGenerator extends AsyncTask<Void, Integer, Application[]> {
	
	IAppListListener listener;
	
	Context context;
	int includeAppTypes;
	
	public AppListGenerator(Context context, IAppListListener listener) {
		this.context = context;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	/**
	 * Retrieves the list of applications, and returns as an array of Application objects
	 */
	@Override
	protected Application[] doInBackground(Void... params) {
		
		/*
		 * Read the list of permissions about which we care from XML files 
		 */
		/*
		Resources resources = context.getResources();
		String packageName = context.getPackageName();
		
		HashSet<String> permissionOfInterest = new HashSet<String>();
		
		XmlResourceParser xrp = resources.getXml(R.xml.pdroid_settings);
		try {
			xrp = resources.getXml(R.xml.permission_setting_map);
			int eventType = xrp.next();
			while(!(eventType == XmlResourceParser.START_TAG && xrp.getName().equals("permission")) && eventType != XmlResourceParser.END_DOCUMENT) {
				eventType = xrp.next();
			}
			while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("permission")) {
				String id = xrp.getIdAttribute();
				eventType = xrp.next();
				while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
					eventType = xrp.next();
				}
				while (eventType == XmlResourceParser.START_TAG && xrp.getName().equals("setting")) {
					Log.d("PDroidAlternative","permission ID:" + id + " setting ID: " + xrp.getIdAttribute());
					permissionOfInterest.add(id);
					eventType = xrp.next();
					while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
						eventType = xrp.next();
					}
			        if (eventType == XmlResourceParser.END_TAG && xrp.getName().equals("setting")) {
			        	eventType = xrp.next();
						while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
							eventType = xrp.next();
						}
			        } else {
			        	break;
			        }
				}
				if (eventType == XmlResourceParser.END_TAG && xrp.getName().equals("setting")) {
					eventType = xrp.next();
					while(eventType == XmlResourceParser.TEXT && xrp.isWhitespace()) {
						eventType = xrp.next();
					}
				} else {
					break;
				}
			}
		} catch (Exception e) {
			Log.d("PDroidAlternative",e.getMessage());
			//TODO: Exception handling, mayhaps?
		}
		*/
		LinkedList<Application> appList = new LinkedList<Application>();
		PackageManager pkgMgr = context.getPackageManager();
		
		List<ApplicationInfo> installedApps = pkgMgr.getInstalledApplications(PackageManager.GET_META_DATA);

		Integer[] progressObject = new Integer[2];
		progressObject[0] = 0;
		progressObject[1] = installedApps.size();
		
		publishProgress(progressObject.clone());

		for (ApplicationInfo appInfo : installedApps) {
			try {
				progressObject[0] += 1;
				PackageInfo pkgInfo = pkgMgr.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS);
				String [] requestedPermissions = pkgInfo.requestedPermissions;
				//if (requestedPermissions != null) {
				//	boolean appOfInterest = false;
				//	for (String permission : requestedPermissions) {
				//		if (permissionOfInterest.contains(permission)) {
				//			appOfInterest = true;
				//			break;
				//		}
				//	}
				//	if (appOfInterest) {
						int appFlags = 0;
						if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) { 
							appFlags = Application.APP_FLAG_IS_SYSTEM_APP;
						}
						
						if (pkgMgr.checkPermission("android.permission.INTERNET", appInfo.packageName) == PackageManager.PERMISSION_GRANTED) {
							appFlags = appFlags | Application.APP_FLAG_HAS_INTERNET;
						}
		
						Application thisApp = new Application(
								appInfo.packageName,
								pkgMgr.getApplicationLabel(appInfo).toString(),
								pkgInfo.versionCode,
								appFlags,
								0,
								appInfo.uid,
								pkgMgr.getApplicationIcon(appInfo.packageName),
								pkgInfo.requestedPermissions
								);
						
						appList.add(thisApp);
					//}
				//}
			} catch (NameNotFoundException e) {	
				Log.d("PDroidAlternative", String.format("Application %s went missing from installed applications list", appInfo.packageName));
			}
			publishProgress(progressObject.clone());
		}
		
		return appList.toArray(new Application[appList.size()]);
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		listener.appListProgressUpdate(progress);
	}
	
	@Override
	protected void onPostExecute(Application[] result) {
		super.onPostExecute(result);
		listener.appListLoadCompleted(result);
	}
}