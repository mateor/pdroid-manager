package net.digitalfeed.pdroidalternative;

import java.util.Set;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ListView;

public class AppDetailActivity extends Activity implements IAsyncTaskCallback<Application> {
	
	public static final String BUNDLE_PACKAGE_NAME = "packageName";
	
	private Setting [] settingList;
	private String packageName;
	private ListView listView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);
    }

    @Override
    public void onStart() {
    	super.onStart();
        Bundle bundle = getIntent().getExtras();
        packageName = bundle.getString(BUNDLE_PACKAGE_NAME);
        //this.setTitle(packageName);
        AppDetailLoaderTask appDetailLoader = new AppDetailLoaderTask(this, this);
        appDetailLoader.execute(packageName);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_app_detail_activity2, menu);
        return true;
    }

	@Override
	public void asyncTaskComplete(Application app) {
		this.setTitle(app.getLabel());
		String [] permissions = app.getPermissions();
		Set<Setting> settings = PermissionSettingMapper.getMapper(this).getSettings(permissions);
		if (settings != null) {
			this.settingList = settings.toArray(new Setting[settings.size()]); 
			listView = (ListView)findViewById(R.id.settingList);
			listView.setAdapter(new AppDetailAdapter(this, R.layout.setting_list_row_allow_deny, this.settingList));
		}
	}
}