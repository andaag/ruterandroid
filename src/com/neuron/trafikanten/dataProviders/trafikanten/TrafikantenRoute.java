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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.GenericDataProviderThread;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.RouteSearchData;
import com.neuron.trafikanten.dataSets.StationData;
import com.neuron.trafikanten.hacks.WaittimeBug;

public class TrafikantenRoute extends GenericDataProviderThread<RouteProposal> {
	private static final String TAG = "Trafikanten-TrafikantenRoute";
	private final Context context;
	private final RouteSearchData routeSearch;
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat("ddMMyyyyHHmm");
	
	public TrafikantenRoute(Context context, RouteSearchData routeSearch, IGenericProviderHandler<RouteProposal> handler) {
		super(handler);
		this.context = context;
		this.routeSearch = routeSearch;
		start();
	}
	
    @Override
	public void run() {
		try {
			/*
			 * Setup time
			 */
			final Boolean isAfter = routeSearch.arrival == 0;
			long travelTime = isAfter ? routeSearch.departure : routeSearch.arrival;
			if (travelTime == 0) {
				travelTime = Calendar.getInstance().getTimeInMillis();
			}

			/*
			 * Begin building url
			 */
			StringBuffer urlString = new StringBuffer(Trafikanten.getApiUrl() + "/Travel/GetTravelsAdvanced/?time=" + dateFormater.format(travelTime));
			boolean firstInList = true;
			
			/*
			 * Setup from stations
			 */
			// FIRSTINLIST IS TRUE HERE
			for (StationData station : routeSearch.fromStation) {
				if (firstInList) {
					urlString.append("&fromStops=");
					firstInList = false;
				} else {
					urlString.append(",");
				}
				urlString.append(station.stationId);
			}
			
			/*
			 * Setup to stations
			 */
			firstInList = true;
			for (StationData station : routeSearch.toStation) {
				if (firstInList) {
					urlString.append("&toStops=");
					firstInList = false;
				} else {
					urlString.append(",");
				}
				urlString.append(station.stationId);
			}
			
			/*
			 * Setup whether or not we're arriving before or departing after
			 */
			urlString.append("&isAfter=" + isAfter.toString());
			
			/*
			 * Disable advanced options  if they are not visible
			 */
			if (!routeSearch.advancedOptionsEnabled) {
				routeSearch.resetAdvancedOptions();
			}
			
			/*
			 * Change margin/change punish/proposals
			 */
			String changePunish = new Integer(routeSearch.changePunish).toString();
			String changeMargin = new Integer(routeSearch.changeMargin).toString();
			String proposals = new Integer(routeSearch.proposals).toString();
			CharSequence transportTypes = routeSearch.renderTransportTypesApi(routeSearch.getAPITransportArray(context));

			urlString.append("&changeMargin=" + changeMargin + "&changePunish=" + changePunish + "&proposals=" + proposals + "&transporttypes=" + transportTypes);
			
			Log.i(TAG,"Searching with url " + urlString);
			
			final InputStream stream = HelperFunctions.executeHttpRequest(context, new HttpGet(urlString.toString()), false).stream;

			/*
			 * Parse json
			 */
	    	long perfSTART = System.currentTimeMillis();
	    	Log.i(TAG,"PERF : Getting route data");
			jsonParseRouteProposal(stream);
			Log.i(TAG,"PERF : Parsing web request took " + ((System.currentTimeMillis() - perfSTART)) + "ms");
		} catch(Exception e) {
			if (e.getClass() == InterruptedException.class) {
				ThreadHandlePostExecute(null);
				return;
			}
			e.printStackTrace();
			ThreadHandlePostExecute(e);
			return;
		}
		ThreadHandlePostExecute(null);
    }
    
    /*
     * This parses the top level "route" proposals
     */
    public void jsonParseRouteProposal(InputStream stream) throws JSONException, IOException {
		final JSONArray jsonArray = new JSONArray(HelperFunctions.InputStreamToString(stream));
		final int arraySize = jsonArray.length();
		for (int i = 0; i < arraySize; i++) {
			RouteProposal travelProposal = new RouteProposal();
			jsonParseTravelStage(travelProposal, jsonArray.getJSONObject(i));
			
	        //hack:
	        WaittimeBug.onSendData(travelProposal);
	        ThreadHandlePostData(travelProposal);
		}
    }
    
    /*
     * This parses the individual travel stages of a route.
     */
    public void jsonParseTravelStage(RouteProposal travelProposal, JSONObject travelstages) throws JSONException {
    	final JSONArray jsonArray = travelstages.getJSONArray("TravelStages");
		final int arraySize = jsonArray.length();
		for (int i = 0; i < arraySize; i++) {
			final JSONObject json = jsonArray.getJSONObject(i);
			
			/*
			 * Temporary values for parsing:
			 */
            RouteData travelStage = new RouteData();
            /*
             * Parse station data:
             */
            travelStage.fromStation = jsonParseStop(json.getJSONObject("DepartureStop"));
            travelStage.toStation = jsonParseStop(json.getJSONObject("ArrivalStop"));
            
            /*
             * Start parsing local data
             */
            travelStage.departure = HelperFunctions.jsonToDate(json.getString("DepartureTime"));
            travelStage.arrival = HelperFunctions.jsonToDate(json.getString("ArrivalTime"));
            travelStage.line = json.getString("LineName");
            travelStage.destination = json.getString("Destination");
            travelStage.tourID = json.getInt("TourID");
            
            switch(json.getInt("Transportation")) {
            //Order : Walking|AirportBus|Bus|Dummy|AirportTrain|Boat|Train|Tram|Metro
            case 0: // 0 = walking
            	travelStage.transportType = R.drawable.icon_line_walk;
            	break;
            case 1: // 1 = airportbus
            case 2: // 2 = bus
            	travelStage.transportType = R.drawable.icon_line_bus;
            	break;
            case 3: // 3 = Dummy
            	travelStage.transportType = R.drawable.icon_line_bus;
            	break;
            case 4: // 4 = AirportTrain
            case 6: // 6 = Train
            	travelStage.transportType = R.drawable.icon_line_train;
            	break;
            case 5: // 5 = Boat
            	travelStage.transportType = R.drawable.icon_line_boat;
            	break;
            case 7: // 7 = Tram
            	travelStage.transportType = R.drawable.icon_line_tram;
            	break;
            case 8: // 8 = Metro
            	travelStage.transportType = R.drawable.icon_line_underground;
            	break;
            }
            
            /*
             * Alright, done parsing, give it to the parent
             */
            travelProposal.travelStageList.add(travelStage);
		}
    }
    
    /*
     * This parses departureStop/arrivalStop under the travel stages
     */
    public StationData jsonParseStop(JSONObject json) throws JSONException {
    	StationData station = new StationData();
    	
    	station.stationId = json.getInt("ID");
    	station.stopName = json.getString("Name");
		station.utmCoords[0] = json.getInt("X");
		station.utmCoords[1] = json.getInt("Y");
		station.realtimeStop = json.getBoolean("RealTimeStop");
    	
    	return station;
    }
}
