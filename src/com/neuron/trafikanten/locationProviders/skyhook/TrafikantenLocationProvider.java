package com.neuron.trafikanten.locationProviders.skyhook;

import android.content.Context;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.XPS;

public class TrafikantenLocationProvider {
	public static final int SETTING_LOCATION_ACCURACY = 80; // Needed accuracy for auto continue when waiting for a fix.
	private final static String TAG = "Trafikanten-SkyhookLocation";
	private final IGenericProviderHandler<LocationData> handler;
	private boolean _stop = true;
	
	private WPSAuthentication _auth;
	private XPS _xps;

	public TrafikantenLocationProvider(Context context, IGenericProviderHandler<LocationData> handler) {
		this.handler = handler;
		_auth = new WPSAuthentication("aagaande", "http://code.google.com/p/trafikanten/");
		
		/*
		 * Setup cache for XPS
		 */
		_xps = new XPS(context);
		_xps.setTiling(context.getCacheDir().toString(),
                200*1024,
                1000*1024,
                null);
		handler.onPreExecute();
		getPeriodicLocation();
	}
	
	/*
	 * Get a periodic location update
	 */
	private void getPeriodicLocation() {
		Log.i(TAG,"Getting periodic location");
		_stop = false;
		_xps.getXPSLocation(_auth, (int) 2, XPS.EXACT_ACCURACY, _locationListener);
	}

	public void stop() {
		_stop = true;
		_xps.abort();
	}
	
	public boolean running() {
		return (!_stop);
	}
	
	/*
	 * Location Listener, notify caller.
	 */
	private WPSPeriodicLocationCallback _locationListener = new WPSPeriodicLocationCallback() {

		@Override
		public WPSContinuation handleWPSPeriodicLocation(WPSLocation location) {
	    	if (_stop) {
	    		return WPSContinuation.WPS_STOP;
	    	}
			
	    	/*
	    	 * Check the age, if the age is too old it's a cached gps position, we dont want those.
	    	 */
			final long age = (System.currentTimeMillis() - location.getTime()) / HelperFunctions.SECOND;
			if (age > 30) { // Age > 30 seconds is ignored
				return WPSContinuation.WPS_CONTINUE;
			}

			/*
			 * Notify we've found a location
			 */
			Log.i(TAG,"Recieved location update " + location.getNAP() + " " + location.getHPE());
			final double latitude = location.getLatitude();
			final double longitude = location.getLongitude();
			final double accuracy = Math.round(location.getHPE());
			handler.onData(new LocationData(latitude, longitude, accuracy));
	    	return WPSContinuation.WPS_CONTINUE;
		}

		@Override
		public void done() {
			handler.onPostExecute(null);
		}

		@Override
		public WPSContinuation handleError(WPSReturnCode arg0) {
			/*
			 * We keep trying until canceled.
			 */
			return WPSContinuation.WPS_CONTINUE;
		}
	};
	
	public class LocationData {
		public final double latitude;
		public final double longitude;
		public final double accuracy;
		
		public LocationData(double latitude, double longitude, double accuracy) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.accuracy = accuracy;
		}
	}

}

