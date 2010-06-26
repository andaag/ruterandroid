package com.neuron.trafikanten.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.tasks.handlers.ReturnCoordinatesHandler;

public class SearchAddressTask implements GenericTask {
	private static final String TAG = "Trafikanten-SearchAddressTask";
	
	private Activity activity;
	private List<Address> addresses;
	ReturnCoordinatesHandler handler;
	private Dialog dialog; 
	private GoogleAnalyticsTracker tracker;
	    
	
	public SearchAddressTask(Activity activity, GoogleAnalyticsTracker tracker, ReturnCoordinatesHandler handler) 
	{
		this.activity = activity;
		this.tracker = tracker;
		tracker.trackPageView("/task/searchAddress");
		this.handler = handler;
		showAddressField();
	}
	
	/*
	 * Show the address field dialog and wait for input
	 */
	private void showAddressField() {
		dialog = new Dialog(activity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_searchaddress);
		
		final TextView message = (TextView) dialog.findViewById(R.id.message);
		message.setText(R.string.searchAddressTask);

		final EditText searchEdit = (EditText) dialog.findViewById(R.id.search);
		searchEdit.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                
                switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                	dialog.dismiss();
                	geoMap(searchEdit.getText().toString());
                	return true;
                }
				return false;
			}
		});
		
		/*
		 * Handler onCancel
		 */
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				handler.onCanceled();				
			}
		});
		
		dialog.show();
	}

	private void showAddressSelection() {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    builder.setTitle(R.string.searchAddressTask);
	    
	    
	    final List<Address> duplicateAddresses = new ArrayList<Address>();
	    
	    /*
	     * First take all addresses, convert them into strings
	     * remove duplicates so addressStrings index matches address index
	     */
	    ArrayList<String> addressStrings = new ArrayList<String>();
	    for(Address address : addresses) {
	    	Log.d(TAG,"Got address " + address.toString());
    		String result = address.getAddressLine(0);
    		for (int i = 1; i <= address.getMaxAddressLineIndex(); i++) {
    			result = result + ", " + address.getAddressLine(i);
    		}
    		
    		if (!addressStrings.contains(result)) {
    			addressStrings.add(result);
    		} else {
    			duplicateAddresses.add(address);
    			Log.d(TAG, "  - Address is dupe!");
    		}
	    }
	    /*
	     * Remove duplicates
	     */
	    while(duplicateAddresses.size() > 0) {
	    	addresses.remove(duplicateAddresses.get(0));
	    	duplicateAddresses.remove(0);
	    }
	    
	    /*
	     * Then convert it into the array setItems wants
	     */
	    final String[] items = new String[addressStrings.size()];
	    addressStrings.toArray(items);
	    
	    /*
	     * Then search for the address
	     */
	    builder.setItems(items, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SearchAddressTask.this.foundLocation(addresses.get(which));
			}
	    });
	    
	    
	    dialog = builder.create();
		/*
		 * Handler onCancel
		 */
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				handler.onCanceled();
				tracker.stop();
			}
		});
		dialog.show();
	}
	
	/*
	 * Do geo mapping.
	 */
	private void geoMap(String searchAddress) {
		final Geocoder geocoder = new Geocoder(activity);
		try {
			addresses = geocoder.getFromLocationName(searchAddress, 10, 57, 3, 71, 32);

			switch(addresses.size()) {
			case 0:
				Toast.makeText(activity, R.string.failedToFindAddress, Toast.LENGTH_SHORT).show();
				handler.onCanceled();
				break;
			case 1:
				foundLocation(addresses.get(0));
				break;
			default:
				showAddressSelection();
			    break;
			}
		} catch (IOException e) {
			/*
			 * Pass exceptions to parent
			 */
			handler.onError(e);
		}
	}
	
	private void foundLocation(Address location) {
		handler.onFinished(location.getLatitude(), location.getLongitude());
		tracker.stop();				

	}

	@Override
	public void stop() {
		handler.onCanceled();
		dialog.dismiss();
	}
}
