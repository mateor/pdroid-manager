package net.digitalfeed.pdroidalternative;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class AppListGeneratorTask extends AsyncTask<Void, Integer, Application []> {
	
	protected static final int PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME = 0;
	protected static final int PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION = 1;
	
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL = 0;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME = 1;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID = 2;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE = 3;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS = 4;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON = 5;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS = 6;
	
	IAsyncTaskCallbackWithProgress<Application []> listener;
	
	Context context;
	int includeAppTypes;
	
	public AppListGeneratorTask(Context context, IAsyncTaskCallbackWithProgress<Application []> listener) {
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
	protected Application [] doInBackground(Void... params) {
		LinkedList<Application> appList = new LinkedList<Application>();
		PackageManager pkgMgr = context.getPackageManager();
		
		List<ApplicationInfo> installedApps = pkgMgr.getInstalledApplications(PackageManager.GET_META_DATA);

		Integer[] progressObject = new Integer[2];
		progressObject[0] = 0;
		progressObject[1] = installedApps.size();
		
		publishProgress(progressObject.clone());

		SQLiteDatabase write_db = DBInterface.getInstance(context).getDBHelper().getWritableDatabase();
		//Being inside a transaction speeds things up (see http://www.outofwhatbox.com/blog/2010/12/android-using-databaseutils-inserthelper-for-faster-insertions-into-sqlite-database/)
		//I haven't personally checked this
		write_db.beginTransaction();
		//Clear the application list before putting in a new list.
		write_db.delete(DBInterface.ApplicationTable.TABLE_NAME, null, null);		

		InsertHelper applicationsInsertHelper = new InsertHelper(write_db, DBInterface.ApplicationTable.TABLE_NAME);
		int [] applicationTableColumnNumbers = new int[7];
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_LABEL);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_UID);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PERMISSIONS);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_ICON);
		applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS] = applicationsInsertHelper.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_FLAGS);

		
		InsertHelper permissionsInsertHelper = new InsertHelper(write_db, DBInterface.PermissionApplicationTable.TABLE_NAME);
		int [] permissionsTableColumnNumbers = new int[2];
		//I was thinking about using enums instead of static finals here, but apparently the performance in android for enums is not so good??
		permissionsTableColumnNumbers[PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PACKAGENAME);
		permissionsTableColumnNumbers[PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION] = permissionsInsertHelper.getColumnIndex(DBInterface.PermissionApplicationTable.COLUMN_NAME_PERMISSION);
		
		/*
		 * We don't (and can't) filter out apps based on permissions, because some things we provide settings for
		 * don't require any permissions (e.g. ANDROID_ID)
		 */
		
		for (ApplicationInfo appInfo : installedApps) {
			try {
				PackageInfo pkgInfo = pkgMgr.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS);

				int appFlags = 0;
				if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) { 
					appFlags = Application.APP_FLAG_IS_SYSTEM_APP;
				}
				
				if (pkgMgr.checkPermission("android.permission.INTERNET", appInfo.packageName) == PackageManager.PERMISSION_GRANTED) {
					appFlags = appFlags | Application.APP_FLAG_HAS_INTERNET;
				}

				/*
				 * An alternative to putting the apps in the database and simultaneously creating a list of them
				 * to return is to just write to the database, then re-read them afterwards. Since we
				 * want the app list anyway, this makes more sense to me.
				 */
				
				applicationsInsertHelper.prepareForInsert();
				String [] permissions = pkgInfo.requestedPermissions;
				if (permissions != null) {
					applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS], TextUtils.join(",", pkgInfo.requestedPermissions));
					for (String permission : permissions) {
						permissionsInsertHelper.prepareForInsert();
						permissionsInsertHelper.bind(permissionsTableColumnNumbers[PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME], appInfo.packageName);
						permissionsInsertHelper.bind(permissionsTableColumnNumbers[PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION], permission);
						permissionsInsertHelper.execute();
					}
				} else {
					applicationsInsertHelper.bindNull(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS]);
				}
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL], pkgMgr.getApplicationLabel(appInfo).toString());
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME], appInfo.packageName);
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID], appInfo.uid);
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE], pkgInfo.versionCode);
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON], IconHelper.getIconByteArray(pkgMgr.getApplicationIcon(appInfo.packageName)));
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS], appFlags);
				Log.d("PDroidAddon","Application write :" + applicationsInsertHelper.execute());
				
				Application app = new Application(
						appInfo.packageName,
						pkgMgr.getApplicationLabel(appInfo).toString(),
						pkgInfo.versionCode,
						appFlags,
						0,
						appInfo.uid,
						pkgMgr.getApplicationIcon(appInfo.packageName),
						permissions
						);
				appList.add(app);
				
				//Below is the alterative to using the insert helper. It is 'neater' but is almost certainly slower because
				//it doesn't use compiled SQL queries
				//write_db.insert(DBInterface.ApplicationTable.TABLE_NAME, null, DBInterface.ApplicationTable.getContentValues(app));					
			} catch (NameNotFoundException e) {	
				Log.d("PDroidAlternative", String.format("Application %s went missing from installed applications list", appInfo.packageName));
			}
			progressObject[0] += 1;
			publishProgress(progressObject.clone());
		}
		
		write_db.rawQuery(DBInterface.QUERY_DELETE_APPS_WITHOUT_STATUS, null);
		write_db.setTransactionSuccessful();
		write_db.endTransaction();
		write_db.close();
		return appList.toArray(new Application [appList.size()]);
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		listener.asyncTaskProgressUpdate(progress);
	}
	
	@Override
	protected void onPostExecute(Application [] result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
}