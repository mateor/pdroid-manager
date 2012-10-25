package net.digitalfeed.pdroidalternative;

import android.content.Context;
import android.os.AsyncTask;

public class AppListLoaderTask extends AsyncTask<AppListLoader, Integer, Application[]> {
	
	IAsyncTaskCallback<Application[]> listener;
	
	Context context;
	
	public AppListLoaderTask(Context context, IAsyncTaskCallback<Application[]> listener) {
		this.context = context;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute(){ 
		super.onPreExecute();
	}
	
	@Override
	protected Application[] doInBackground(AppListLoader... appListLoader) {
		if (appListLoader[0] != null) {
			return appListLoader[0].getMatchingApplications();
		} else {
			throw new NullPointerException("No AppListLoader was provided to the AppListLoaderTask");
		}
	}
	
	@Override
	protected void onPostExecute(Application[] result) {
		super.onPostExecute(result);
		listener.asyncTaskComplete(result);
	}
	
}
