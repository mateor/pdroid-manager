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
import net.digitalfeed.pdroidalternative.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class PackageChangeHandler extends BroadcastReceiver {

	enum NotificationType { newinstall, update };
	
	public PackageChangeHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("PDroidAlternative","Received notification of package change");
		//Get the package name. If this fails, then the intent didn't contain the
		//essential data and should be ignored
		String packageName;
		Uri inputUri = Uri.parse(intent.getDataString());
		if (!inputUri.getScheme().equals("package")) {
			Log.d("PDroidAlternative","Scheme name was not 'package'");
			return;
		}
		packageName = inputUri.getSchemeSpecificPart();
		
		//If a package is being removed, we only want to delete the related
		//info it is not being updated/replaced by a newer version
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
			if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
				Log.d("PDroidAlternative","Handling app removal for app " + packageName);
				DBInterface.getInstance(context).deleteApplicationRecord(packageName);
			}
		} else if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
			//If the package is just getting updated, then we only need to notify the user
			//if the permissions have changed
			
			Log.d("PDroidAlternative","Created new Application object for " + packageName);
			
			if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
				//TODO: check if the permissions have changed
				Log.d("PDroidAlternative","Handling app replacement for app " + packageName);
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
					displayNotification(context, NotificationType.update, packageName, DBInterface.getInstance(context).getApplicationLabel(packageName));
				}
			} else {
				/*
				 * This is a new app, not an app being replaced. We need to add it to the
				 * database, then display a notification for it
				 */
				Log.d("PDroidAlternative","Handling new app for " + packageName);
				/*
				 * I'm not sure if we really want to be doing all this processing here, but
				 * we do need to record a list of new/updated apps to write to the database
				 * when the app next starts, or the intent is executed - or we do it now
				 */
				//get the application from the system, not from the database
				Application app = Application.fromPackageName(context, packageName);
				app.setStatusFlags(app.getStatusFlags() | Application.STATUS_FLAG_NEW);
				DBInterface.getInstance(context).addApplicationRecord(app);
				Log.d("PDroidAlternative","Added application record for " + packageName);
				displayNotification(context, NotificationType.newinstall, packageName, app.getLabel());
				Log.d("PDroidAlternative","Added notification for " + packageName);
			}
		}
	}
	
	private void displayNotification(Context context, NotificationType notificationType, String packageName, String label) {
		//This pattern is essentially taken from
		//https://developer.android.com/guide/topics/ui/notifiers/notifications.html
		
		Resources res = context.getResources();

		//TODO: Fix the icon in the notification bar				
		Notification.Builder builder = new Notification.Builder(context)
				.setPriority(Notification.PRIORITY_MAX)
				.setSmallIcon(R.drawable.notification_icon);
				//.setLargeIcon(res.getDrawable(R.drawable.allow_icon))
		
		String appLabel = DBInterface.getInstance(context).getApplicationLabel(packageName);
		Log.d("PDroidAlternative","new packagename is " + packageName);
		Log.d("PDroidAlternative","app label is " + appLabel);
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
		packageDetailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK &
				Intent.FLAG_ACTIVITY_CLEAR_TASK);
		
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(AppDetailActivity.class);
		stackBuilder.addNextIntent(packageDetailIntent);
		
		PendingIntent pendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT
				);
		builder.setContentIntent(pendingIntent);
		
		Notification builtNotification = builder.build();
		builtNotification.flags = builtNotification.flags | Notification.FLAG_AUTO_CANCEL | Notification.FLAG_NO_CLEAR;
		
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(0, builtNotification);
	}
	
	private boolean havePermissionsChanged(Context context, Application oldApp, Application newApp) {
		//TODO: Add detection of permissions change
		//Maybe add a table which stores 'changed since last' info, so new permissions can be highlighted?
		if (oldApp == null) {
			//this is an error situtation
			Log.d("PDroidAlternative","oldApp == null; thus permissions have changed");
			return true;
		} else {
			/*
			 * Now to find if there are any non-matches between the two lists: have the permissions
			 * changed?
			 */
			//If only one array is null, then permissions have changed. If both are, then they haven't.
			if (oldApp.getPermissions() == null) {
				if (newApp.getPermissions() == null) {
					Log.d("PDroidAlternative","oldApp and newApp permissions are both null; permissions have not changed");
					return false;
				} else {
					Log.d("PDroidAlternative","oldApp permissions == null; newApp not; thus permissions have changed");
					return true;
				}
			} else if (newApp.getPermissions() == null) {
				Log.d("PDroidAlternative","oldApp permissions != null; newApp are; thus permissions have changed");
				return true;
			}
			
			//if the number of permissions is different, then permissions have changed
			if (oldApp.getPermissions().length != newApp.getPermissions().length) {
				Log.d("PDroidAlternative","permissions lengths differ: oldapp: " + Integer.toString(oldApp.getPermissions().length) + " newapp: " + Integer.toString(newApp.getPermissions().length));
				return true;
			}
			
			HashSet<String> currPermissions = new HashSet<String>();
			currPermissions.addAll(Arrays.asList(oldApp.getPermissions()));
			if (!currPermissions.containsAll(Arrays.asList(newApp.getPermissions()))) {
				Log.d("PDroidAlternative","containsAll returned false: thus permissions have changed");
				return true;
			}
		}
		Log.d("PDroidAlternative","Got to the end; permissions have stayed the same, it seems");
		return false;
	}
}