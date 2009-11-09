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
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.locationProviders.ILocationProvider;
import com.neuron.trafikanten.locationProviders.LocationProviderFactory;
import com.neuron.trafikanten.tasks.handlers.ReturnCoordinatesHandler;

/*
 * Calculate our location, and search for a station
 */
public class LocationTask implements GenericTask {
	private ILocationProvider locationProvider;
	private Activity activity;
	private ReturnCoordinatesHandler handler;
	private TextView message;
	
    public LocationTask(Activity activity, ReturnCoordinatesHandler handler)
    {
            this.activity = activity;
            this.handler = handler;
            showDialog();

    }
    
    public void showDialog() {
		final Dialog dialog = new Dialog(activity);
		dialog.setContentView(R.layout.dialog_waitlocation);
		
		message = (TextView) dialog.findViewById(R.id.message);
		message.setText(R.string.locationWaiting);
		/*
		 * Setup continue button
		 */
		Button continueButton = (Button) dialog.findViewById(R.id.continueButton);
		continueButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				returnLocation();
			}
		});
		locationProvider = LocationProviderFactory.getLocationProvider(activity, locationHandler);
		
		/*
		 * Handler onCancel
		 */
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				locationProvider.Stop();
				handler.onCanceled();				
			}
		});
		
		dialog.show();
    }
    
    private void returnLocation() 
    {
    	locationProvider.Stop();
    	final double[] location = LocationProviderFactory.getLocation();
		if (location[0] == 0) {
			Toast.makeText(activity, R.string.noLocationFoundError, Toast.LENGTH_SHORT).show();
			return;
		}
		handler.onFinished(location[0], location[1]);
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
        		message.setText(activity.getText(R.string.locationWaiting).toString() + "\n" + activity.getText(R.string.current) + " " + location[2] + "m");
        		if (LocationProviderFactory.SETTING_LOCATION_ACCURACY > location[2] && location[2] > 0) {
        			/*
        			 * Return instant location ok only if accuracy is enough, and it's not a cached gps location (accuracy 0.0 meters)
        			 */
        			returnLocation();
        		}
        		return;
        	}
        }
    };
    
	@Override
	public void stop() {
		locationProvider.Stop();		
	}
}
