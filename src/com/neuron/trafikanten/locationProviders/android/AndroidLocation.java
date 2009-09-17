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

package com.neuron.trafikanten.locationProviders.android;

import java.util.List;

import com.neuron.trafikanten.locationProviders.ILocationProvider;
import com.neuron.trafikanten.locationProviders.LocationProviderFactory;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

public class AndroidLocation implements ILocationProvider {
	public static final int PROVIDER_ANDROID = 1;
	private Handler handler;
	private boolean _stop = true;
	private LocationManager _locationManager;
	
	private List<String> _providers;

	public AndroidLocation(Context context, Handler handler) {
		this.handler = handler;
		_locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		/*
		 * Setup Criteria and select provider.
		 */
		_providers = _locationManager.getAllProviders();
		/*final Criteria criteria = new Criteria();
		//criteria.setAccuracy(Criteria.ACCURACY_COARSE);  // Faster, no GPS fix.
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		criteria.setCostAllowed(false);
		
		_provider = _locationManager.getBestProvider(criteria, true);
		if (_provider == null)
			_provider = _locationManager.getBestProvider(criteria, false);*/
	}
	
	/*
	 * Get a periodic location update
	 * @see com.neuron.trafikanten.locationProviders.ILocationProvider#getPeriodicLocation()
	 */
	@Override
	public void getPeriodicLocation() {
		_stop = false;
		for(String provider : _providers) {
			_locationManager.requestLocationUpdates(provider,
					120000, // 2min 
					100, // 100m 
					_locationListener); 
		}		
	}

	/*
	 * Get a single location
	 * @see com.neuron.trafikanten.locationProviders.ILocationProvider#getSingleLocation()
	 */
	@Override
	public void getSingleLocation() {
		_stop = true;
		for(String provider : _providers) {
			_locationManager.requestLocationUpdates(provider,
					120000, // 2min 
					100, // 100m 
					_locationListener); 
		}		
	}

	/*
	 * Stop location updates (no reports are done after stop is called)
	 */
	@Override
	public void stop() {
		_stop = true;
		_locationManager.removeUpdates(_locationListener);
	}

	/*
	 * Location Listener, notify caller.
	 */
	private LocationListener _locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
		    if (location != null) {
		    	LocationProviderFactory.setLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
		    	handler.sendEmptyMessage(ILocationProvider.MESSAGE_DONE);
		        
		    	if (_stop) {
		    		stop();
		    	}
		    }
		  }

		@Override
		public void onProviderDisabled(String arg0) {}

		@Override
		public void onProviderEnabled(String arg0) {}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
	};
}