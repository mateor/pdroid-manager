package net.digitalfeed.pdroidalternative;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.R.anim;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;


public class MainActivity extends Activity {

	Application[] appList;
	String[] appTitleList;
	ListView listView;
	Context context;
	
	int pkgCounter = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
        Log.d("PDroidAlternative", "got context");
        listView = (ListView)findViewById(R.id.applicationList);
        Log.d("PDroidAlternative", "got listview");
        if (appList == null) {
        	updateApplicationList();
        }
        context = this;
        Button btn = (Button)findViewById(R.id.button1);
        btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	if (appList != null) {
		    		PrivacySettingsManager psm = (PrivacySettingsManager)context.getSystemService("privacy");
		    		PrivacySettings pset = psm.getSettings(appList[pkgCounter].packageName);
		    		if (pset != null) {
			    		Log.d("PDroidAlternative", appList[pkgCounter].label + " " + Byte.toString(pset.getOutgoingCallsSetting()));
		    		} else {
		    			Log.d("PDroidAlternative", appList[pkgCounter].label + " null");
		    		}
		    		pkgCounter = pkgCounter++ % appList.length;
		    	}
			}
		});
        
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
	    		PrivacySettingsManager psm = (PrivacySettingsManager)context.getSystemService("privacy");
	    		PrivacySettings pset = psm.getSettings(appList[position].packageName);
	    		if (pset != null) {
		    		Log.d("PDroidAlternative", appList[position].label + " " + Byte.toString(pset.getOutgoingCallsSetting()));
		    		Toast.makeText(context, appList[position].packageName + ": " + Byte.toString(pset.getOutgoingCallsSetting()), Toast.LENGTH_SHORT).show();
	    		} else {
	    			Log.d("PDroidAlternative", appList[position].label + " null");
	    			Toast.makeText(context, appList[position].packageName + ": null =(", Toast.LENGTH_SHORT).show();
	    		}
	    		
	    		view.getTag();
	    		view.inflate(context, R.layout.application_list_row_expanded, parent)
	    		LayoutInflater inflater = ((Activity)context).getLayoutInflater();
				view = inflater.inflate(this.textViewResourceId,  parent, false);
			}
        	
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void updateApplicationList() {
    	Log.d("PDroidAlternative", "starting updateApplicationList");
    	//int appTypes = ApplicationListRetriever.TYPE_USER | ApplicationListRetriever.TYPE_SYSTEM;
    	//int appTypes = ApplicationListRetriever.TYPE_USER;
    	int appTypes = ApplicationListRetriever.TYPE_SYSTEM;
    	Log.d("PDroidAlternative", "set app types");
    	ApplicationListRetriever applicationListRetriever = new ApplicationListRetriever(this);
    	Log.d("PDroidAlternative", "Created applicationListRetriever");
    	applicationListRetriever.execute(appTypes);
    }
    
	public class ApplicationListRetriever extends AsyncTask<Integer, Integer, Application[]> {

		public static final int TYPE_USER = 1;
		public static final int TYPE_SYSTEM = 2;
		
		Context context;
		ProgressDialog progDialog;
		int includeAppTypes;
		
		public ApplicationListRetriever(Context context) {
			this.context = context;
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
			
			if (appTypes[0] == (ApplicationListRetriever.TYPE_USER | ApplicationListRetriever.TYPE_SYSTEM)) {
				progressObject[1] = installedApps.size(); 
			} else {
				progressObject[1] = 0;
				for (ApplicationInfo appInfo : installedApps) {
					if ((((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & ApplicationListRetriever.TYPE_SYSTEM) == ApplicationListRetriever.TYPE_SYSTEM)) ||
							(((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & ApplicationListRetriever.TYPE_USER) == ApplicationListRetriever.TYPE_USER))) {
						progressObject[1]++;
					}
				}
			}
			
			publishProgress(progressObject.clone());
	        Log.d("PDroidAlternative", "Showed");

			for (ApplicationInfo appInfo : installedApps) {
				progressObject[0] += 1;
				try {
					if ((((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & ApplicationListRetriever.TYPE_SYSTEM) == ApplicationListRetriever.TYPE_SYSTEM)) ||
							(((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) && ((appTypes[0] & ApplicationListRetriever.TYPE_USER) == ApplicationListRetriever.TYPE_USER))) {
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
			appList = result;
			appTitleList = new String[appList.length];
			for (int i=0;i<appList.length;i++) {
				appTitleList[i] = appList[i].label;
			}
	        //listView.setAdapter(new ArrayAdapter<Application>(context, R.id.applicationItem, appList));
			listView.setAdapter(new AppListAdapter(context, R.layout.application_list_row, appList));
			progDialog.dismiss();
		}
	}
}
