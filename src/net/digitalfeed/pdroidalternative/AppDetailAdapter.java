package net.digitalfeed.pdroidalternative;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AppDetailAdapter extends ArrayAdapter<Setting>{

	Context context;
	int standardResourceId;
	Setting[] settingList = null;
	 
	public AppDetailAdapter(Context context, int standardResourceId, Setting[] settingList) {
		super(context, standardResourceId, settingList);
		
		this.context = context;
		this.standardResourceId = standardResourceId;
		this.settingList = settingList;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		SettingHolder holder = null;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(this.standardResourceId, parent, false);
	
			holder = new SettingHolder();			
			holder.settingName = (TextView)row.findViewById(R.id.settingName);				
			row.setTag(holder);
		} else {
			holder = (SettingHolder)row.getTag();
		}
		
		Setting setting = settingList[position];
		holder.settingName.setText(setting.getType());
		
		return row;
	}

	static class SettingHolder
	{
		TextView settingName;
	}

}