package net.digitalfeed.pdroidalternative;

import java.util.LinkedList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

public class AppListGenerator extends AsyncTask<Integer, Integer, Application[]> {

	public static final int TYPE_USER = 1;
	public static final int TYPE_SYSTEM = 2;
	
	IAppListListener listener;
	
	Context context;
	ProgressDialog progDialog;
	int includeAppTypes;
	
	public AppListGenerator(Context context, IAppListListener listener) {
		this.context = context;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
        this.progDialog = new ProgressDialog(this.context);
        this.progDialog.setMessage("Loading Application List");
        this.progDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	}
	
	/**
	 * Retrieves the list of applications, and returns as an array of Application objects
	 * 
	 * @param	appTypes	A two-item array, in which if appTypes[0] == true, normal applications are retrieved; if appTypes[1] == true, system apps are included
	 */
	@Override
	protected Application[] doInBackground(Integer... appTypes) {
		LinkedList<Application> appList = new LinkedList<Application>();
		PackageManager pkgMgr = context.getPackageManager();
		
		List<ApplicationInfo> installedApps = pkgMgr.getInstalledApplications(PackageManager.GET_META_DATA);

		Integer[] progressObject = new Integer[2];
		progressObject[0] = 0;
		
		if (appTypes[0] == (AppListGenerator.TYPE_USER | AppListGenerator.TYPE_SYSTEM)) {
			progressObject[1] = installedApps.size(); 
		} else {
			progressObject[1] = 0;
			for (ApplicationInfo appInfo : installedApps) {
				if ((((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & AppListGenerator.TYPE_SYSTEM) == AppListGenerator.TYPE_SYSTEM)) ||
						(((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & AppListGenerator.TYPE_USER) == AppListGenerator.TYPE_USER))) {
					progressObject[1]++;
				}
			}
		}
		
		publishProgress(progressObject.clone());
        Log.d("PDroidAlternative", "Showed");

		for (ApplicationInfo appInfo : installedApps) {
			progressObject[0] += 1;
			try {
				if ((((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & AppListGenerator.TYPE_SYSTEM) == AppListGenerator.TYPE_SYSTEM)) ||
						(((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & AppListGenerator.TYPE_USER) == AppListGenerator.TYPE_USER))) {
					PackageInfo pkgInfo = pkgMgr.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS);
				
					Application thisApp = new Application(
							appInfo.packageName,
							appInfo.name,
							pkgMgr.getApplicationLabel(appInfo).toString(),
							pkgInfo.versionCode,
							pkgInfo.versionName,
							(appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM,
							appInfo.uid,
							pkgMgr.getApplicationIcon(appInfo.packageName),
							pkgInfo.requestedPermissions
							);
					
					appList.add(thisApp);
				}
			} catch (NameNotFoundException e) {	
				Log.d("PDroidAlternative", String.format("Application %s went missing from installed applications list", appInfo.packageName));
			}
			
			publishProgress(progressObject.clone());
		}
		
		return appList.toArray(new Application[appList.size()]);
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		if (this.progDialog.isShowing()) {
			this.progDialog.setProgress(progress[0]);
		} else {
			this.progDialog.setMax(progress[1]);
			this.progDialog.setProgress(progress[0]);
			this.progDialog.show();
		}
	}
	
	@Override
	protected void onPostExecute(Application[] result) {
		super.onPostExecute(result);
		listener.appListLoadCompleted(result);
        //listView.setAdapter(new ArrayAdapter<Application>(context, R.id.applicationItem, appList));
		progDialog.dismiss();
	}
}