package com.neuron.trafikanten.dataProviders.trafikanten;

import java.io.InputStream;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataProviders.GenericDataProviderThread;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataSets.StationData;

public class TrafikantenTrip extends GenericDataProviderThread<StationData> {
	private static final String TAG = "Trafikanten-TrafikantenTrip";
	private final Context context;
	private final int tourID;
	private int startStation;
	private int stopStation;
	
	/*
	 * can filter on startStation/stopStation and only return data in between
	 * to ignore filtering set startStation = stopStation = 0
	 */
	public TrafikantenTrip(Context context, int tourID, int startStation, int stopStation, IGenericProviderHandler<StationData> handler) {
		super(handler);
		this.context = context;
		this.tourID = tourID;
		this.startStation = startStation;
		this.stopStation = stopStation;
		start();
	}
	
	@Override
	public void run() {
		try {
			/*
			 * Setup args and send request
			 */
			final String urlString = "http://services.epi.trafikanten.no/Trip/GetTrip/" + tourID + "/";
			Log.i(TAG,"Searching with url " + urlString);
			final InputStream stream = HelperFunctions.executeHttpRequest(context, new HttpGet(urlString));

			/*
			 * Parse json
			 */
			final JSONArray jsonArray = new JSONObject(HelperFunctions.InputStreamToString(stream)).getJSONArray("Stops");
			final int arraySize = jsonArray.length();
			for (int i = 0; i < arraySize; i++) {
				final JSONObject json = jsonArray.getJSONObject(i);
				StationData station = new StationData();
				
				
				station.stationId = json.getInt("ID");
				station.realtimeStop = json.getBoolean("RealTimeStop");
				station.stopName = json.getString("Name");
				TrafikantenSearch.searchForAddress(station);
				
				final String district = json.getString("District");
				if (district.length() > 0) {
			    	if (station.extra == null) {
		    			station.extra = district;
		    		} else {
		    			station.extra = station.extra + ", " + district;
		    		}
				}
				station.utmCoords[0] = json.getInt("X");
				station.utmCoords[1] = json.getInt("Y");
				
				
				/*
				 * Check if station is between startStation and stopStation
				 */
	        	if (startStation > 0) {
	        		if (station.stationId == startStation) {
	        			startStation = 0;
	        		}
	        	}
	        	if (startStation == 0) {
	        		ThreadHandlePostData(station);
	        	}
	        	/*
	        	 * If the station data we just sent is the stopStation, set startStation to a value we'll never find
	        	 */
	        	if (station.stationId == stopStation) {
	        		startStation = Integer.MAX_VALUE;
	        	}
			}

		} catch(Exception e) {
			if (e.getClass() == InterruptedException.class) {
				ThreadHandlePostExecute(null);
				return;
			}
			ThreadHandlePostExecute(e);
			return;
		}
		ThreadHandlePostExecute(null);
    }
}