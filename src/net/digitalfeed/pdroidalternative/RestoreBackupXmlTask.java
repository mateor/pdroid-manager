package net.digitalfeed.pdroidalternative;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.util.Log;


//Thanks to IBM for a useful article on writing XML: http://www.ibm.com/developerworks/library/x-androidxml/index.html
public class RestoreBackupXmlTask extends AsyncTask<Void, Void, Integer> {
	public static final int BACKUP_RESTORE_SUCCESS = 0;
	public static final int BACKUP_RESTORE_FAIL_READING = 1;
	public static final int BACKUP_RESTORE_FAIL_INVALID = 2;
	public static final int BACKUP_RESTORE_FAIL_OTHER = 3;
	
	Context context;
	final String path;
	final String filename;
	final SecretKey key;
	final IAsyncTaskCallback<Integer> callback;
	
	public RestoreBackupXmlTask(Context context, String path, String filename, IAsyncTaskCallback<Integer> callback) {
		if (context == null || path == null || filename == null) {
			throw new InvalidParameterException("Context, path and filename must be provided to write backup");
		}
		
		this.context = context;
		this.path = path;
		this.filename = filename;
		Preferences prefs = new Preferences(context);
		this.key = prefs.getOrCreateSigningKey();
		prefs = null;
		this.callback = callback;
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		DocumentBuilder documentBuilder;
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Don't bomb the whole program - maybe a toast or something instead?
			e.printStackTrace();
			//throw new RuntimeException("Error occurred while creating a new document builder for XML backup generation");
			return BACKUP_RESTORE_FAIL_OTHER;
		}

		File file = new File(this.path, this.filename);
		Document document;
		try {
			document = documentBuilder.parse(file);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_RESTORE_FAIL_INVALID;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_RESTORE_FAIL_READING;
		}
				
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		PrivacySettings privacySettings;
		
		PackageManager pkgMgr = context.getPackageManager();
		PackageInfo pkgInfo;
		
		DBInterface dbinterface = DBInterface.getInstance(context);
		SQLiteDatabase db = dbinterface.getDBHelper().getReadableDatabase();
		PermissionSettingHelper psh = new PermissionSettingHelper();
		
		String packageName;
		int uid;
		
		NodeList apps = document.getElementsByTagName(PreferencesListFragment.BACKUP_XML_ROOT_NODE);
		if (apps.getLength() != 1) {
			return BACKUP_RESTORE_FAIL_INVALID;
		}
		Element rootNode = (Element)apps.item(0);
		apps = rootNode.getChildNodes();
		
		for (int appNum = 0; appNum < apps.getLength(); appNum++) {
			Node node = apps.item(appNum);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element appElement = (Element)node;
				packageName = appElement.getAttribute(PreferencesListFragment.BACKUP_XML_APP_NODE_PACKAGENAME_ATTRIBUTE);

				privacySettings = privacySettingsManager.getSettings(packageName);
				try {
					if (privacySettings == null) {
						if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Settings not found for " + packageName + ": using pkgMgr");
						pkgInfo = pkgMgr.getPackageInfo(packageName, 0);
						uid = pkgInfo.applicationInfo.uid;
						privacySettings = new PrivacySettings(null, packageName, uid);
					}
					
					if (privacySettings != null) {
						psh.setPrivacySettingsFromXml(db, privacySettings, appElement);
						privacySettingsManager.saveSettings(privacySettings);
					}
					
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					if(GlobalConstants.LOG_DEBUG) Log.d(GlobalConstants.LOG_TAG, "Could not find package " + packageName);
				}
			}
		}
		
		Preferences prefs = new Preferences(context);
		prefs.setIsApplicationListCacheValid(false);
		prefs = null;
		
		return BACKUP_RESTORE_SUCCESS;
	}
	
	
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		callback.asyncTaskComplete(result);
	}
}