package net.digitalfeed.pdroidalternative;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.widget.ListView;

public class AppDetailActivity extends Activity {
	
	public static final String BUNDLE_PACKAGE_NAME = "packageName";
	
	private Application application;
	private Setting [] settingList;
	private String packageName;
	private ListView listView;
	private Context context;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);
    }

    @Override
    public void onStart() {
    	super.onStart();
    	context = this;
        Bundle bundle = getIntent().getExtras();
        packageName = bundle.getString(BUNDLE_PACKAGE_NAME);
        //this.setTitle(packageName);
        AppDetailAppLoaderTask appDetailAppLoader = new AppDetailAppLoaderTask(this, new AppDetailAppLoaderTaskCompleteHandler());
        appDetailAppLoader.execute(packageName);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_app_detail_activity2, menu);
        return true;
    }

    class AppDetailAppLoaderTaskCompleteHandler implements IAsyncTaskCallback<Application>
    {
		@Override
		public void asyncTaskComplete(Application inApplication) {
			setTitle(inApplication.getLabel());
			application = inApplication;
			AppDetailSettingsLoaderTask appDetailSettingsLoader = new AppDetailSettingsLoaderTask(context, new AppDetailSettingsLoaderTaskCompleteHandler());
			appDetailSettingsLoader.execute(application.getPackageName());
			
		}
    }
    
    class AppDetailSettingsLoaderTaskCompleteHandler implements IAsyncTaskCallback<LinkedList<Setting>>
    {
		@Override
		public void asyncTaskComplete(LinkedList<Setting> inSettingList) {
			if (inSettingList != null) {
				settingList = inSettingList.toArray(new Setting[inSettingList.size()]); 
				listView = (ListView)findViewById(R.id.settingList);
				listView.setAdapter(new AppDetailAdapter(context, R.layout.setting_list_row_allow_deny, settingList));
			}
		}
    }
}