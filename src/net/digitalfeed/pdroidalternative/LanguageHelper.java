package net.digitalfeed.pdroidalternative;

import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class LanguageHelper {

	public static void updateLanguageIfRequired(Context context) {
		Preferences prefs = new Preferences(context);
		//if a language has been forced, we will always use that language
		String useLanguage = prefs.getForcedLanguage();
		if (useLanguage != null) {
			Log.d("PDroidAlternative","Forced Language " + useLanguage); 
		}
		//useLanguage = "en";
		if (useLanguage == null) {
			//Language isn't being force: get the 'real' language
			//Check if Locale has changed. If so, then the setting names etc in the database need to be replaced
			useLanguage = context.getResources().getConfiguration().locale.getLanguage();
			Log.d("PDroidAlternative","No forced Language. Use real language " + useLanguage);
		} else {
			Locale forcedLocale = new Locale(useLanguage); 
		    Locale.setDefault(forcedLocale);
		    
			Configuration config = new Configuration();
	        config.locale = forcedLocale;

	        context.getResources().updateConfiguration(config, context.getResources()
	                .getDisplayMetrics());

	        forcedLocale = null;
	        config = null;
		}
		
		if (!useLanguage.equals(prefs.getLastRunLanguage())) {
			prefs.setLastRunLanguage(useLanguage);
			DBInterface dbinterface = DBInterface.getInstance(context);
			SQLiteDatabase db = dbinterface.getDBHelper().getWritableDatabase();
			dbinterface.getDBHelper().loadDefaultData(db);
		}
	}
}
