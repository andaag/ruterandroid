package com.neuron.trafikanten.locationProviders.skyhook;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

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
	private boolean _stopInstant = false;
	
	private WPSAuthentication _auth;
	private XPS _xps;
	

	public SkyhookLocation(Context context, Handler handler) {
		this.handler = handler;
		_auth = new WPSAuthentication("aagaande", "http://code.google.com/p/trafikanten/");
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
		_stopInstant = false;
        _xps.getXPSLocation(_auth,
                5,
                XPS.EXACT_ACCURACY,
                _locationListener);
	}

	/*
	 * Get a single location
	 * @see com.neuron.trafikanten.locationProviders.ILocationProvider#getSingleLocation()
	 */
	@Override
	public void getSingleLocation() {
		Log.i(TAG,"Getting single location");
		_stop = true;
		_stopInstant = false;
        _xps.getXPSLocation(_auth,
                1,
                XPS.EXACT_ACCURACY,
                _locationListener);	
	}

	

	@Override
	public void stop() {
		_stopInstant = true;
		_xps.abort();
		_stop = true;
	}
	
	/*
	 * Location Listener, notify caller.
	 */
	private WPSPeriodicLocationCallback _locationListener = new WPSPeriodicLocationCallback() {

		@Override
		public WPSContinuation handleWPSPeriodicLocation(WPSLocation location) {
			if (_stopInstant) {
				return WPSContinuation.WPS_STOP;
			}
			Log.i(TAG,"Recieved location update");

	    	LocationProviderFactory.setLocation(location.getLatitude(), location.getLongitude(), location.getHPE());
	    	handler.sendEmptyMessage(ILocationProvider.MESSAGE_DONE);
	        
	    	if (_stop) {
	    		return WPSContinuation.WPS_STOP;
	    	}
	    	return WPSContinuation.WPS_CONTINUE;
		}

		@Override
		public void done() {}

		@Override
		public WPSContinuation handleError(WPSReturnCode arg0) {
			Log.e(TAG, "WPSContinuation - HandleError " + arg0);
			return null;
		}

/*		public void onLocationChanged(Location location) {
		    if (location != null) {
		    	LocationProviderFactory.setLocation(location.getLatitude(), location.getLongitude(), location.getAccuracy());
		    	handler.sendEmptyMessage(ILocationProvider.MESSAGE_DONE);
		        
		    	if (_stop) {
		    		stop();
		    	}
		    }
		  }*/
	};
}