package com.neuron.trafikanten.locationProviders.skyhook;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.locationProviders.ILocationProvider;
import com.neuron.trafikanten.locationProviders.LocationProviderFactory;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.XPS;

public class SkyhookLocation implements ILocationProvider {
	private final static String TAG = "Trafikanten-SkyhookLocation";
	public static final int PROVIDER_SKYHOOK = 1;
	private Handler handler;
	private boolean _stop = true;
	
	private WPSAuthentication _auth;
	private XPS _xps;
	

	public SkyhookLocation(Context context, Handler handler) {
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
	}
	
	/*
	 * Get a periodic location update
	 * @see com.neuron.trafikanten.locationProviders.ILocationProvider#getPeriodicLocation()
	 */
	@Override
	public void getPeriodicLocation() {
		Log.i(TAG,"Getting periodic location");
		_stop = false;
		_xps.getXPSLocation(_auth, (int) 2, XPS.EXACT_ACCURACY, _locationListener);
	}

	@Override
	public void Stop() {
		_stop = true;
		_xps.abort();
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
			Log.d("DEBUG CODE", "handleLocation " + age + " " + location.toString());
			if (age > 30) { // Age > 30 seconds is ignored
				return WPSContinuation.WPS_CONTINUE;
			}

			/*
			 * Notify we've found a location
			 */
			Log.i(TAG,"Recieved location update " + location.getNAP() + " " + location.getHPE());
	    	LocationProviderFactory.setLocation(location.getLatitude(), location.getLongitude(), Math.round(location.getHPE()));
	    	handler.sendEmptyMessage(ILocationProvider.MESSAGE_DONE);
	        
	    	return WPSContinuation.WPS_CONTINUE;
		}

		@Override
		public void done() {}

		@Override
		public WPSContinuation handleError(WPSReturnCode arg0) {
			/*
			 * We keep trying until canceled.
			 */
			return WPSContinuation.WPS_CONTINUE;
		}
	};
}