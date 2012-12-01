package net.digitalfeed.pdroidalternative;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.Arrays;
import java.util.Comparator;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import com.google.common.base.CaseFormat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

public class BackupManager {

	//Should we be trying to handle localisation here? Seems risky!
	private static final String [] DEFAULT_BACKUP_PATH = new String [] {"pdroidmanager","backups"};
	private static final String DEFAULT_BACKUP_FILENAME_PREFIX = "pdroidmanager_backup_";
	
	public BackupManager() {
		// TODO Auto-generated constructor stub
	}
	
	public static void saveBackup(Context context, String filePath, String filename) {
		saveBackup(context, filePath, filename, false);
	}

	public static void saveBackup(Context context, String filePath, String filename, boolean force) {
		Preferences prefs = new Preferences(context);
		SecretKey key = prefs.getOrCreateSigningKey();
	}

	
	public static void loadBackup(Context context, String filePath, String filename) {
		loadBackup(context, filePath, filename, false);
	}
	
	public static void loadBackup(Context context, String filePath, String filename, boolean force) {
		//TODO: Move this to an asynctask or thread!
		Log.d("PDroidAlternative","Should try to restore file " + filename + " now");
		Preferences prefs = new Preferences(context);
		SecretKey key = prefs.getOrCreateSigningKey();
		File backupFile = new File(filePath, filename);
		File backupSignature = new File(filePath, filename + ".sig");
		if (backupFile.exists()) {
			Log.d("PDroidAlternative","Backup file exists: " + backupFile.getAbsolutePath());
			byte[] backupFileBytes;
			try {
				//read the backup file
				backupFileBytes = readFileToByteArray(backupFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("Backup file read fail");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException("Backup file read fail");
			}

			if (backupSignature.exists()) {
				Log.d("PDroidAlternative","Backup file signature exists: " + backupSignature.getAbsolutePath());
				byte[] backupSignatureBytes;
				try {
					//read the backup file
					backupSignatureBytes = readFileToByteArray(backupSignature);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("Backup file read fail");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("Backup file read fail");
				}

				try {
					Mac mac = Mac.getInstance("HmacSHA1");
					mac.init(key);
					byte [] signature = mac.doFinal(backupFileBytes);
					if (!signature.equals(backupSignatureBytes)) {
						Log.d("PDroidAlternative", "Invalid signature");
					} else {
						Log.d("PDroidAlternative", "Valid signature");
					}
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("Unknown algorith HmacSHA1");
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException("Invalid key for HmacSHA1");
				}
			} else {
				Log.d("PDroidAlternative","Backup file signature does not exist: " + backupSignature.getAbsolutePath());
			}
		} else {
			Log.d("PDroidAlternative","Backup file does not exist: " + backupFile.getAbsolutePath());
		}
	}
	
    private static File getBackupDirectory(boolean createIfMissing) throws ExternalStorageNotReadyException {
    	if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
    		//Some problem with the external storage: we could check details,
    		//but for now just provide a generic error message
    		throw new ExternalStorageNotReadyException("External storage not available"); 
    	}
    	
    	//do I need to loop like this to create each directory as a layer?
    	File dir = Environment.getExternalStorageDirectory();
    	for (int dirLayer = 0; dirLayer < DEFAULT_BACKUP_PATH.length; dirLayer++) {
    		dir = new File(dir, DEFAULT_BACKUP_PATH[dirLayer]);
    	}
    	if (dir.isDirectory()) {
    		return dir;
    	} else if (createIfMissing && dir.mkdirs()) {
    		return dir;
    	}
    	return null;
    }
	
    public static String [] getBackupFileList(File backupDirectory) {    	
    	String [] backupFiles = null;
    	if (backupDirectory != null) {
    		backupFiles = backupDirectory.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.toLowerCase().endsWith(".xml")) {
						return true;
					} else {
						return false;
					}
				}
			});
    	}
    	
    	//sort the backup files alphabetically

    	Arrays.sort(backupFiles, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return lhs.compareToIgnoreCase(rhs);
			}
		});

    	return backupFiles;
    }
    
    private static byte [] readFileToByteArray(File file) throws FileNotFoundException, IOException {
		FileInputStream fileIn = new FileInputStream(file);
		byte[] bytes; 
		{
			ByteArrayOutputStream backupFileByteStream = new ByteArrayOutputStream();
			byte[] b = new byte[1024];
			int bytesRead;
			while ((bytesRead = fileIn.read(b)) != -1) {
				   backupFileByteStream.write(b, 0, bytesRead);
			}
			bytes = backupFileByteStream.toByteArray();
		}
		return bytes;
    }
    
    
    /**
     * Shows a confirmation dialog fragment
     */
    private void showBackupDialog(String title, String body, String backupPath, String backupFilename, int action) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        BackupManager.ConfirmationDialogFragment fragment = ConfirmationDialogFragment.newInstance();
        fragment.show(ft, "dialog");
    }
    
    public static class LoadBackupDialogFragment extends DialogFragment {
    	public static CharSequence selectedBackupFilename = null;
    	public static String backupPath = null;
    	public static CharSequence [] backupFiles = null;

        public static LoadBackupDialogFragment newInstance() {
            return new LoadBackupDialogFragment();
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	selectedBackupFilename = null;
        	backupPath = null;
        	backupFiles = null;

        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        	//builder.setIcon(R.drawable.alert_dialog_icon)
        	builder.setTitle(R.string.restore_dialog_title)
            // Create the 'ok' button
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                	//if no items in the list, then simply close if OK clicked
                	if (backupFiles == null) {
                		closeDialog();
                	} else if (selectedBackupFilename != null) {
                		//an option is selected; restore the backup
                		loadBackup(getActivity(), backupPath, selectedBackupFilename.toString());
                	}
                }
            });
        	try {
        		File backupDirectory = getBackupDirectory(false);
        		if (backupDirectory != null) {
        			backupPath = backupDirectory.getAbsolutePath();
        			backupFiles = getBackupFileList(backupDirectory);
        			backupDirectory = null;
        		}
        	} catch (ExternalStorageNotReadyException e) {
	    		//something has gone wrong!!
	    		//TODO: Handle this problem in some reasonable way
        		return builder.setMessage(R.string.storage_error_body).create();
	    	}
        	
        	if (backupFiles == null || backupFiles.length == 0) {
        		//no items are available to restore - provide a message
            	return builder.setMessage(R.string.restore_no_backups).create();
        	} else {
        		selectedBackupFilename = backupFiles[0]; //the first item appears selected by default, so we should 
	        	return builder.setSingleChoiceItems(backupFiles, 0, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								selectedBackupFilename = backupFiles[which];
							}
	            		})
	            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	                @Override
	                public void onClick(DialogInterface dialog, int id) {
	                	closeDialog();
	                }
	            })
	            .create();
        	}
        }
        
        private void closeDialog() {
        	this.dismiss();
        }
    }
    
    
    public static class SaveBackupDialogFragment extends DialogFragment {
    	public static CharSequence selectedBackupFilename = null;
    	public static String backupPath = null;
    	public static CharSequence [] backupFiles = null;

        public static SaveBackupDialogFragment newInstance() {
            return new SaveBackupDialogFragment();
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	selectedBackupFilename = null;
        	backupPath = null;

        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        	//builder.setIcon(R.drawable.alert_dialog_icon)
        	builder.setTitle(R.string.backup_dialog_title)
            // Create the 'ok' button
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                	//if no items in the list, then simply close if OK clicked
                	if (backupFiles == null) {
                		closeDialog();
                	} else if (selectedBackupFilename != null) {
                		//an option is selected; restore the backup
                		saveBackup(getActivity(), backupPath, selectedBackupFilename.toString());
                	}
                }
            });
        	try {
        		File backupDirectory = getBackupDirectory(false);
        		if (backupDirectory != null) {
        			backupPath = backupDirectory.getAbsolutePath();
        			backupFiles = getBackupFileList(backupDirectory);
        			backupDirectory = null;
        		}
        	} catch (ExternalStorageNotReadyException e) {
	    		//something has gone wrong!!
	    		//TODO: Handle this problem in some reasonable way
        		return builder.setMessage(R.string.storage_error_body).create();
	    	}
        	
        	if (backupFiles == null || backupFiles.length == 0) {
        		//no items are available to restore - provide a message
            	return builder.setMessage(R.string.restore_no_backups).create();
        	} else {
        		selectedBackupFilename = backupFiles[0]; //the first item appears selected by default, so we should 
	        	return builder.setSingleChoiceItems(backupFiles, 0, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								selectedBackupFilename = backupFiles[which];
							}
	            		})
	            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	                @Override
	                public void onClick(DialogInterface dialog, int id) {
	                	closeDialog();
	                }
	            })
	            .create();
        	}
        }
        
        private void closeDialog() {
        	this.dismiss();
        }
    }
    
    
    public static class ConfirmationDialogFragment extends DialogFragment {
    	public static final int ACTION_BACKUP = 0;
    	public static final int ACTION_RESTORE = 0;
    	
    	public static final String BUNDLE_TITLE = "title";
    	public static final String BUNDLE_BODY = "body";
    	public static final String BUNDLE_BACKUP_PATH = "backupPath";
    	public static final String BUNDLE_BACKUP_FILENAME = "backupFilename";
    	public static final String BUNDLE_ACTION = "action";

    	public static ConfirmationDialogFragment newInstance(String title, String body, String backupPath, String backupFilename, int action) {
        	if (title == null || body == null) {
        		throw new InvalidParameterException("Title and body cannot be null");
        	}
            ConfirmationDialogFragment dialog = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putString(BUNDLE_TITLE, title);
            args.putString(BUNDLE_BODY, body);
            args.putString(BUNDLE_BACKUP_PATH, backupPath);
            args.putString(BUNDLE_BACKUP_FILENAME, backupPath);
            args.putInt(BUNDLE_ACTION, action);
            dialog.setArguments(args);
            return dialog;
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	final String backupPath = getArguments().getString(BUNDLE_BACKUP_PATH);
        	final String backupFilename = getArguments().getString(BUNDLE_BACKUP_FILENAME);
        	final int action = getArguments().getInt(BUNDLE_ACTION);
        	
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        	//builder.setIcon(R.drawable.alert_dialog_icon)
        	return builder.setTitle(getArguments().getString(BUNDLE_ACTION))
        	.setMessage(getArguments().getString(BUNDLE_BODY))
        	
            // Create the 'ok' button
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                	if (action == ACTION_BACKUP) {
                		saveBackup(getActivity(), backupPath, backupFilename, true);
                	} else if (action == ACTION_RESTORE) {
                		loadBackup(getActivity(), backupPath, backupFilename, true);
                	} else {
                		throw new RuntimeException("Not a valid confirmation dialog action");
                	}
                }
            })
	        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int id) {
	            	closeDialog();
	            }
	        })
	        .create();
        }
        
        private void closeDialog() {
        	this.dismiss();
        }
    }
}
