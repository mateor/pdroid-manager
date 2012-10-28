package net.digitalfeed.pdroidalternative;

import java.security.InvalidParameterException;
import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class AppListLoader {

	private Context context;
	private SearchType searchType;
	private String [] projectionIn;

	enum SearchType { ALL, PACKAGE_NAME, PERMISSION, TYPE }
	enum ResultsType { ALL, PACKAGE_NAME }

	public AppListLoader(Context context, SearchType searchType, String [] projectionIn) throws InvalidParameterException {
		this.context = context;
		this.searchType = searchType;
		if (projectionIn != null) {
			this.projectionIn = projectionIn.clone();
		} else {
			this.projectionIn = null;
		}
	}
	
	public Application [] getMatchingApplications() {
		String query;
		switch (searchType) {
		case ALL:
			query = DBInterface.QUERY_GET_ALL_APPS_WITH_STATUS;
			break;
		case PACKAGE_NAME:
			query = DBInterface.QUERY_GET_APPS_BY_LABEL_WITH_STATUS;
			break;
		case PERMISSION:
			query = DBInterface.QUERY_GET_APPS_BY_PERMISSION_WITH_STATUS;
			break;
		case TYPE:
			query = DBInterface.QUERY_GET_APPS_BY_TYPE_WITH_STATUS;
			break;
		default:
			throw new InvalidParameterException("Unsupported application list search type");
		}

		
		LinkedList<Application> appList = new LinkedList<Application>();
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		
    	Log.d("PDroidAlternative","Executing Query " + query);
    	
    	Cursor cursor = db.rawQuery(query, this.projectionIn);
    	
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
    	
    	Log.d("PDroidAlternative","Got matching applications: " + appList.size());
    	
    	return appList.toArray(new Application[appList.size()]);
	}
	
	public LinkedList<String> getMatchingPackageNames() {
		String query = "";
		switch (searchType) {
		case ALL:
			query = DBInterface.QUERY_GET_ALL_APPS_PACKAGENAME_ONLY;
			break;
		case PACKAGE_NAME:
			query = DBInterface.QUERY_GET_APPS_BY_LABEL_PACKAGENAME_ONLY;
			break;
		case PERMISSION:
			query = DBInterface.QUERY_GET_APPS_BY_PERMISSION_PACKAGENAME_ONLY;
			break;
		case TYPE:
			query = DBInterface.QUERY_GET_APPS_BY_TYPE_PACKAGENAME_ONLY;
			break;
		default:
			throw new InvalidParameterException("Unsupported application list search type");
		}

		LinkedList<String> packageList = new LinkedList<String>();
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		    	
    	Cursor cursor = db.rawQuery(query, this.projectionIn);
    	
		cursor.moveToFirst();
    	int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.TABLE_NAME + "." + DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME); 

    	do {
    		String packageName = cursor.getString(packageNameColumn);
    		packageList.add(packageName);
    	} while (cursor.moveToNext());

    	cursor.close();
    	db.close();
    	    	
    	return packageList;
	}
}
