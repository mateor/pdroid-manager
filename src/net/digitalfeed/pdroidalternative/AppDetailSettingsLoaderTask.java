package net.digitalfeed.pdroidalternative;

import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;


/**
 * Loads the settings list for a single application from the database.
 * @author smorgan
 */
public class AppDetailSettingsLoaderTask extends AsyncTask<String, Integer, LinkedList<Setting>> {
	
	IAsyncTaskCallback<LinkedList<Setting>> listener;
	
	Context context;
	
	public AppDetailSettingsLoaderTask(Context context, IAsyncTaskCallback<LinkedList<Setting>> listener) {
		this.context = context;
		this.listener = listener;
	}
		
	@Override
	protected LinkedList<Setting> doInBackground(String... selectPackageName) {
		SQLiteDatabase db = DBInterface.getInstance(context).getDBHelper().getReadableDatabase();
		
		Cursor cursor = db.rawQuery(DBInterface.QUERY_GET_SETTINGS_BY_PACKAGENAME, selectPackageName);
    	int idColumn = cursor.getColumnIndex(DBInterface.SettingTable.TABLE_NAME + "." + DBInterface.SettingTable.COLUMN_NAME_ID);
    	int nameColumn = cursor.getColumnIndex(DBInterface.SettingTable.TABLE_NAME + "." + DBInterface.SettingTable.COLUMN_NAME_NAME);
    	int titleColumn = cursor.getColumnIndex(DBInterface.SettingTable.TABLE_NAME + "." + DBInterface.SettingTable.COLUMN_NAME_TITLE);
    	int groupColumn = cursor.getColumnIndex(DBInterface.SettingTable.TABLE_NAME + "." + DBInterface.SettingTable.COLUMN_NAME_GROUP_ID);
    	int groupTitleColumn = cursor.getColumnIndex(DBInterface.SettingTable.TABLE_NAME + "." + DBInterface.SettingTable.COLUMN_NAME_GROUP_TITLE);
    	int optionsColumn = cursor.getColumnIndex(DBInterface.SettingTable.TABLE_NAME + "." + DBInterface.SettingTable.COLUMN_NAME_OPTIONS);
    	
		cursor.moveToFirst();
		LinkedList<Setting> settingSet = new LinkedList<Setting>();
		do {
			String id = cursor.getString(idColumn);
			String name = cursor.getString(nameColumn);
			String title = cursor.getString(titleColumn);
			String group = cursor.getString(groupColumn);
			String groupTitle = cursor.getString(groupTitleColumn);
			String options = cursor.getString(optionsColumn);

			String [] optionsArray = null;
			if (options != null) {
				optionsArray = TextUtils.split(options, ",");
			}
			
			settingSet.add(new Setting(id, name, title, group, groupTitle, optionsArray));
		} while (cursor.moveToNext());
		cursor.close();
		db.close();
		return settingSet;
	}
	
	@Override
	protected void onPostExecute(LinkedList<Setting> result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}
