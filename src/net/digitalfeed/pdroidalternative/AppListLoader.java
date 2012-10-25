package net.digitalfeed.pdroidalternative;

import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class AppListLoader {

	private Context context;
	private String query;
	private String [] projectionIn;

	enum SearchType { PACKAGE_NAME, PERMISSION }

	public AppListLoader(Context context, SearchType searchType, String [] projectionIn) throws Exception {
		this.context = context;
		switch (searchType) {
		case PACKAGE_NAME:
			this.query = DBInterface.ApplicationByName;
			break;
		case PERMISSION:
			this.query = DBInterface.ApplicationByName;
			break;
		default:
			throw new Exception("Unsupported application list search type");
		}
		this.projectionIn = projectionIn.clone(); 
	}
	
	public Application [] getMatchingApplications() {
		LinkedList<Application> appList = new LinkedList<Application>();
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
    	Cursor cursor = db.rawQuery(this.query, this.projectionIn);

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
    	} while (cursor.moveToNext());

    	cursor.close();
    	db.close();
    	
    	return appList.toArray(new Application[appList.size()]);
	}
}
