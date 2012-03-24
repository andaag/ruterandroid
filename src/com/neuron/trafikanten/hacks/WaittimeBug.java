package com.neuron.trafikanten.hacks;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;

/*
 * HACK : This class exists to track waittime in trafikantenRoute, this shouldn't be needed as trafikanten already sends this information, only as empty elements.
 */
public class WaittimeBug {
	public static void onSendData(RouteProposal travelStage) {
		if (travelStage.travelStageList.size() == 0) 
			return;
		RouteData previousRouteData = null;
		int days = 0;
		for (RouteData routeData : travelStage.travelStageList) {
			if (previousRouteData != null) {
				if (routeData.departure < previousRouteData.arrival) {
					/*
					 * a day has passed.
					 */
					days++;
				}
				routeData.departure = routeData.departure + (days * (HelperFunctions.HOUR * 24));
				routeData.arrival = routeData.arrival + (days * (HelperFunctions.HOUR * 24));
				
				routeData.waitTime = (int)((routeData.departure - previousRouteData.arrival) / HelperFunctions.MINUTE);
			}
			previousRouteData = routeData;
		}
	}

}
