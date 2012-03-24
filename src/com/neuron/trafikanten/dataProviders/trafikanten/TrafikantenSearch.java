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

/**
 * Trafikanten's api is under trafikanten's control and may not be used 
 * without written permission from trafikanten.no.
 * 
 * See Developer.README
 */


package com.neuron.trafikanten.dataProviders.trafikanten;

import java.io.InputStream;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.content.Context;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataProviders.GenericDataProviderThread;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataSets.StationData;


public class TrafikantenSearch extends GenericDataProviderThread<StationData> {
	private static final String TAG = "Trafikanten-TrafikantenSearch";
	
	public final Context context;
	private double latitude = 0;
	private double longitude = 0;
	
	private String query = null;
	private boolean isRealtimeStopFiltered = false;
	
	public TrafikantenSearch(Context context, double latitude, double longitude, boolean isRealtimeStopFiltered, IGenericProviderHandler<StationData> handler) {
		super();
		this.context = context;
		this.latitude = latitude;
		this.longitude = longitude;
		this.isRealtimeStopFiltered = isRealtimeStopFiltered;
		start(handler);
	}
	
	public TrafikantenSearch(Context context, String query, boolean isRealtimeStopFiltered, IGenericProviderHandler<StationData> handler) {
		super();
		this.context = context;
		this.query = query;
		this.isRealtimeStopFiltered = isRealtimeStopFiltered;
		start(handler);
	}
	
	/*
	 * This searches stopName for address, and puts address in extra (the next line).
	 * As station names are sometimes StationName (address)
	 */
	public static void searchForAddress(StationData station) {
		final String stopName = station.stopName;
	
		int startAddress = stopName.indexOf('(');
		if (startAddress < 0) {
			return;
		} else {
			String address = stopName.substring(startAddress + 1, stopName.length() - 1);
			station.stopName = stopName.substring(0, startAddress - 1);
			if (address.startsWith("i "))
				address = address.substring(2);
			station.extra = address;
		}
	}
	
    @Override
	public void run() {
		try {
			String urlString;
			
			if (query != null) {
				/*
				 * Setup URL for a normal station search query.
				 */
				urlString = Trafikanten.getApiUrl() + "/ReisRest/Place/FindMatches/" + HelperFunctions.properEncode(query);
			} else {
				/*
				 * Setup URL for coordinate search.
				 */
				final LatLng latLong = new LatLng(latitude, longitude);
				final UTMRef utmRef = latLong.toUTMRef();
				urlString = Trafikanten.getApiUrl() + "/ReisRest/Stop/GetClosestStopsByCoordinates/?coordinates=(X=" +  (int) utmRef.getEasting() + ",Y=" + (int) utmRef.getNorthing() + ")&proposals=10";
			}
			Log.i(TAG,"Searching with url " + urlString);
			final InputStream stream = HelperFunctions.executeHttpRequest(context, new HttpGet(urlString), false).stream;
			
			/*
			 * Parse json
			 */
			final JSONArray jsonArray = new JSONArray(HelperFunctions.InputStreamToString(stream));
			final int arraySize = jsonArray.length();
			for (int i = 0; i < arraySize; i++) {
				final JSONObject json = jsonArray.getJSONObject(i);
				/*
				 * We only care about normal stops for now, type 0.
				 */
				if (json.getInt("Type") == 0) {
					StationData station = new StationData();
					
					// We parse realtimestop first for performance reason.
					station.realtimeStop = json.getBoolean("RealTimeStop");
					if (isRealtimeStopFiltered && !station.realtimeStop) {
						continue;
					}
					
					station.stationId = json.getInt("ID");
					station.stopName = json.getString("Name");
					searchForAddress(station);
					
					final String district = json.getString("District");
					if (district.length() > 0) {
				    	if (station.extra == null) {
			    			station.extra = district;
			    		} else {
			    			station.extra = station.extra + ", " + district;
			    		}
					}
					
					if (json.has("WalkingDistance")) {
						station.walkingDistance = json.getInt("WalkingDistance");
					}
					station.utmCoords[0] = json.getInt("X");
					station.utmCoords[1] = json.getInt("Y");
					
					ThreadHandlePostData(station);
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