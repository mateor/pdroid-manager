package net.digitalfeed.pdroidalternative;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppListAdapter extends ArrayAdapter<Application>{

	Context context;
	int textViewResourceId;
	Application[] appList = null;
	 
	public AppListAdapter(Context context, int textViewResourceId, Application[] appList) {
		super(context, textViewResourceId, appList);
		
		this.context = context;
		this.textViewResourceId = textViewResourceId;
		this.appList = appList;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		AppHolder holder = null;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(this.textViewResourceId,  parent, false);
			
			holder = new AppHolder();
			holder.icon = (ImageView)row.findViewById(R.id.icon);
			holder.appName = (TextView)row.findViewById(R.id.appName);
			holder.versionName = (TextView)row.findViewById(R.id.versionName);
			row.setTag(holder);
		} else {
            holder = (AppHolder)row.getTag();
        }
		
		Application app = appList[position];
		holder.icon.setImageDrawable(app.icon);
		holder.appName.setText(app.label);
		holder.versionName.setText(app.versionName);
		
		return row;
	}
	
	static class AppHolder
	{
		ImageView icon;
		TextView appName;
		TextView versionName;
	}
}
