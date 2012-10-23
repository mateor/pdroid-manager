package net.digitalfeed.pdroidalternative;

import android.R.bool;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Preferences {
	
	private static final String SHARED_PREFERENCES_NAME = "net.digitalfeed.pdroidalternative";
	private static final String IS_CACHE_VALID = "isCacheValid";
	private SharedPreferences prefs;
	
	public Preferences(Context context) {
		this.prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, context.MODE_PRIVATE);
	}
	
	public Boolean getIsApplicationListCacheValid() {
		return this.prefs.getBoolean(IS_CACHE_VALID, false);
	}
	
	public void setIsApplicationListCacheValid(Boolean isValid) {
		Editor editor = this.prefs.edit();
		editor.putBoolean(IS_CACHE_VALID, isValid);
		editor.commit();
	}
	
}
