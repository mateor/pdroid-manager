package net.digitalfeed.pdroidalternative;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.util.Log;


//Thanks to IBM for a useful article on writing XML: http://www.ibm.com/developerworks/library/x-androidxml/index.html
public class WriteBackupXmlTask extends AsyncTask<Void, Void, Integer> {
	public static final int BACKUP_WRITE_SUCCESS = 0;
	public static final int BACKUP_WRITE_FAIL_SIGNING = 1;
	public static final int BACKUP_WRITE_FAIL_WRITING = 2;
	public static final int BACKUP_WRITE_FAIL_OTHER = 3;
	
	Context context;
	final String path;
	final String filename;
	final SecretKey key;
	final IAsyncTaskCallback<Integer> callback;
	
	public WriteBackupXmlTask(Context context, String path, String filename, IAsyncTaskCallback<Integer> callback) {
		if (context == null || path == null || filename == null) {
			throw new InvalidParameterException("Context, path and filename must be provided to write backup");
		}
		Log.d("PDroidAlternative","Starting to write backup");
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
			return BACKUP_WRITE_FAIL_OTHER;
		}

		Document document = documentBuilder.newDocument(); 
		Element root = document.createElement(PreferencesListFragment.BACKUP_XML_ROOT_NODE);
		document.appendChild(root);
		
		PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager)context.getSystemService("privacy");
		PrivacySettings privacySettings;
		
		DBInterface dbinterface = DBInterface.getInstance(context);
		SQLiteDatabase db = dbinterface.getDBHelper().getReadableDatabase();
		AppQueryBuilder queryBuilder = new AppQueryBuilder();
		queryBuilder.addColumns(AppQueryBuilder.COLUMN_TYPE_PACKAGENAME);
		Cursor cursor = queryBuilder.doQuery(db);
		String packageName;
		
		PermissionSettingHelper psh = new PermissionSettingHelper();
		
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			Log.d("PDroidAlternative","Number of apps to backup: " + Integer.toString(cursor.getCount()));
			int packageNameColumn = cursor.getColumnIndex(DBInterface.ApplicationTable.COLUMN_NAME_PACKAGENAME);

			do {
	    		packageName = cursor.getString(packageNameColumn);
	    		Log.d("PDroidAlternative","Backing up: " + packageName);
	    		privacySettings = privacySettingsManager.getSettings(packageName);
	    		if (privacySettings != null) {
		    		Element appNode = psh.getPrivacySettingsXml(db, privacySettings, document);
		    		if (appNode == null) {
		    			//TODO: error handling
		    			return BACKUP_WRITE_FAIL_OTHER;
		    			//throw new RuntimeException("Something went wrong building the appNode");
		    		}
		    		root.appendChild(appNode);
	    		}
	    	} while (cursor.moveToNext());

	    	cursor.close();
		}

		try {
			File file = new File(this.path, this.filename);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
	        Properties properties = new Properties();
	        properties.setProperty( OutputKeys.INDENT, "yes" );
	        properties.setProperty( OutputKeys.METHOD, "xml" );
	        properties.setProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );
	        properties.setProperty( OutputKeys.VERSION, "1.0" );
	        properties.setProperty( OutputKeys.ENCODING, "UTF-8" );
	        transformer.setOutputProperties(properties);
	
	        DOMSource domSource = new DOMSource(document.getDocumentElement());
	        ByteArrayOutputStream output = new ByteArrayOutputStream();
	        StreamResult result = new StreamResult(output);
            transformer.transform(domSource, result);
            byte [] backupFileBytes = output.toByteArray();
            
            //generate the signature for the backup file
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(this.key);
			byte [] signature = mac.doFinal(backupFileBytes);
            
            //write the backup file
            FileOutputStream outFile = new FileOutputStream(file);
            outFile.write(backupFileBytes);
            outFile.close();
            output.close();
            
            //write the signature file
            outFile = new FileOutputStream(file + ".sig");
            outFile.write(signature);
            outFile.close();
            return BACKUP_WRITE_SUCCESS;
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_WRITE_FAIL_OTHER;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_WRITE_FAIL_WRITING;
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_WRITE_FAIL_OTHER;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_WRITE_FAIL_OTHER;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_WRITE_FAIL_SIGNING;
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return BACKUP_WRITE_FAIL_SIGNING;
		}
	}
	
	
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		callback.asyncTaskComplete(result);
	}
}