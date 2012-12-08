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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppListAdapter extends ArrayAdapter<Application>{
	
	private static final int APP_TYPE_LABEL_SYSTEM = 0;
	private static final int APP_TYPE_LABEL_USER = 1;
	
	private static final int APP_STATUS_LABEL_UNTRUSTED = 0;
	private static final int APP_STATUS_LABEL_TRUSTED = 1;
	private static final int APP_STATUS_LABEL_NOSETTINGS = 2;
	
	Context context;
	int standardResourceId;
	List<Application>appList = null;
	Drawable [] internetAccessIcons;
	String [] appTypeLabels;
	String [] appStatusLabels;
	
	public AppListAdapter(Context context, int standardResourceId, List<Application> appList) {
		super(context, standardResourceId, appList);
		Log.d("PDroidAlternative", "AppListAdapter:AppListAdapter");
		
		this.context = context;
		this.standardResourceId = standardResourceId;
		this.appList = appList;
		Resources resources = context.getResources(); 
		appTypeLabels = resources.getStringArray(R.array.app_type_labels);
		appStatusLabels = resources.getStringArray(R.array.app_status_labels);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Log.d("PDroidAlternative", "AppListAdapter:getView");
		View row = convertView;
		AppHolder holder = null;
		
		if (row == null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(this.standardResourceId, parent, false);
	
			holder = new AppHolder();	
			holder.icon = (ImageView)row.findViewById(R.id.application_list_app_icon);			
			holder.appLabel = (TextView)row.findViewById(R.id.application_list_app_label);			
			holder.appType = (TextView)row.findViewById(R.id.application_list_app_type);
			holder.appStatus = (TextView)row.findViewById(R.id.application_list_app_status);
			holder.hasNetIcon = (ImageView)row.findViewById(R.id.application_list_has_net_icon);
			holder.hasNoNetIcon = (ImageView)row.findViewById(R.id.application_list_has_no_net_icon);
			row.setTag(holder);
		} else {
			holder = (AppHolder)row.getTag();
		}
		
		Application app = appList.get(position);
		holder.icon.setImageDrawable(app.getIcon());
		holder.appLabel.setText(app.getLabel());
		if (app.getIsSystemApp()) {
			holder.appType.setText(appTypeLabels[APP_TYPE_LABEL_SYSTEM]);
		} else {
			holder.appType.setText(appTypeLabels[APP_TYPE_LABEL_USER]);
		}
		if (app.getHasSettings()) {
			if (app.getIsUntrusted()) {
				holder.appStatus.setText(appStatusLabels[APP_STATUS_LABEL_UNTRUSTED]);
			} else {
				holder.appStatus.setText(appStatusLabels[APP_STATUS_LABEL_TRUSTED]);
			}
		} else {
			holder.appStatus.setText(appStatusLabels[APP_STATUS_LABEL_NOSETTINGS]);
		}
		if (app.getHasInternet()) {
			holder.hasNetIcon.setVisibility(View.VISIBLE);
			holder.hasNoNetIcon.setVisibility(View.INVISIBLE);
		} else {
			holder.hasNetIcon.setVisibility(View.INVISIBLE);
			holder.hasNoNetIcon.setVisibility(View.VISIBLE);
		}
		
		return row;
	}
	
	static class AppHolder
	{
		ImageView icon;
		TextView appLabel;
		TextView appType;
		TextView appStatus;
		ImageView hasNetIcon;
		ImageView hasNoNetIcon;
	}
}
