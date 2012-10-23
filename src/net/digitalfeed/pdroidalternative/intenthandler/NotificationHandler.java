package net.digitalfeed.pdroidalternative.intenthandler;

import javax.xml.datatype.Duration;

import android.privacy.PrivacySettings; 
import android.widget.Toast;
import net.digitalfeed.pdroidalternative.DBInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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
        
        DBInterface.getInstance(context).addLogEntry(packageName, uid, accessMode, dataType);
        
        Toast msgToast = new Toast(context);
        msgToast.setDuration(Toast.LENGTH_SHORT);
        msgToast.setText((CharSequence)packageName);
        //SQLiteDatabase write_db = DBInterface.getInstance(context).getDBHelper().getWritableDatabase();
        //ContentValues logEntry = DBInterface.ApplicationLogTable.getContentValues()
        //write_db.insert(DBInterface.ApplicationLogTable.TABLE_NAME, null, )
	}

}
