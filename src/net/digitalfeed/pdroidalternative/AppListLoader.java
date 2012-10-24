package net.digitalfeed.pdroidalternative;

import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class AppListLoader extends AsyncTask<Void, Integer, Application[]> {
	
	IAppListListener listener;
	
	Context context;
	
	public AppListLoader(Context context, IAppListListener listener) {
		this.context = context;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	@Override
	protected Application[] doInBackground(Void... params) {
		LinkedList<Application> appList = new LinkedList<Application>();
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
    	Cursor cursor = db.rawQuery(DBInterface.ApplicationListQuery, null);

		Integer[] progressObject = new Integer[2];
		progressObject[0] = 0;
		progressObject[1] = cursor.getCount();

		cursor.moveToFirst();
    	int labelColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_LABEL);
    	int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME); 
    	int uidColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_UID);
    	int versionCodeColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_VERSIONCODE);
    	int appFlagsColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_FLAGS);
    	int statusFlagsColumn = cursor.getColumnIndex(DBInterface.ApplicationStatusTable.TABLE_NAME + "." + DBInterface.ApplicationStatusTable.COLUMN_NAME_FLAGS);
    	int iconColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_ICON);

    	do {
    		String label = cursor.getString(labelColumn);
    		String packageName = cursor.getString(packageNameColumn);
    		int uid = cursor.getInt(uidColumn);
    		int versionCode = cursor.getInt(versionCodeColumn);
    		int appFlags = cursor.getInt(appFlagsColumn);
    		int statusFlags = cursor.getInt(statusFlagsColumn);
    		byte[] iconBlob = cursor.getBlob(iconColumn);

    		Drawable icon = new BitmapDrawable(context.getResources(),BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length));
    		appList.add(new Application(packageName, label, versionCode, appFlags, statusFlags, uid, icon));
    		progressObject[0]++;
    		publishProgress(progressObject.clone());
    	} while (cursor.moveToNext());

    	cursor.close();
    	db.close();
    	
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
