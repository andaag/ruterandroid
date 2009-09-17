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

package com.neuron.trafikanten.dataProviders.trafikantenInternal;

import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.SearchStationData;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TrafikantenRoute implements IRouteProvider {
	private Handler handler;
	private TrafikantenRouteThread thread;
	
	public TrafikantenRoute(Handler handler) {
		this.handler = handler;
	}

	/*
	 * Search for route
	 * @see com.neuron.trafikanten.dataProviders.IRouteProvider#Search(com.neuron.trafikanten.dataSets.RouteData)
	 */
	@Override
	public void Search(RouteData routeData) {
		Stop();
		thread = new TrafikantenRouteThread(handler, routeData);
		thread.start();
		
	}

	/*
	 * Stop running thread
	 */
	@Override
	public void Stop() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}
	
	/*
	 * Get results (note that this MUST NOT be called while thread is running)
	 */
	public static ArrayList<RouteData> GetRouteList() {
		final ArrayList<RouteData> results = RouteHandler.routeList;
		RouteHandler.routeList = null;
		return results; 
	}
}

class TrafikantenRouteThread extends Thread implements Runnable {
	private final static String TAG = "TrafikantenRouteThread";
	private final static SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private final static SimpleDateFormat TIMEFORMAT = new SimpleDateFormat("HH:mm");
	private Handler handler;
	public static RouteData routeData;
	
	public TrafikantenRouteThread(Handler handler, RouteData routeData) {
		this.handler = handler;
		TrafikantenRouteThread.routeData = routeData;
	}
	
	/*
	 * Run current thread.
	 * This function setups the url and the xmlreader, and passes data to the RouteHandler. 
	 */
	
	public void run() {
		try {
			URL url = new URL("http://www5.trafikanten.no/txml/?type=4&" + 
					"fromid=" + routeData.fromStation.stationId + "&toid=" + routeData.toStation.stationId +
					"&depdate=" + DATEFORMAT.format(routeData.departure) +
					"&deptime=" + TIMEFORMAT.format(routeData.departure)					
			);
			Log.i(TAG, "Loading url " + url.toString());
			
			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new RouteHandler(handler));
			
			reader.parse(new InputSource(url.openStream()));
		} catch(Exception e) {
			/*
			 * All exceptions except thread interruptions are passed to callback.
			 */
			if (e.getClass() == InterruptedException.class)
				return;

			final Message msg = handler.obtainMessage(IRouteProvider.MESSAGE_EXCEPTION);
			final Bundle bundle = new Bundle();
			bundle.putString(IRouteProvider.KEY_EXCEPTION, e.toString());
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
}

class RouteHandler extends DefaultHandler {
	private Handler handler;
	private RouteData routeData;
	public static ArrayList<RouteData> routeList;
	private final static DateFormat timeParser = new SimpleDateFormat("kk:mm");

	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inTravelStage = false;
	private boolean inDepartureStopId = false;
	private boolean inDepartureStopName = false;
	private boolean inDepartureTime = false;
	private boolean inArrivalStopId = false;
	private boolean inArrivalStopName = false;
	private boolean inArrivalTime = false;
	private boolean inLine = false;
	private boolean inDestination = false;
	private boolean inTransportationID = false;
	private boolean inWaitingTime = false;
	
	/*
	 * Date object used to check if we should increase day.
	 */
	private long searchDate;
	
	public RouteHandler(Handler handler) {
		routeList = new ArrayList<RouteData>();
		this.handler = handler;
		searchDate = TrafikantenRouteThread.routeData.departure;
	}
	
	/*
	 * End of document, call onCompleted with complete stationList
	 */
	@Override
	public void endDocument() throws SAXException {
		handler.sendEmptyMessage(IRouteProvider.MESSAGE_DONE);
	}
	
	/*
	 * Parse the time string returned by trafikanten's route data as a Date.TimeInMillis
	 */
	private long parseTime(String time) {
		try {
			/*
			 * Parse time, and if time is 00:30 and last time was 23:30 we can increase a day.
			 */
			Date parsedDate = timeParser.parse(time);
			
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(TrafikantenRouteThread.routeData.departure);
			calendar.set(Calendar.HOUR_OF_DAY, parsedDate.getHours());
			calendar.set(Calendar.MINUTE, parsedDate.getMinutes());

			if (calendar.getTimeInMillis() < searchDate) {
				// parsedDate is before lastDate, this means a new day has dawned.
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			return calendar.getTimeInMillis();
		} catch (ParseException e) {
			final Message msg = handler.obtainMessage(IRouteProvider.MESSAGE_EXCEPTION);
			final Bundle bundle = new Bundle();
			bundle.putString(IRouteProvider.KEY_EXCEPTION, e.toString());
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
		return 0;
	}
	
	/*
	 * CODE BLOCK GENERATED BY SCRIPT scripts/route.sh
	 */
	@Override
	public void startElement(String namespaceURI, String localName, 
	              String qName, Attributes atts) throws SAXException {
	    if (!inTravelStage) {
	        if (localName.equals("TravelStage")) {
	            inTravelStage = true;
	            routeData = new RouteData();
	            routeData.fromStation = new SearchStationData();
	            routeData.toStation = new SearchStationData();
	        }
	    } else if (localName.equals("DepartureStopId")) {
	        inDepartureStopId = true;
	    } else if (localName.equals("DepartureStopName")) {
	        inDepartureStopName = true;
	    } else if (localName.equals("DepartureTime")) {
	        inDepartureTime = true;
	    } else if (localName.equals("ArrivalStopId")) {
	        inArrivalStopId = true;
	    } else if (localName.equals("ArrivalStopName")) {
	        inArrivalStopName = true;
	    } else if (localName.equals("ArrivalTime")) {
	        inArrivalTime = true;
	    } else if (localName.equals("Line")) {
	        inLine = true;
	    } else if (localName.equals("Destination")) {
	        inDestination = true;
	    } else if (localName.equals("TransportationID")) {
	        inTransportationID = true;
	    } else if (localName.equals("WaitingTime")) {
	        inWaitingTime = true;
	    }
	}


	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
	    if (!inTravelStage) return;
	    if (localName.equals("TravelStage")) {
	        /*
	         * on StopMatch we're at the end, and we need to add the station to the station list.
	         */
	        inTravelStage = false;
	        routeList.add(routeData);
	    } else if (localName.equals("DepartureStopId")) {
	        inDepartureStopId = false;
	    } else if (localName.equals("DepartureStopName")) {
	        inDepartureStopName = false;
	    } else if (localName.equals("DepartureTime")) {
	        inDepartureTime = false;
	    } else if (localName.equals("ArrivalStopId")) {
	        inArrivalStopId = false;
	    } else if (localName.equals("ArrivalStopName")) {
	        inArrivalStopName = false;
	    } else if (localName.equals("ArrivalTime")) {
	        inArrivalTime = false;
	    } else if (localName.equals("Line")) {
	        inLine = false;
	    } else if (localName.equals("Destination")) {
	        inDestination = false;
	    } else if (localName.equals("TransportationID")) {
	        inTransportationID = false;
	    } else if (localName.equals("WaitingTime")) {
	        inWaitingTime = false;
	    }
	}

	@Override
	public void characters(char ch[], int start, int length) { 
	    if (!inTravelStage) return;
	    if (inDepartureStopId) {
	    	routeData.fromStation.stationId = Integer.parseInt(new String(ch, start, length));
	    } else if (inDepartureStopName) {
	    	routeData.fromStation.stopName = new String(ch, start, length);	
	    } else if (inDepartureTime) {
	    	routeData.departure = parseTime(new String(ch, start, length));
	    } else if (inArrivalStopId) {
	    	routeData.toStation.stationId = Integer.parseInt(new String(ch, start, length));
	    } else if (inArrivalStopName) {
	    	routeData.toStation.stopName = new String(ch, start, length);
	    } else if (inArrivalTime) {
	    	routeData.arrival = parseTime(new String(ch, start, length));
	    } else if (inLine) {
	    	routeData.line = new String(ch, start, length);
	    } else if (inDestination) {
	    	routeData.destination = new String(ch, start, length);
	    } else if (inTransportationID) {
	    	/*
	    	 * Append TransportationID to Extra
	    	 */
	    	final String transportationString = new String(ch, start, length);
	    	if (transportationString.equals("G")) {
	    		routeData.transportType = IRouteProvider.TRANSPORT_WALK;
	    	} else {
	    		switch(Integer.parseInt(new String(ch, start, length))) {
	    		case 2:
	    			routeData.transportType = IRouteProvider.TRANSPORT_BUS;
	    			break;
	    		case 6:
	    			routeData.transportType = IRouteProvider.TRANSPORT_TRAIN;
	    			break;
	    		case 7:
	    			routeData.transportType = IRouteProvider.TRANSPORT_TRAM;
	    			break;
	    		case 8:
	    			routeData.transportType = IRouteProvider.TRANSPORT_SUBWAY;
	    			break;
	    		}
	    	}
	    } else if (inWaitingTime) {
	    	/*
	    	 * Parse time and add wait time in minutes to route data
	    	 */
			try {
				Date parsedDate = timeParser.parse(new String(ch, start, length));
		    	routeData.waitTime = parsedDate.getMinutes() + (parsedDate.getHours() * 60);
			} catch (ParseException e) {
				final Message msg = handler.obtainMessage(IRouteProvider.MESSAGE_EXCEPTION);
				final Bundle bundle = new Bundle();
				bundle.putString(IRouteProvider.KEY_EXCEPTION, e.toString());
				msg.setData(bundle);
				handler.sendMessage(msg);
			}
	    }
	}
	/*
	 * END CODE BLOCK GENERATED BY SCRIPT scripts/route.sh
	 */
}
