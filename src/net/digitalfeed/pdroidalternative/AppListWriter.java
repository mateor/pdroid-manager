package net.digitalfeed.pdroidalternative;

import android.content.Context;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;

public class AppListWriter extends AsyncTask<Application, Void, Void> {
	protected static final int PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME = 0;
	protected static final int PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION = 1;
	
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL = 0;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME = 1;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID = 2;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE = 3;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS = 4;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON = 5;
	protected static final int APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS = 6;
	
	private Context context;
	
	public AppListWriter(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Application... params) {
		SQLiteDatabase write_db = DBInterface.getInstance(context).getDBHelper().getWritableDatabase();
		//Clear the application list before putting in a new list.
		write_db.delete(DBInterface.ApplicationTable.TABLE_NAME, null, null);
		//Being inside a transaction speeds things up (see http://www.outofwhatbox.com/blog/2010/12/android-using-databaseutils-inserthelper-for-faster-insertions-into-sqlite-database/)
		//I haven't personally checked this
		write_db.beginTransaction();

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

		for (Application app : params) {
			applicationsInsertHelper.prepareForInsert();
			String [] permissions = app.getPermissions();
			if (permissions != null) {
				applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS], TextUtils.join(",", permissions));
				for (String permission : permissions) {
					permissionsInsertHelper.prepareForInsert();
					permissionsInsertHelper.bind(PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME, app.getPackageName());
					permissionsInsertHelper.bind(PERMISSIONS_TABLE_COLUMN_NUMBER_OFFSET_PERMISSION, permission);
					permissionsInsertHelper.execute();
				}
			} else {
				applicationsInsertHelper.bindNull(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PERMISSIONS]);
			}
			applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_LABEL], app.getLabel());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_PACKAGENAME], app.getPackageName());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_UID], app.getUid());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_VERSIONCODE], app.getVersionCode());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_ICON], app.getIconByteArray());
			applicationsInsertHelper.bind(applicationTableColumnNumbers[APPLICATION_TABLE_COLUMN_NUMBER_OFFSET_APPFLAGS], app.getAppFlags());
			applicationsInsertHelper.execute();
		}
		
		write_db.rawQuery(DBInterface.QUERY_DELETE_APPS_WITHOUT_STATUS, null);
		write_db.setTransactionSuccessful();
		write_db.endTransaction();
		write_db.close();
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		Preferences prefs = new Preferences(context);
		prefs.setIsApplicationListCacheValid(true);
	}
}
