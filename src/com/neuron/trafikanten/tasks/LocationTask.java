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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.locationProviders.ILocationProvider;
import com.neuron.trafikanten.locationProviders.LocationProviderFactory;

/*
 * Calculate our location, and search for a station
 */
public class LocationTask extends GenericTask {
	public static final int TASK_LOCATION = 105;
	private ILocationProvider locationProvider;
	
	public static void StartTask(Activity activity) {
		final Intent intent = new Intent(activity, LocationTask.class);
		StartGenericTask(activity, intent, TASK_LOCATION);
	}
	
	@Override
	public int getlayoutId() { return R.layout.dialog_waitlocation;	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		message.setText(R.string.locationWaiting);
		
		/*
		 * Setup continue button
		 */
		Button continueButton = (Button) findViewById(R.id.continueButton);
		continueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				returnOk();
			}
		});
		locationProvider = LocationProviderFactory.getLocationProvider(this, locationHandler);
	}
	
	private void returnOk() {
		final double[] location = LocationProviderFactory.getLocation();
		if (location[0] == 0) {
			Toast.makeText(this, R.string.noLocationFoundError, Toast.LENGTH_SHORT).show();
			return;
		}
		SearchStationTask.StartTask(this, location[0], location[1]);
		setVisible(false);
	}
	
	
	/*
	 * Handler for location service:
	 */
    public final Handler locationHandler = new Handler() {
        public void handleMessage(Message msg) {
        	if (msg.what == ILocationProvider.MESSAGE_DONE) {
        		/*
        		 * Got a location.
        		 */
        		final double[] location = LocationProviderFactory.getLocation();
        		message.setText(getText(R.string.locationWaiting).toString() + "\n" + getText(R.string.current) + " " + location[2] + "m");
        		if (LocationProviderFactory.SETTING_LOCATION_ACCURACY > location[2]) {
        			returnOk();
        		}
        		return;
        	}
        	/*
        	 * msg.what is an error/warning/otherwise, pass to parent and return.
        	 */
        	Intent intent = new Intent();
        	intent.putExtra(KEY_MESSAGE, msg);
        	setResult(RESULT_OK, intent);
        	finish();
        }
    };
    
    /*
     * Direct passthrough
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	setResult(resultCode, data);
    	finish();
    }

    /*
     * Dont keep polling location if we switch views.
     * @see android.app.Activity#onPause()
     */
	@Override
	protected void onPause() {
		locationProvider.stop();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		message.setText(R.string.locationWaiting);
		locationProvider.getPeriodicLocation();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
}
