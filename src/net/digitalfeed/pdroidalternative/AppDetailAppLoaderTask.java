package net.digitalfeed.pdroidalternative;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;


/**
 * Loads app details for a single app by package name. Assumes the app exists in the database.
 * Maybe should be merged into AppListLoader? There is mostly duplicate functionality
 * @author smorgan
 */
public class AppDetailAppLoaderTask extends AsyncTask<String, Integer, Application> {
	
	IAsyncTaskCallback<Application> listener;
	
	Context context;
	
	public AppDetailAppLoaderTask(Context context, IAsyncTaskCallback<Application> listener) {
		this.context = context;
		this.listener = listener;
	}
		
	@Override
	protected Application doInBackground(String... selectPackageName) {
		Log.d("PDroidAlternative","Looking up package name: " + selectPackageName[0]);
		
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
    	Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_APPS_BY_PACKAGENAME_WITH_STATUS, selectPackageName);
    	Log.d("PDroidAlternative","Returned item count: " + Integer.toString(cursor.getCount()));
		
		cursor.moveToFirst();
		/*
    	int labelColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_LABEL);
    	int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME); 
    	int uidColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_UID);
    	int versionCodeColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE);
    	int appFlagsColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_FLAGS);
    	int iconColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_ICON);
    	int permissionsColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_PERMISSIONS);

		String label = cursor.getString(labelColumn);
		String packageName = cursor.getString(packageNameColumn);
		int uid = cursor.getInt(uidColumn);
		int versionCode = cursor.getInt(versionCodeColumn);
		int appFlags = cursor.getInt(appFlagsColumn);
		byte[] iconBlob = cursor.getBlob(iconColumn);
		String permissions = cursor.getString(permissionsColumn);
		*/

		String label = cursor.getString(
				cursor.getColumnIndex(
						DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_LABEL
						)
				);
		String packageName = cursor.getString(
				cursor.getColumnIndex(
						DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME
						)
				);
		int uid = cursor.getInt(
				cursor.getColumnIndex(
						DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_UID
						)
				);
		int versionCode = cursor.getInt(
				cursor.getColumnIndex(
						DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE
						)
				);
		int appFlags = cursor.getInt(
				cursor.getColumnIndex(
						DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_FLAGS
						)
				);
		byte[] iconBlob = cursor.getBlob(
				cursor.getColumnIndex(
						DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_ICON
						)
				);
		String permissions = cursor.getString(
				cursor.getColumnIndex(
						DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_PERMISSIONS
						)
				);

		Drawable icon = new BitmapDrawable(context.getResources(),BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length));
		
		String [] permissionsArray = null;
		if (permissions != null) {
			permissionsArray = TextUtils.split(permissions, ",");
		}
	
		cursor.close();
    	db.close();
    	
    	
    	return new Application(packageName, label, versionCode, appFlags, 0, uid, icon, permissionsArray);
	}
	
	@Override
	protected void onPostExecute(Application result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}
