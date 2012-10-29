package net.digitalfeed.pdroidalternative;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AppDetailAdapter extends ArrayAdapter<Setting>{
	protected static final int VIEW_TYPE_STANDARD = 0;
	protected static final int VIEW_TYPE_LOCATION = 1;
	protected static final int VIEW_TYPE_YESNO = 2;
	
	
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
	public int getViewTypeCount() {
		return 1;
	}
	
	@Override
	public int getItemViewType(int position) {
		return 0;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		SettingHolder holder = null;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(this.standardResourceId, parent, false);
	
			holder = new SettingHolder();			
			holder.settingName = (TextView)row.findViewById(R.id.option_title);				
			row.setTag(holder);
		} else {
			holder = (SettingHolder)row.getTag();
		}
		
		Setting setting = settingList[position];
		holder.settingName.setText(setting.getTitle());
		
		return row;
	}

	static class SettingHolder
	{
		TextView settingName;
	}

}