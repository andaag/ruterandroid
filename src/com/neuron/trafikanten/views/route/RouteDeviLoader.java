package com.neuron.trafikanten.views.route;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataProviders.trafikanten.TrafikantenDevi;
import com.neuron.trafikanten.dataSets.DeviData;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteDeviData;
import com.neuron.trafikanten.dataSets.RouteProposal;

public class RouteDeviLoader {
	private final static String TAG = "Trafikanten-RouteDeviLoader";
	private TrafikantenDevi deviProvider = null;
	private Context context;
	IGenericProviderHandler<Void> handler;
	private RouteDeviData deviList;
	private String deviKey;

	
	public RouteDeviLoader(Context context, RouteDeviData deviList, IGenericProviderHandler<Void> handler) {
		this.context = context;
		this.handler = handler;
		this.deviList = deviList;
	}
	
	/*
	 * Load devi, returns false if all devi is loaded
	 */
	public boolean load(ArrayList<RouteProposal> routeProposalList) {
		for (RouteProposal routeProposal : routeProposalList) {
			if (load(routeProposal)) {
				return true;
			}
		}
		Log.i(TAG,"Done loading route devi");
		return false;
	}
	
	/*
	 * Load devi for single proposal
	 */
	public boolean load(RouteProposal routeProposal) {
		for (RouteData routeData : routeProposal.travelStageList) {
			/*
			 * if tourId = 0 we're walking, no devi for that ;)
			 */
			if (routeData.tourID > 0) {
				/*
				 * TODO, come up with a better way of id'ing the different values, using a string for this is dumb.
				 *  - this is also in overviewrouteview
				 */
				deviKey = "" + routeData.fromStation.stationId + "-" + routeData.line;
				/*
				 * if the deviList contains the key we've already asked.
				 */
				if (!deviList.items.containsKey(deviKey)) {
					Log.i(TAG,"Loading route devi " + deviKey);
					loadDevi(routeData, routeData.fromStation.stationId, routeData.line);
					return true;
				} /* else {
					Log.i(TAG,"Found " + deviKey + " in cache");
				} */
				
			}
		}
		return false;
	}
	
	private void loadDevi(final RouteData routeData, int stationId, String line) {
		deviProvider = new TrafikantenDevi(context, stationId, line, new IGenericProviderHandler<DeviData>() {
			private ArrayList<DeviData> list = new ArrayList<DeviData>();

    		@Override
    		public void onExtra(int what, Object obj) {
    			/* Class has no extra data */
    		}

			@Override
			public void onData(DeviData deviData) {
				list.add(deviData);
			}

			@Override
			public void onPostExecute(Exception exception) {
				if (exception == null) {
					deviList.items.put(deviKey, list);
				}
				deviProvider = null;
				handler.onPostExecute(exception);
			}

			@Override
			public void onPreExecute() {}
    	});
	}
	
	public void kill() {
		if (deviProvider != null) {
			deviProvider.kill();
		}
		deviProvider = null;
	}
}

