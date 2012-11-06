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

import android.text.SpannableStringBuilder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.digitalfeed.pdroidalternative.DBInterface;
import net.digitalfeed.pdroidalternative.Preferences;
import net.digitalfeed.pdroidalternative.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.privacy.PrivacySettingsManagerService;

public class NotificationHandler extends BroadcastReceiver {

	public NotificationHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final Context innercontext = context;
		// Check we're receiving a valid notification to handle: if not, exit
		if (!intent.getAction().equals("com.privacy.pdroid.PRIVACY_NOTIFICATION")) {
			return;
		}
		
		//read data out of the bundle
		Bundle bundle = intent.getExtras();
		final String packageName = bundle.getString("packageName");
		int uid = bundle.getInt("uid");
		byte accessMode = bundle.getByte("accessMode");
		final String dataType = bundle.getString("dataType");
        String output = bundle.getString("output");
        
        final Preferences prefs = new Preferences(context);
        
        boolean logEvent = prefs.getDoLogForPackage(packageName);
        boolean notifyEvent = prefs.getDoNotifyForPackage(packageName);
        
        if (logEvent || notifyEvent) {
        	DBInterface dbInterface = DBInterface.getInstance(context);
        	
	        if (logEvent) {
		        dbInterface.addLogEntry(packageName, uid, accessMode, dataType);
	        }
	        
	        if (notifyEvent) {
	        	//get the last time that this particular notification was presented
	        	long lastNotificationTime = prefs.getLastNotificationTime(packageName, dataType);
	        	int notificationDuration = prefs.getNotificationDuration();
	        	long currentTime = System.currentTimeMillis();
	        	
				switch (notificationDuration) {
				case Toast.LENGTH_SHORT:
					lastNotificationTime += 2000;
					break;
				case Toast.LENGTH_LONG:
					lastNotificationTime += 3500;
					break;
				}
				
	        	//Toast LONG_DELAY = 3.5s, SHORT_DELAY = 2 seconds
	        	//see http://stackoverflow.com/questions/2220560/can-an-android-toast-be-longer-than-toast-length-long
	        	if (lastNotificationTime < currentTime) {
		        	String packageLabel = dbInterface.getApplicationLabel(packageName);
		        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        	View layout = inflater.inflate(R.layout.notification_toast, null);
		        	TextView textView = (TextView) layout.findViewById(R.id.notification_toast_text);
		        	
		        	SpannableStringBuilder builder = new SpannableStringBuilder(packageLabel);
		        	builder.append(": access to ").append(dataType).append(":");
		        	Resources res = context.getResources();
		        	switch (accessMode) {
		        	case PrivacySettings.REAL:
		        		builder.append("allowed");
		        		textView.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.allow_icon), null, null, null);
		        		break;
		        	case PrivacySettings.RANDOM:
		        		builder.append("random");
		        		textView.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.random_icon), null, null, null);
		        		break;
		        	case PrivacySettings.CUSTOM:
		        		builder.append("custom");
		        		textView.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.custom_icon), null, null, null);
		        		break;
		        	case PrivacySettings.EMPTY:
		        		builder.append("denied");
		        		textView.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.deny_icon), null, null, null);
		        		break;
		        	}
		        	
		        	textView.setText(builder);
		        	final Toast toast = new Toast(context);
		        	toast.setDuration(notificationDuration);
		        	toast.setGravity(Gravity.TOP, 10, 0);
		        	toast.setView(layout);
		        	textView.setOnTouchListener(new OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							Toast.makeText(innercontext, "You touched it!", Toast.LENGTH_SHORT).show();
							//prefs.clearLastNotificationTime(packageName, dataType);
							toast.cancel();
							return true;
						}
					});
		        	
		        	prefs.setLastNotificationTime(packageName, dataType, currentTime);
		        	toast.show();
		        	
		        	//Toast.makeText(context, packageName, prefs.getNotificationDuration()).show();	
	        	}
	        }
	        dbInterface = null;
        }
	}
}
