package net.digitalfeed.pdroidalternative;

import android.media.audiofx.BassBoost.Settings;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.ListView;

public class AppDetailActivity extends Activity implements IAppDetailListener {
	
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
        AppDetailLoader appDetailLoader = new AppDetailLoader(this, this);
        appDetailLoader.execute(packageName);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_app_detail_activity2, menu);
        return true;
    }

	@Override
	public void appDetailLoadCompleted(Application app) {
		this.setTitle(app.getLabel());
		settingList = PermissionSettingMapper.getSettingsObjects(app.getPermissions());
        listView = (ListView)findViewById(R.id.settingList);
        listView.setAdapter(new AppDetailAdapter(this, R.layout.setting_list_row_allow_deny, this.settingList));
	}
}