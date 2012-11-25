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
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
import android.graphics.Typeface;
import android.os.Bundle;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;

public class PrivacyNotificationHandler extends BroadcastReceiver {

	public PrivacyNotificationHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Check we're receiving a valid notification to handle: if not, exit
		if (!intent.getAction().equals(PrivacySettingsManager.ACTION_PRIVACY_NOTIFICATION)) {
			Log.d("PDroidAlternative","NotificationHandler: Notification has incorrect action: " + intent.getAction());
			return;
		}
		
		//read data out of the bundle
		Bundle bundle = intent.getExtras();
		final String packageName = bundle.getString("packageName");
		int uid = bundle.getInt("uid");
		byte accessMode = bundle.getByte("accessMode");
		final String dataType = bundle.getString("dataType");
        //String output = bundle.getString("output");
        
		Log.d("PDroidAlternative","NotificationHandler: Notification for: " + packageName + ":" + dataType);
		
        final Preferences prefs = new Preferences(context);
        
        boolean logEvent = prefs.getDoLogForPackage(packageName);
        boolean notifyEvent = prefs.getDoNotifyForPackage(packageName);
        
        if (logEvent || notifyEvent) {        	
        	DBInterface dbInterface = DBInterface.getInstance(context);
        	
	        if (logEvent) {
	        	Log.d("PDroidAlternative","NotificationHandler: Log setting on for " + packageName);
		        dbInterface.addLogEntry(packageName, uid, accessMode, dataType);
	        }
	        
	        if (notifyEvent) {
	        	Log.d("PDroidAlternative","NotificationHandler: Notify setting on for " + packageName);
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
	        		Log.d("PDroidAlternative","NotificationHandler: Minimum inter-toast time exceeded: " + packageName + ":" + dataType);
		        	String packageLabel = dbInterface.getApplicationLabel(packageName);
		        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        	View layout = inflater.inflate(R.layout.notification_toast, null);
		        	TextView textView = (TextView) layout.findViewById(R.id.notification_toast_text);
		        	ImageView imageView = (ImageView) layout.findViewById(R.id.notification_toast_image);

		        	int startOffset = 0;
		        	
		        	SpannableStringBuilder builder = new SpannableStringBuilder(packageLabel);
		        	Resources res = context.getResources();
		        	
		        	builder.setSpan(new StyleSpan(Typeface.BOLD), 0, packageLabel.length(), 0);
		        	builder.setSpan(new ForegroundColorSpan(res.getColor(R.color.toast_text_highlight)), 0, packageLabel.length(), 0);
		        	builder.append(" ").append(res.getString(R.string.notification_toast_text1)).append(" ");
		        	startOffset = builder.length();

		        	//thanks to AndDev for how to get the identifier: http://www.anddev.org/viewtopic.php?p=17846
		        	//Note that Google recommend against using getIdentifier for performance reasons, but here I don't see we have a choice
		        	int notificationStringId = res.getIdentifier("access_notification_" + dataType, "string", context.getPackageName());
		        	String dataTypeMessage;
		        	if (notificationStringId != 0) {
		        		dataTypeMessage = res.getString(notificationStringId);
		        	} else {
		        		dataTypeMessage = dataType;
		        	}
		        	
		        	builder.append(dataTypeMessage).append(" ").append(res.getString(R.string.notification_toast_text2)).append(" ");
		        	builder.setSpan(new StyleSpan(Typeface.BOLD), startOffset, startOffset + dataTypeMessage.length(), 0);
		        	builder.setSpan(new ForegroundColorSpan(res.getColor(R.color.toast_text_highlight)), startOffset, startOffset + dataTypeMessage.length(), 0);
		        	switch (accessMode) {
		        	case PrivacySettings.REAL:
		        		builder.append(res.getString(R.string.notification_toast_allowed_text));
		        		imageView.setImageDrawable(res.getDrawable(R.drawable.allow_icon));
		        		break;
		        	case PrivacySettings.RANDOM:
		        		builder.append(res.getString(R.string.notification_toast_random_text));
		        		imageView.setImageDrawable(res.getDrawable(R.drawable.random_icon));
		        		break;
		        	case PrivacySettings.CUSTOM:
		        		builder.append(res.getString(R.string.notification_toast_custom_text));
		        		imageView.setImageDrawable(res.getDrawable(R.drawable.custom_icon));
		        		break;
		        	case PrivacySettings.EMPTY:
		        		builder.append(res.getString(R.string.notification_toast_denied_text));
		        		imageView.setImageDrawable(res.getDrawable(R.drawable.deny_icon));
		        		break;
		        	}
		        	
		        	textView.setText(builder);
		        	final Toast toast = new Toast(context);
		        	toast.setDuration(notificationDuration);
		        	prefs.setLastNotificationTime(packageName, dataType, currentTime);
		        	toast.setView(layout);
		        	toast.show();
		        	
		        	//Toast.makeText(context, packageName, prefs.getNotificationDuration()).show();	
	        	} else {
	        		Log.d("PDroidAlternative","NotificationHandler: Minimum inter-toast time not reached: " + packageName + ":" + dataType);
	        	}
	        }
	        dbInterface = null;
        }
	}
}
