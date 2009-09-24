package com.neuron.trafikanten.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Toast;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IRouteProvider;

public class SearchAddressTask extends GenericTask {
	private static final String TAG = "Trafikanten-SearchAddressTask";
	public static final int TASK_SEARCHADDRESS = 106;
	private static final int DIALOG_SEARCHADDRESS = Menu.FIRST;
	
	private List<Address> addresses;

	public static void StartTask(Activity activity) {
		final Intent intent = new Intent(activity, SearchAddressTask.class);
		StartGenericTask(activity, intent, TASK_SEARCHADDRESS);
	}
	
	@Override
	public int getlayoutId() { return R.layout.dialog_searchaddress; }
	
	/*
	 * onCreate for searchAddressTask
	 * Task shows a entry box for writing in an address, then does geo lookup to find the coordinates, and asks provider for data around that point.
	 * @see com.neuron.trafikanten.tasks.GenericTask#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		message.setText(R.string.searchAddressTask);
		
		final EditText searchEdit = (EditText) findViewById(R.id.search);
		searchEdit.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }
                
                switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                	geoMap(searchEdit.getText().toString());
                	return true;
                }
				return false;
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_SEARCHADDRESS:
		    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setTitle(R.string.searchAddressTask);
		    

		    /*
		     * First take all addresses, convert them into strings and check for duplicates.
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
	    		}
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
		    
		    
		    AlertDialog dialog = builder.create();
		    dialog.setOnCancelListener(new OnCancelListener() {
		    	/*
		    	 * OnCancel we should return to previous view.
		    	 * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
		    	 */
				@Override
				public void onCancel(DialogInterface dialog) {
					SearchAddressTask.this.setResult(RESULT_CANCELED, new Intent());
					SearchAddressTask.this.finish();
				}
		    	
		    });
		    return dialog;
		}
		return null;
	}
	
	/*
	 * Do geo mapping.
	 */
	private void geoMap(String searchAddress) {
		setVisible(true);
		final Geocoder geocoder = new Geocoder(this);
		try {
			addresses = geocoder.getFromLocationName(searchAddress, 2);

			switch(addresses.size()) {
			case 0:
				Toast.makeText(getApplicationContext(), R.string.failedToFindAddress, Toast.LENGTH_SHORT).show();
				finish();
				break;
			case 1:
				foundLocation(addresses.get(0));
				break;
			default:
				showDialog(DIALOG_SEARCHADDRESS);
			    break;
			}
		} catch (IOException e) {
			/*
			 * Pass exceptions to parent
			 */
			final Message msg = handler.obtainMessage(IRouteProvider.MESSAGE_EXCEPTION);
			final Bundle bundle = new Bundle();
			bundle.putString(IRouteProvider.KEY_EXCEPTION, e.toString());
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
	
	private void foundLocation(Address location) {
		SearchStationTask.StartTask(this, location.getLatitude(), location.getLongitude());
		setVisible(false);
	}
	
    /*
     * Direct passthrough
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	setResult(resultCode, data);
    	finish();
    }
}
