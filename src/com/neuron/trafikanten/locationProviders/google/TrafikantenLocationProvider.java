package com.neuron.trafikanten.locationProviders.google;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataProviders.GenericDataProviderThread;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataSets.LocationData;

/*
 * NOTE, this is using GenericDataProviderThread. This makes it a thread, but it's a thread that never runs.
 * The reason for this is allowing us to use the GenericDataProviderThread's ThreadPost etc functions from skyhooks callback thread.
 */
public class TrafikantenLocationProvider extends GenericDataProviderThread<LocationData> {
	public static final int SETTING_LOCATION_ACCURACY = 80; // Needed accuracy for auto continue when waiting for a fix.
	private final static String TAG = "Trafikanten-GoogleLocation";
	private boolean _stop = true;
	private LocationManager mLocationManager;
	private LocationListener mLocationListener;
	
	public TrafikantenLocationProvider(Context context, IGenericProviderHandler<LocationData> handler) {
		super(handler);
		handler.onPreExecute(); // we're running this even when we're not a thread, so we manually trigger onPreExecute
		
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new MyLocationListener();
		getPeriodicLocation();
	}
	
	/*
	 * Get a periodic location update
	 */
	private void getPeriodicLocation() {
		Log.i(TAG,"Getting periodic location");
		_stop = false;
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
	}
	
	

	@Override
	public void kill() {
		_stop = true;
		mLocationManager.removeUpdates(mLocationListener);
		ThreadHandlePostExecute(null);
		super.kill(); // this isn't a thread, so this is sortof pointless.
	}
	
	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if (_stop) {
				return;
			}
			
			final long age = (System.currentTimeMillis() - location.getTime()) / HelperFunctions.SECOND;
			if (age > 30) { // Age > 30 seconds is ignored
				return;
			}
			
			/*
			 * Notify we've found a location
			 */
			
			Log.i(TAG,"Recieved location update " + location.getAccuracy() + " " + location.getProvider());
			ThreadHandlePostData(new LocationData(location.getLatitude(), location.getLongitude(), Math.round(location.getAccuracy())));
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
	}
}

