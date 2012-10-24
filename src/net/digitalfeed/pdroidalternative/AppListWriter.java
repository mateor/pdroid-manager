package net.digitalfeed.pdroidalternative;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class AppListWriter extends AsyncTask<Application, Void, Void> {

	private Context context;
	
	public AppListWriter(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Application... params) {
		SQLiteDatabase write_db = DBInterface.getInstance(context).getDBHelper().getWritableDatabase();
		write_db.delete(DBInterface.ApplicationTable.TABLE_NAME, null, null);
		for (Application app : params) {
			write_db.insert(DBInterface.ApplicationTable.TABLE_NAME, null, DBInterface.ApplicationTable.getContentValues(app));
		}
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
