package net.digitalfeed.pdroidalternative;

import java.security.InvalidParameterException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class DialogHelper {

	private static ProgressDialog progDialog;
	
	private DialogHelper() {}
	
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
     * Helper to show a non-cancellable spinner progress dialog
     * 
     * @param title  Title for the progress dialog (or null for none)
     * @param message  Message for the progress dialog (or null for none)
     * @param type  ProgressDialog.x for the type of dialog to be displayed
     */	
	static void showProgressDialog(Context context, String title, String message, int type) {
		dismissProgressDialog();
		progDialog = new ProgressDialog(context);
		progDialog.setProgressStyle(type);
		if (title != null) {
			progDialog.setTitle(title);
		}
		if (message != null) {
			progDialog.setMessage(message);
		}
    	progDialog.setCancelable(false);
    	progDialog.show();
	}
	
	
    /**
     * Helper to show a non-cancellable spinner progress dialog
     * 
     * @param title  Title for the progress dialog (or null for none)
     * @param message  Message for the progress dialog (or null for none)
     */
	static void showProgressDialog(Context context, String title, String message) {
		showProgressDialog(context, title, message, ProgressDialog.STYLE_SPINNER);
	}
	
	
	/**
	 * Helper to close a dialog if one is open
	 */
	static void dismissProgressDialog() {
		if (progDialog != null) {
			progDialog.dismiss();
		}
	}
	
	static void updateProgressDialog(int currentValue, int maxValue) {
	    if(GlobalConstants.LOG_FUNCTION_TRACE) Log.d(GlobalConstants.LOG_TAG, "DialogHelper:updateProgressDialog");
	    if (progDialog != null) {
	    	if (progDialog.isShowing()) {
	    		progDialog.setProgress(currentValue);
	    	} else {
	    		progDialog.setMax(maxValue);
	    		progDialog.setProgress(currentValue);
	    		progDialog.show();
			}
	    }
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
     * General purpose confirmation dialog, with 'yes' and 'no' options
     * @author smorgan
     *
     */
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
            .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                	callback.onDialogSuccess();
                	closeDialog();
                }
            })
	        .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
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
