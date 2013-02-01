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
package net.digitalfeed.pdroidalternative.intenthandler;

import java.util.Arrays;
import java.util.HashSet;

import net.digitalfeed.pdroidalternative.AppDetailActivity;
import net.digitalfeed.pdroidalternative.Application;
import net.digitalfeed.pdroidalternative.DBInterface;
import net.digitalfeed.pdroidalternative.GlobalConstants;
import net.digitalfeed.pdroidalternative.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class PackageChangeHandler extends BroadcastReceiver {

	enum NotificationType { newinstall, update };
	
	public PackageChangeHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"PackageChangeHandler event received");
		//Get the package name. If this fails, then the intent didn't contain the
		//essential data and should be ignored
		String packageName;
		Uri inputUri = Uri.parse(intent.getDataString());
		if (!inputUri.getScheme().equals("package")) {
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Intent scheme was not 'package'");
			return;
		}
		packageName = inputUri.getSchemeSpecificPart();
		
		//If a package is being removed, we only want to delete the related
		//info it is not being updated/replaced by a newer version
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
			if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Triggering application deletion for package:" + packageName);
				DBInterface.getInstance(context).deleteApplicationRecord(packageName);
			}
		} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
			//If the package is just getting updated, then we only need to notify the user
			//if the permissions have changed
						
			if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
				//TODO: check if the permissions have changed
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"PackageChangeHandler: App being replaced: " + packageName);
				Application oldApp = Application.fromDatabase(context, packageName);
				Application newApp = Application.fromPackageName(context, packageName);
				if (havePermissionsChanged(context, oldApp, newApp)) {
					//TODO: Handle permission change
					/*
					 * This is an app for which the permissions have been updated, 
					 * so we need to update the database to include only correct permissions.
					 * Maybe we should have some way of flagging new ones?
					 */
					newApp.setStatusFlags((newApp.getStatusFlags() | oldApp.getStatusFlags() | Application.STATUS_FLAG_UPDATED) & ~Application.STATUS_FLAG_NEW);
					DBInterface.getInstance(context).updateApplicationRecord(newApp);
					
					displayNotification(context, NotificationType.update, packageName, DBInterface.getInstance(context).getApplicationLabel(packageName), DBInterface.getInstance(context).getApplicationRowId(packageName));
				}
			} else {
				/*
				 * This is a new app, not an app being replaced. We need to add it to the
				 * database, then display a notification for it
				 */
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"PackageChangeHandler: New app added: " + packageName);
				/*
				 * I'm not sure if we really want to be doing all this processing here, but
				 * we do need to record a list of new/updated apps to write to the database
				 * when the app next starts, or the intent is executed - or we do it now
				 */
				//get the application from the system, not from the database
				Application app = Application.fromPackageName(context, packageName);
				app.setStatusFlags(app.getStatusFlags() | Application.STATUS_FLAG_NEW);
				DBInterface.getInstance(context).addApplicationRecord(app);
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"PackageChangeHandler: New app record added: " + packageName);
				displayNotification(context, NotificationType.newinstall, packageName, app.getLabel(), DBInterface.getInstance(context).getApplicationRowId(packageName));
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"PackageChangeHandler: Notification presented: " + packageName);
			}
		}
	}
	

	    
	private void displayNotification(Context context, NotificationType notificationType, String packageName, String label, Integer NotificationId) {
		//This pattern is essentially taken from
		//https://developer.android.com/guide/topics/ui/notifiers/notifications.html
		Resources res = context.getResources();
		//TODO: Fix the icon in the notification bar				
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.notification_icon)
				.setAutoCancel(true)
				.setPriority(0)
				.setOnlyAlertOnce(true);
				//.setLargeIcon(res.getDrawable(R.drawable.allow_icon))
		
		String appLabel = DBInterface.getInstance(context).getApplicationLabel(packageName);
		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"new packagename is " + packageName);
		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"app label is " + appLabel);
		switch (notificationType) {
		case newinstall:
			builder.setContentTitle(appLabel + " " + res.getString(R.string.notification_newinstall_title))
			.setContentText(res.getString(R.string.notification_newinstall_text));
			break;
		case update:
			builder.setContentTitle(appLabel + " " + res.getString(R.string.notification_update_title))
			.setContentText(res.getString(R.string.notification_update_text));
			break;
		}
		
		Intent packageDetailIntent = new Intent(context, AppDetailActivity.class);
		packageDetailIntent.putExtra(AppDetailActivity.BUNDLE_PACKAGE_NAME, packageName);
		packageDetailIntent.putExtra(AppDetailActivity.BUNDLE_IN_APP, false);
		packageDetailIntent.setData((Uri.parse("custom://"+System.currentTimeMillis())));
		packageDetailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK &
				Intent.FLAG_ACTIVITY_CLEAR_TASK);
		
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(AppDetailActivity.class);
		stackBuilder.addNextIntent(packageDetailIntent);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, packageDetailIntent, 
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
    
		NotificationManager mNotificationManager =
		    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		mNotificationManager.notify(NotificationId, builder.build());
	}
	
	private boolean havePermissionsChanged(Context context, Application oldApp, Application newApp) {
		//TODO: Add detection of permissions change
		//Maybe add a table which stores 'changed since last' info, so new permissions can be highlighted?
		if (oldApp == null) {
			//this is an error situtation
			if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"oldApp == null; thus permissions have changed");
			return true;
		} else {
			/*
			 * Now to find if there are any non-matches between the two lists: have the permissions
			 * changed?
			 */
			//If only one array is null, then permissions have changed. If both are, then they haven't.
			if (oldApp.getPermissions() == null) {
				if (newApp.getPermissions() == null) {
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"oldApp and newApp permissions are both null; permissions have not changed");
					return false;
				} else {
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"oldApp permissions == null; newApp not; thus permissions have changed");
					return true;
				}
			} else if (newApp.getPermissions() == null) {
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"oldApp permissions != null; newApp are; thus permissions have changed");
				return true;
			}
			
			//if the number of permissions is different, then permissions have changed
			if (oldApp.getPermissions().length != newApp.getPermissions().length) {
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"permissions lengths differ: oldapp: " + Integer.toString(oldApp.getPermissions().length) + " newapp: " + Integer.toString(newApp.getPermissions().length));
				return true;
			}
			
			HashSet<String> currPermissions = new HashSet<String>();
			currPermissions.addAll(Arrays.asList(oldApp.getPermissions()));
			if (!currPermissions.containsAll(Arrays.asList(newApp.getPermissions()))) {
				if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"containsAll returned false: thus permissions have changed");
				return true;
			}
		}
		if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG,"Got to the end; permissions have stayed the same, it seems");
		return false;
	}
}