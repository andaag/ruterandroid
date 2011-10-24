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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.AnalyticsUtils;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataSets.LocationData;
import com.neuron.trafikanten.locationProviders.skyhook.TrafikantenLocationProvider;
import com.neuron.trafikanten.tasks.handlers.ReturnCoordinatesHandler;

/*
 * Calculate our location, and search for a station
 */
public class LocationTask implements GenericTask {
	private TrafikantenLocationProvider locationProvider;
	private Activity activity;
	private ReturnCoordinatesHandler handler;
	private TextView message;
	
	/*
	 * Coordinates:
	 */
	private double latitude;
	private double longitude;
	
	/*
	 * Dialog related
	 */
	private Dialog dialog;
	private Button continueButton;
    private Handler buttonRefresher = new Handler();
    private long showContinueAfterMs = 20000;
	
	
    public LocationTask(Activity activity, ReturnCoordinatesHandler handler)
    {
            this.activity = activity;
            AnalyticsUtils.getInstance(activity).trackPageView("/task/location");
            this.handler = handler;
            showDialog();
    }
    
    private Runnable updateButton = new Runnable() {
		@Override
		public void run() {
			showContinueAfterMs = showContinueAfterMs - 5000;
			if (showContinueAfterMs <= 0 && latitude != 0) {
				continueButton.setVisibility(View.VISIBLE);
			} else {
				buttonRefresher.postDelayed(updateButton, 5000);
				continueButton.setVisibility(View.GONE);
			}
		}
    };
    
    private void showDialog() {
		dialog = new Dialog(activity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_waitlocation);
		
		message = (TextView) dialog.findViewById(R.id.message);
		message.setText(R.string.locationWaiting);
		/*
		 * Setup continue button
		 */
		continueButton = (Button) dialog.findViewById(R.id.continueButton);
		continueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				returnLocation();
				dialog.dismiss();
			}
		});
		updateButton.run();
		
		locationProvider = new TrafikantenLocationProvider(activity, new IGenericProviderHandler<LocationData>() {
			@Override
			public void onExtra(int what, Object obj) {
				/* Class has no extra data */
			}
			
			@Override
			public void onData(LocationData data) {
				LocationTask.this.latitude = data.latitude;
				LocationTask.this.longitude = data.longitude;
        		message.setText(activity.getText(R.string.locationWaiting).toString() + "\n" + activity.getText(R.string.current) + " " + data.accuracy + "m");
        		if (TrafikantenLocationProvider.SETTING_LOCATION_ACCURACY > data.accuracy && data.accuracy > 0) {
        			/*
        			 * Return instant location ok only if accuracy is enough, and it's not a cached gps location (accuracy 0.0 meters)
        			 */
        			AnalyticsUtils.getInstance(activity).trackEvent("Task", "FoundLocation", null, (int)data.accuracy);
        			returnLocation();
        			dialog.dismiss();
        		}
				
				
			}

			@Override
			public void onPostExecute(Exception e) {
				// not needed
			}

			@Override
			public void onPreExecute() {
				// not needed
			}
		});
		
		/*
		 * Handler onCancel
		 */
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				locationProvider.kill();
				handler.onCanceled();	
			}
		});
		
		dialog.show();
    }
    
    private void returnLocation() 
    {
    	locationProvider.kill();
		if (latitude == 0) {
			Toast.makeText(activity, R.string.noLocationFoundError, Toast.LENGTH_SHORT).show();
			return;
		}
		handler.onFinished(latitude, longitude);
    }
    
	@Override
	public void stop() {
		AnalyticsUtils.getInstance(activity).trackEvent("Task", "FoundLocation", null, -1);
		locationProvider.kill();
		handler.onCanceled();
		dialog.dismiss();
	}
}
