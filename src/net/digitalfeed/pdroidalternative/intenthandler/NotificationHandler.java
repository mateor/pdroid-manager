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

import android.widget.Toast;
import net.digitalfeed.pdroidalternative.DBInterface;
import net.digitalfeed.pdroidalternative.Preferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class NotificationHandler extends BroadcastReceiver {

	public NotificationHandler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		
		// Check we're receiving a valid notification to handle: if not, exit
		if (!intent.getAction().equals("com.privacy.pdroid.PRIVACY_NOTIFICATION")) {
			return;
		}
		
		//read data out of the bundle
		Bundle bundle = intent.getExtras();
		String packageName = bundle.getString("packageName");
		int uid = bundle.getInt("uid");
		byte accessMode = bundle.getByte("accessMode");
		String dataType = bundle.getString("dataType");
        String output = bundle.getString("output");
        
        Preferences prefs = new Preferences(context);
        
        if (prefs.getDoLogForPackage(packageName)) {
	        DBInterface dbInterface = DBInterface.getInstance(context);
	        dbInterface.addLogEntry(packageName, uid, accessMode, dataType);
	        dbInterface = null;
        }
        
        if (prefs.getDoNotifyForPackage(packageName)) {
	        Toast msgToast = new Toast(context);
	        msgToast.setDuration(prefs.getNotificationDuration());
	        msgToast.setText((CharSequence)packageName);
        }
        prefs = null;
	}
}
