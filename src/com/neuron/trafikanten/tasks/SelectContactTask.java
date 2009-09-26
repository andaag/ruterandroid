/**
 *     Copyright (C) 2009 Anders Aagaard <aagaande@gmail.com>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neuron.trafikanten.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IRouteProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Message;
import android.provider.Contacts;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

public class SelectContactTask extends GenericTask {
	private static final String TAG = "Trafikanten-SelectContactTask";
	public static final int TASK_SELECTCONTACT = 104;
	private static final int DIALOG_SELECTCONTACT = Menu.FIRST;
	
	
	public static final String KEY_ADDRESS = "address";
	public static final String KEY_MESSAGE = "message";

	public static void StartTask(Activity activity) {
		final Intent intent = new Intent(activity, SelectContactTask.class);
		StartGenericTask(activity, intent, TASK_SELECTCONTACT);
	}
	
	@Override
	public int getlayoutId() { return R.layout.dialog_progress;	}
	
	/*
	 * onCreate for selectContactTask.
	 * Task shows contact list, allows user to select a contact, calculates geo position (with loading bar) and returns.
	 * @see com.neuron.trafikanten.tasks.GenericTask#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		message.setText(R.string.searchStationTask);
		setVisible(false);
		showDialog(DIALOG_SELECTCONTACT);
	}
	
	private static final String FILTER_POSTAL = Contacts.ContactMethods.KIND + "=" + Contacts.KIND_POSTAL;
	private static final String[] CONTACTMETHODS_PROJECTION = new String[] {
		Contacts.People.NAME,
		Contacts.ContactMethods.DATA
	};
	
	
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case DIALOG_SELECTCONTACT:
			/*
			 * Dialog contains a list, force recreating it.
			 */
			removeDialog(DIALOG_SELECTCONTACT);
			dialog = onCreateDialog(DIALOG_SELECTCONTACT);
			break;
		}
		super.onPrepareDialog(id, dialog);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_SELECTCONTACT:
			/*
			 * Setup list of names with addresses
			 */
			ArrayList<String> nameList = new ArrayList<String>();
			final Cursor cursor = managedQuery(Contacts.ContactMethods.CONTENT_URI, CONTACTMETHODS_PROJECTION, FILTER_POSTAL, null, null);
			while (cursor.moveToNext()) {
				nameList.add(cursor.getString(0));
			}
			cursor.close();
			
			/*
			 * Setup select contact alert dialog
			 */
		    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setTitle(R.string.selectContact);
		    final String[] items = new String[nameList.size()];
		    nameList.toArray(items);
		    builder.setItems(items, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int item) {
		        	final Cursor cursor = managedQuery(Contacts.ContactMethods.CONTENT_URI, CONTACTMETHODS_PROJECTION,
		        			Contacts.People.NAME + " == ? AND " +
		        			FILTER_POSTAL, new String[] {items[item]}, null);
		        	if (!cursor.moveToNext()) {
		        		Log.w(TAG, "Couldn't lookup address for contact after contact selection : " + items[item]);
		        		Toast.makeText(SelectContactTask.this, "Failed to lookup contact, this is a bug, report!", Toast.LENGTH_SHORT).show();
		        		return;
		        	}
		        	dialog.dismiss();
		        	geoMap(cursor.getString(0), cursor.getString(1));
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
					SelectContactTask.this.setResult(RESULT_CANCELED, new Intent());
					SelectContactTask.this.finish();
				}
		    	
		    });
		    return dialog;
			
		}
		return super.onCreateDialog(id);
	}
	
	/*
	 * Do geo mapping.
	 */
	private void geoMap(String name, String address) {
		setVisible(true);
		final Geocoder geocoder = new Geocoder(this);
		try {
			List<Address> addresses = geocoder.getFromLocationName(address, 2, 57, 3, 66, 16);

			switch(addresses.size()) {
			case 0:
				Toast.makeText(getApplicationContext(), R.string.failedToFindAddress, Toast.LENGTH_SHORT).show();
				break;
			case 2:
				Toast.makeText(getApplicationContext(), R.string.multipleAddressesFound, Toast.LENGTH_SHORT).show();
				// Fallthrough
			default:
				foundLocation(addresses.get(0));
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
		setVisible(false);
		SearchStationTask.StartTask(this, location.getLatitude(), location.getLongitude());
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
