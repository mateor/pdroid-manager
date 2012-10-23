/**
 * Copyright (C) 2012 Simeon J. Morgan (smorgan@digitalfeed.net)
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 * The software has the following requirements (GNU GPL version 3 section 7):
 * You must retain in pdroid-manager, any modifications or derivatives of
 * pdroid-manager, or any code or components taken from pdroid-manager the author
 * attribution included in the files.
 * In pdroid-manager, any modifications or derivatives of pdroid-manager, or any
 * application utilizing code or components taken from pdroid-manager must include
 * in any display or listing of its creators, authors, contributors or developers
 * the names or pseudonyms included in the author attributions of pdroid-manager
 * or pdroid-manager derived code.
 * Modified or derivative versions of the pdroid-manager application must use an
 * alternative name, rather than the name pdroid-manager.
 */

/**
 * @author Simeon J. Morgan <smorgan@digitalfeed.net>
 */

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
	int standardResourceId;
	Application[] appList = null;
	 
	public AppListAdapter(Context context, int standardResourceId, Application[] appList) {
		super(context, standardResourceId, appList);
		
		this.context = context;
		this.standardResourceId = standardResourceId;
		this.appList = appList;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		AppHolder holder = null;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(this.standardResourceId, parent, false);
	
			holder = new AppHolder();	
			holder.icon = (ImageView)row.findViewById(R.id.icon);			
			holder.appName = (TextView)row.findViewById(R.id.appName);			
			holder.versionName = (TextView)row.findViewById(R.id.versionName);	
			row.setTag(holder);
		} else {
			holder = (AppHolder)row.getTag();
		}
		
		Application app = appList[position];
		holder.icon.setImageDrawable(app.getIcon());
		holder.appName.setText(app.getLabel());
		holder.versionName.setText(app.getPackageName());
		
		return row;
	}
	
	static class AppHolder
	{
		ImageView icon;
		TextView appName;
		TextView versionName;
	}
}
