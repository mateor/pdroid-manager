package net.digitalfeed.pdroidalternative;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class PreferencesListFragment extends ListFragment {

	private static final int ITEM_TYPE_HEADING = 0;
	private static final int ITEM_TYPE_CONTENT = 1;
	private static final int ITEM_TYPE_COUNT = 2;

	private static final int BACKUP_VALIDATION_OK = 0;
	private static final int BACKUP_VALIDATION_SIGNATUREERROR = 1;
	private static final int BACKUP_VALIDATION_SIGNATUREMISSING= 2;
	private static final int BACKUP_VALIDATION_INVALID = 3;
	private static final int BACKUP_VALIDATION_MISSING_OR_ERROR = 4;
	
	private static final String PDROIDMANAGER_XDA_THREAD_URL = "http://forum.xda-developers.com/showthread.php?p=34190204";
	
	//Should we be trying to handle localisation here? Seems risky!
	private static final String [] DEFAULT_BACKUP_PATH = new String [] {"pdroidmanager","backups"};
	private static final String DEFAULT_BACKUP_FILENAME_PREFIX = "pdroidmanager_backup_";

	
	Context context;
	Preferences prefs;
	PreferencesAdapter adapter;
	private final List<BasePreference> preferences = new ArrayList<BasePreference>(10);
	
	public interface ListItemClickListener {
		public void onListItemClick (ListView l, View v, int position, long id);
	}
	
	public PreferencesListFragment() {}
	
	private static class BasePreference {
		public String title;
		public BasePreference(String title) {
			this.title = title;
		}
	}
	private static class PreferenceHeader extends BasePreference {
		public PreferenceHeader(String title) {
			super(title);
		}}
	private static class Preference extends BasePreference {
		public String summary;
		public Drawable icon;
		public ListItemClickListener clickListener;
		
		public Preference(String title, String summary, Drawable icon, ListItemClickListener clickListener) {
			super(title);
			this.summary = summary;
			this.icon = icon;
			this.clickListener = clickListener;
		}
		
		public Preference(String title, String summary) {
			this(title, summary, null, null);
		}
	}
	
	/**
	 * Adapter for the 'preferences' list - i.e. the configuration options for the app.
	 * Note that many of those are not actually preferences as such - i.e. not
	 * shared preferences. The goal is to mimic that look for the settings screen, though.
	 * 
	 * @author smorgan

	 */
	public class PreferencesAdapter extends BaseAdapter {
		
		@Override
		public int getCount() {
			return preferences.size();
		}

		@Override
		public Object getItem(int position) {
			return preferences.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return ITEM_TYPE_COUNT;
		}

		@Override
		public int getItemViewType(int position) {
			return (preferences.get(position) instanceof PreferenceHeader) ? ITEM_TYPE_HEADING
					: ITEM_TYPE_CONTENT;
		}

		@Override
		public boolean isEnabled(int position) {
			// A separator cannot be clicked
			if (preferences.get(position) instanceof PreferenceHeader) {
				return false;
			}
			if (((Preference)preferences.get(position)).clickListener == null) {
				return false;
			}
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final int type = getItemViewType(position);

			// First, let's create a new convertView if needed. You can also
			// create a ViewHolder to speed up changes if you want ;)
			if (convertView == null) {
				convertView = LayoutInflater
						.from(context)
						.inflate(
								type == ITEM_TYPE_HEADING ? R.layout.preference_heading
										: R.layout.preference_item, parent,
								false);
			}

			// We can now fill the list item view with the appropriate data.
			if (type == ITEM_TYPE_HEADING) {
				final PreferenceHeader preference = (PreferenceHeader) getItem(position);
				((TextView) convertView.findViewById(R.id.title)).setText(preference.title);
			} else {
				final Preference preference = (Preference) getItem(position);
				((TextView) convertView.findViewById(R.id.title)).setText(preference.title);
				((TextView) convertView.findViewById(R.id.summary)).setText(preference.summary);
			}

			return convertView;
		}
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int position, long id) {
		if (preferences.get(position) instanceof Preference
				&& ((Preference)preferences.get(position)).clickListener != null) {
			((Preference)preferences.get(position)).clickListener.onListItemClick(l, v, position, id);
			return;
		}
	}
	
	@Override
	public void onAttach (Activity activity) {
		super.onAttach(activity);
		this.context = activity;
		this.prefs = new Preferences(context);
		
		preferences.add(
				new PreferenceHeader(getString(R.string.preferences_heading_about))
				);
		preferences.add(
				new Preference(getString(R.string.preferences_about_title),
						getString(R.string.preferences_about_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showInformationDialog(
										getString(R.string.about_dialog_title),
										getString(R.string.about_dialog_body));
							}
						})
				);
		preferences.add(
				new Preference(getString(R.string.preferences_credits_title),
						getString(R.string.preferences_credits_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showInformationDialog(
										getString(R.string.credits_dialog_title),
										getString(R.string.credits_dialog_body));
							}
						})
				);
		preferences.add(
				new Preference(
						getString(R.string.preferences_openxdathread_title),
						getString(R.string.preferences_openxdathread_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								openXDA();
							}
						})
				);
		preferences.add(
				new PreferenceHeader(getString(R.string.preferences_heading_language))
				);
		preferences.add(
				new Preference(
						getString(R.string.preferences_switchlanguage_title),
						getString(R.string.preferences_switchlanguage_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showLanguageSelector();
							}
						})
				);
		//if (this.prefs.getForcedLanguage() != null) {
			preferences.add(
					new Preference(
							getString(R.string.preferences_usephonelocale_title),
							getString(R.string.preferences_usephonelocale_summary),
							null,
							new ListItemClickListener() {
								@Override
								public void onListItemClick (ListView l, View v, int position, long id) {
									prefs.clearForcedLanguage();
									LanguageHelper.updateLanguageIfRequired(context);
									// TODO: The option to 'clear forced language' should be removed
									// when this is selected, or at least disabled
								}
							})
					);

		//}
		preferences.add(
				new PreferenceHeader(getString(R.string.preferences_heading_backuprestore))
				);
		preferences.add(
				new Preference(
						getString(R.string.preferences_backup_title),
						getString(R.string.preferences_backup_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showBackupDialog();
							}
						})
				);
		preferences.add(
				new Preference(
						getString(R.string.preferences_restore_title),
						getString(R.string.preferences_restore_summary),
						null,
						new ListItemClickListener() {
							@Override
							public void onListItemClick (ListView l, View v, int position, long id) {
								showRestoreDialog();
							}
						})
				);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        this.adapter = new PreferencesAdapter(); 
		setListAdapter(adapter);
    }
    
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.preferences_list_view, container);
	}

    @Override
    public void onStart() {
    	super.onStart();
    }
    
    
    /**
     * Open the PDroid Manager thread in XDA-developers
     */
    private void openXDA() {
    	final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(PDROIDMANAGER_XDA_THREAD_URL));
    	context.startActivity(intent);
    }
    
    
    /**
     * Displays a generic 'information' dialog
     */
    private void showInformationDialog(String title, String body) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }

        // Create and show the dialog.
        DialogFragment newFragment = InformationDialog.newInstance(title, body);
        newFragment.show(ft, "dialog");
    }
    
    
    /**
     * Shows the language selector dialog
     */
    private void showLanguageSelector() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }

        // Create and show the dialog.
        SelectLanguageDialogFragment newFragment = SelectLanguageDialogFragment.newInstance();        
        newFragment.show(ft, "dialog");
    }
    
    
    /**
     * Shows a list of backup files which can be restored and allows the user to select
     */
    private void showRestoreDialog() {    	
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }

        // Create and show the dialog.
        LoadBackupDialogFragment fragment = LoadBackupDialogFragment.newInstance(new DialogCallbackWithFilename() {
			@Override
			public void onDialogSuccess(String path, String filename) {
				final String thisPath = path;
				final String thisFilename = filename;

				Log.d("PDroidAlternative","OnDialogSuccess Callback from load dialog with " + path + " " + filename);
        		//an option is selected; restore the backup
				switch (validateBackupForRestoring(getActivity(), path, filename)) {
				case BACKUP_VALIDATION_OK:
					restoreFromBackup(thisPath, thisFilename);
					break;
				case BACKUP_VALIDATION_SIGNATUREERROR:
        			showConfirmationDialog(
        					getString(R.string.restore_dialog_title),
        					getString(R.string.restore_invalid_signature),
        					new DialogCallback() {
								@Override
								public void onDialogSuccess() {
									restoreFromBackup(thisPath, thisFilename);	
								}
		        			}
        				);
        			break;
				case BACKUP_VALIDATION_INVALID:
					
				}
			}});        
        fragment.show(ft, "dialog");
    }
    
    /**
     * Show the interface to set the filename of the backup file to write
     */
    private void showBackupDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }

        // Create and show the dialog.
        SaveBackupDialogFragment newFragment = SaveBackupDialogFragment.newInstance(new DialogCallbackWithFilename() {
			@Override
			public void onDialogSuccess(String path, String filename) {
				// TODO Auto-generated method stub
				Log.d("PDroidAlternative","OnDialogSuccess Callback from load dialog with " + path + " " + filename);
			}
        });        

        newFragment.show(ft, "dialog");
    }
    
    
    /**
     * General purpose information presentation dialog. Displays content and an 'ok' button to close.
     * @author smorgan
     *
     */
    public static class InformationDialog extends DialogFragment {
    	
        static InformationDialog newInstance(String title, String body) {
        	if (title == null || body == null) {
        		throw new InvalidParameterException("Title and body cannot be null");
        	}
            InformationDialog infoDialog = new InformationDialog();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("body", body);
            infoDialog.setArguments(args);
            return infoDialog;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	getDialog().setTitle(getArguments().getString("title"));
        	getDialog().setCanceledOnTouchOutside(true); 
        	//getDialog().setTitle(getString(R.string.credits_dialog_title));
            View view = inflater.inflate(R.layout.preferences_credits_dialog, container, false);
            Button closeButton = (Button)view.findViewById(R.id.credits_dialog_close_button);
            closeButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					closeDialog();
				}
			});
            
            TextView body = (TextView)view.findViewById(R.id.credits_dialog_body);
            body.setText(Html.fromHtml(getArguments().getString("body")));
            //Thank you stackoverflow: need to do the setMovementMethod for links to work in TextView.
            //http://stackoverflow.com/questions/2734270/how-do-i-make-links-in-a-textview-clickable
            body.setMovementMethod(LinkMovementMethod.getInstance());

            return view;
        }
        
        
        private void closeDialog() {
        	this.dismiss();
        }
    }
    

    
    /**
     * Dialog to allow user to select their preferred language from a list of available languages
     * @author smorgan
     *
     */
    
    public static class SelectLanguageDialogFragment extends DialogFragment {

        public static SelectLanguageDialogFragment newInstance() {
            return new SelectLanguageDialogFragment();
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    //.setIcon(R.drawable.alert_dialog_icon)
                    //.setTitle(title)
        	
        	builder.setItems(R.array.language_titles, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							forceLanguage(which);
							LanguageHelper.updateLanguageIfRequired(getActivity());
							closeDialog();
						}
            		});
            return builder.create();
        }
        
        private void forceLanguage(int languageIndex) {
        	Preferences prefs = new Preferences(getActivity());
        	String [] languageCodes = getResources().getStringArray(R.array.language_codes);
        	prefs.setForcedLanguage(languageCodes[languageIndex]);
        }

        
        private void closeDialog() {
        	this.dismiss();
        }
    }

    
    
    
    
    
    
    /**
     * Callback used to provide information back from the static alert dialogs
     * with path and filename details
     * @author smorgan
     *
     */
    public interface DialogCallbackWithFilename {
    	void onDialogSuccess(String path, String filename);
    	//void onDialogCancel();
    }
    
    
    /**
     * Callback used to provide information back from the static alert dialogs
     * without path and filename details
     * @author smorgan
     *
     */
    public interface DialogCallback {
    	void onDialogSuccess();
    	//void onDialogCancel();
    }

    
    /**
     * General purpose confirmation dialog 
     */
    private void showConfirmationDialog(String title, String body, DialogCallback dialogCallback) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }

        // Create and show the dialog.
        ConfirmationDialogFragment newFragment = ConfirmationDialogFragment.newInstance(title, body, dialogCallback);
        newFragment.show(ft, "dialog");
    }
    

    
    public static class LoadBackupDialogFragment extends DialogFragment {
    	public static CharSequence selectedBackupFilename = null;
    	public static String backupPath = null;
    	public static CharSequence [] backupFiles = null;
    	public static DialogCallbackWithFilename callback = null;

        public static LoadBackupDialogFragment newInstance(DialogCallbackWithFilename dialogCallback) {
        	callback = dialogCallback;
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
                		closeDialog();
                		callback.onDialogSuccess(backupPath, selectedBackupFilename.toString());
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
        		//Problem with accessing external storage: present a message to the user
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
    	public static DialogCallbackWithFilename callback;

        public static SaveBackupDialogFragment newInstance(DialogCallbackWithFilename dialogCallback) {
        	callback = dialogCallback;
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
        		//Problem with accessing external storage: present a message to the user
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
    	private static DialogCallback callback;
    	
    	public static final String BUNDLE_TITLE = "title";
    	public static final String BUNDLE_BODY = "body";

    	public static ConfirmationDialogFragment newInstance(String title, String body, DialogCallback dialogCallback) {
        	if (title == null || body == null) {
        		throw new InvalidParameterException("Title and body cannot be null");
        	}
            ConfirmationDialogFragment dialog = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putString(BUNDLE_TITLE, title);
            args.putString(BUNDLE_BODY, body);
            dialog.setArguments(args);
            callback = dialogCallback;
            return dialog;
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        	//builder.setIcon(R.drawable.alert_dialog_icon)
        	return builder.setTitle(getArguments().getString(BUNDLE_TITLE))
        	.setMessage(getArguments().getString(BUNDLE_BODY))
        	
            // Create the 'ok' button
            .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                	callback.onDialogSuccess();
                	closeDialog();
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
    

    /**
     * Backup save/loading code is below here. I'd like to have it in its own class, but due to variable access requests between classes, callbacks, etc
     * I have temporarily merged it
     */
    

	public static void saveBackup(Context context, String filePath, String filename) {
		Preferences prefs = new Preferences(context);
		SecretKey key = prefs.getOrCreateSigningKey();
	}

	/**
	 * Checks that a backup file is valid for restoration, and either returns true if the backup
	 * is fine to restore, otherwise throws an exception
	 * @param context
	 * @param filePath
	 * @param filename
	 */
	public static int validateBackupForRestoring(Context context, String filePath, String filename) {
		//TODO: Move this to an asynctask or thread!
		Log.d("PDroidAlternative","Validating file " + filename + " for restoration");
		Preferences prefs = new Preferences(context);
		SecretKey key = prefs.getOrCreateSigningKey();
		File backupFile = new File(filePath, filename);
		File backupSignature = new File(filePath, filename + ".sig");
		if (!backupFile.exists()) {
			return BACKUP_VALIDATION_MISSING_OR_ERROR;
		}
		
		Log.d("PDroidAlternative","Backup file exists: " + backupFile.getAbsolutePath());
		byte[] backupFileBytes;
		try {
			//read the backup file
			backupFileBytes = readFileToByteArray(backupFile);
		} catch (FileNotFoundException e) {
			return BACKUP_VALIDATION_MISSING_OR_ERROR;
		} catch (IOException e) {
			return BACKUP_VALIDATION_MISSING_OR_ERROR;
		}

		if (!backupSignature.exists()) {
			return BACKUP_VALIDATION_SIGNATUREMISSING;
		}
		
		Log.d("PDroidAlternative","Backup file signature exists: " + backupSignature.getAbsolutePath());
		
		byte[] backupSignatureBytes;
		try {
			//read the backup file
			backupSignatureBytes = readFileToByteArray(backupSignature);
		} catch (FileNotFoundException e) {
			return BACKUP_VALIDATION_SIGNATUREERROR;
		} catch (IOException e) {
			return BACKUP_VALIDATION_SIGNATUREERROR;
		}

		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
			byte [] signature = mac.doFinal(backupFileBytes);
			if (!signature.equals(backupSignatureBytes)) {
				return BACKUP_VALIDATION_SIGNATUREERROR;
			} else {
				return BACKUP_VALIDATION_OK;
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
	}
	
	private static void restoreFromBackup(String path, String filename) {
/*		File file = new File(path, filename);
		FileInputStream fileIn = new FileInputStream(file);
		XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(fileIn, null);
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, ns, "pdroidmanagerbackup");
        List entries = new ArrayList();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("entry")) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }  
        */
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
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] b = new byte[1024];
		int bytesRead;
		while ((bytesRead = fileIn.read(b)) != -1) {
			   outStream.write(b, 0, bytesRead);
		}
		bytes = outStream.toByteArray();
		outStream.close();
		fileIn.close();
		return bytes;
    }
}
