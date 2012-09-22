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

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.HelperFunctions.StreamWithTime;
import com.neuron.trafikanten.dataProviders.GenericDataProviderThread;
import com.neuron.trafikanten.dataSets.RealtimeData;

public class TrafikantenRealtime extends GenericDataProviderThread<RealtimeData> {
	public final static int MSG_TIMEDATA = 10;
	private static final String TAG = "Trafikanten-T-RealtimeThread";
	private final Context context;
	
	private final int stationId;
	
	public TrafikantenRealtime(Context context, int stationId) {
		super();
		this.context = context;
		this.stationId = stationId;
	}
		
    @Override
	public void run() {
		try {
			final String urlString = Trafikanten.getApiUrl() + "/reisrest/realtime/GetAllDepartures/" + stationId;
			Log.i(TAG,"Loading realtime data : " + urlString);
			
			final StreamWithTime streamWithTime = HelperFunctions.executeHttpRequest(context, new HttpGet(urlString), true);
			ThreadHandleTimeData(streamWithTime.timeDifference);

			/*
			 * Parse json
			 */
			//long perfSTART = System.currentTimeMillis();
			//Log.i(TAG,"PERF : Getting realtime data");
			final JSONArray jsonArray = new JSONArray(HelperFunctions.InputStreamToString(streamWithTime.stream));
			final int arraySize = jsonArray.length();
			for (int i = 0; i < arraySize; i++) {
				final JSONObject json = jsonArray.getJSONObject(i);
				RealtimeData realtimeData = new RealtimeData();
				try {
					realtimeData.expectedDeparture = HelperFunctions.jsonToDate(json.getString("ExpectedDepartureTime"));
				} catch (org.json.JSONException e) {
					realtimeData.expectedDeparture = HelperFunctions.jsonToDate(json.getString("ExpectedArrivalTime"));
				}

				
				try {
					realtimeData.lineId = json.getInt("LineRef");					
				} catch (org.json.JSONException e) {
					realtimeData.lineId = -1;
				}
				
				try {
					realtimeData.vehicleMode = json.getInt("VehicleMode");
				} catch (org.json.JSONException e) {
					realtimeData.vehicleMode = 0; // default = bus
				}
                
				
				try {
					realtimeData.destination = json.getString("DestinationName");
				} catch (org.json.JSONException e) {
					realtimeData.destination = "Ukjent";
				}
				
				try {
					realtimeData.departurePlatform = json.getString("DeparturePlatformName");
					if (realtimeData.departurePlatform.equals("null")) {
						realtimeData.departurePlatform = "";
					}

				} catch (org.json.JSONException e) {
					realtimeData.departurePlatform = "";
				}
				
				try {
					realtimeData.realtime = json.getBoolean("Monitored");
				} catch (org.json.JSONException e) {
					realtimeData.realtime = false;
				}
				try {
					realtimeData.lineName = json.getString("PublishedLineName");
				} catch (org.json.JSONException e) {
					realtimeData.lineName = "";
				}
				
				try {
					if (json.has("InCongestion")) {
						realtimeData.inCongestion = json.getBoolean("InCongestion");
					}
				} catch (org.json.JSONException e) {
					// can happen when incongestion is empty string.
				}

				try {
					if (json.has("VehicleFeatureRef")) {
						realtimeData.lowFloor = json.getString("VehicleFeatureRef").equals("lowFloor");
					}
				} catch (org.json.JSONException e) {
					// lowfloor = false by default
				}
					
				try {
					if (json.has("TrainBlockPart") && !json.isNull("TrainBlockPart")) {
						JSONObject trainBlockPart = json.getJSONObject("TrainBlockPart");
						if (trainBlockPart.has("NumberOfBlockParts")) {
							realtimeData.numberOfBlockParts = trainBlockPart.getInt("NumberOfBlockParts");						
						}
					}
				} catch (org.json.JSONException e) {
					// trainblockpart is initialized by default
				}
				
				ThreadHandlePostData(realtimeData);
			}
			
			//Log.i(TAG,"PERF : Parsing web request took " + ((System.currentTimeMillis() - perfSTART)) + "ms");

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
    
    
    
    /*
     * Extra thread function to send time difference
     */
    public void ThreadHandleTimeData(Long data) {
    	Message msg = threadHandler.obtainMessage(MSG_TIMEDATA);
    	msg.obj = data;
    	threadHandler.sendMessage(msg);
    }
}
