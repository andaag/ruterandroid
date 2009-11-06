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

package com.neuron.trafikanten.dataProviders.trafikanten;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.SearchStationData;

import java.text.DateFormat;
import java.text.FieldPosition;

public class TrafikantenRoute implements IRouteProvider {
	private Handler handler;
	private TrafikantenRouteThread thread;
	private Resources resources;
	
	public TrafikantenRoute(Resources resources, Handler handler) {
		this.handler = handler;
		this.resources = resources;
	}

	/*
	 * Search for route
	 * @see com.neuron.trafikanten.dataProviders.IRouteProvider#Search(com.neuron.trafikanten.dataSets.RouteData)
	 */
	@Override
	public void Search(RouteData routeData) {
		Stop();
		thread = new TrafikantenRouteThread(resources, handler, routeData);
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
	public static ArrayList<RouteProposal> GetRouteList() {
		final ArrayList<RouteProposal> results = RouteHandler.travelProposalList;
		RouteHandler.travelProposalList = null;
		return results; 
	}
}

class TrafikantenRouteThread extends Thread implements Runnable {
	private final static String TAG = "Trafikanten-T-RouteThread";
	private final static SimpleDateFormat DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private final static SimpleDateFormat TIMEFORMAT = new SimpleDateFormat("HH:mm");
	private Handler handler;
	private Resources resources;
	public static RouteData routeData;
	
	public TrafikantenRouteThread(Resources resources, Handler handler, RouteData routeData) {
		this.handler = handler;
		this.resources = resources;
		TrafikantenRouteThread.routeData = routeData;
	}
	
	/*
	 * Run current thread.
	 * This function setups the url and the xmlreader, and passes data to the RouteHandler. 
	 */
	
	/*public String soapGetFrom(ArrayList<SearchStationData> stationList) throws IOException {
		String xml = "";
		for(SearchStationData station : stationList) {
			// TODO : From/airdistance/departure time has to be included atleast.
			String[] args = new String[] { new Integer(station.airDistance).toString() };
			xml = xml + HelperFunctions.mergeXmlArgument(resources, R.raw.gettravelsadvancedfrom, args);
		}
		return xml;
	}
	// TODO : soapGetTo*/
	
	public void run() {
		try {
			final SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
			final StringBuffer renderedTime = dateFormater.format(new Date(routeData.departure), new StringBuffer(), new FieldPosition(0));
			final String[] args = new String[]{ new Integer(routeData.fromStation.stationId).toString(), 
					new Integer(routeData.toStation.stationId).toString(), 
					renderedTime.toString()};
			
			final InputStream result = HelperFunctions.soapRequest(resources, R.raw.gettravelsafter, args, Trafikanten.API_URL);
			/*
			 * Setup SAXParser and XMLReader
			 */
			
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new RouteHandler(handler));
			
			reader.parse(new InputSource(result));
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
	// Results:
	public static ArrayList<RouteProposal> travelProposalList;
	// For parsing:
	private static RouteProposal travelProposal;
	private RouteData travelStage; // temporary only, these are moved to travelStageList

	
	private final static DateFormat timeParser = new SimpleDateFormat("kk:mm");

	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inTravelProposal = false;
	private boolean inTravelStage = false;
	private boolean inDepartureStop = false;
	private boolean inArrivalStop = false;
	private boolean inDepartureTime = false;
	private boolean inArrivalTime = false;
	private boolean inLineName = false;
	private boolean inDestination = false;
	//private boolean inRemarks = false; // TODO : Parse remarks
	private boolean inTransportation = false;
	private boolean inTravelTime = false;
	private boolean inWaitingTime = false;
	
	// Stop data:
	private boolean inID = false;
	private boolean inName = false;

	
	/*
	 * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	 */
	private String tmpData;
	private boolean endData;
	
	/*
	 * Date object used to check if we should increase day.
	 */
	private long searchDate;
	
	public RouteHandler(Handler handler) {
		travelProposalList = new ArrayList<RouteProposal>();
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
	
	@Override
	public void startElement(String namespaceURI, String localName, 
	              String qName, Attributes atts) throws SAXException {
		//Log.d("-StartElement", localName);
		tmpData = "";
		if (!inTravelProposal) {
			if (localName.equals("TravelProposal")) {
				inTravelProposal = true;
				travelProposal = new RouteProposal();
			}
		} else {	    
			if (!inTravelStage) {
				/*
				 * We're not in a travel proposal
				 */
		        if (localName.equals("TravelStage")) {
		            inTravelStage = true;
		            travelStage = new RouteData();
		            travelStage.fromStation = new SearchStationData();
		            travelStage.toStation = new SearchStationData();
		        } else if (localName.equals("DepartureTime")) {
		    		inDepartureTime = true;
			    } else if (localName.equals("ArrivalTime")) {
			        inArrivalTime = true;
			    /*} else if (localName.equals("Remarks")) {
		    		inRemarks = true;*/
			    }
		    } else {
		    	/*
		    	 * We're in a travel stage
		    	 */
		    	if (inDepartureStop || inArrivalStop) {
		    		/*
		    		 * We're parsing stop data.
		    		 */
		    		if (localName.equals("ID")) {
		    			inID = true;
		    		} else if (localName.equals("Name")) {
		    			inName = true;
		    		}
		    	} else if (localName.equals("DepartureStop")) {
			        inDepartureStop = true;
			    } else if (localName.equals("ArrivalStop")) {
			        inArrivalStop = true;
			    } else if (localName.equals("DepartureTime")) {
			        inDepartureTime = true;
			    } else if (localName.equals("ArrivalTime")) {
			        inArrivalTime = true;
			    } else if (localName.equals("LineName")) {
			        inLineName = true;
			    } else if (localName.equals("Destination")) {
			        inDestination = true;
			    /*} else if (localName.equals("Remarks")) {
			        inRemarks = true;*/
			    } else if (localName.equals("Transportation")) {
			        inTransportation = true;
			    } else if (localName.equals("TravelTime")) {
			        inTravelTime = true;
			    } else if (localName.equals("WaitingTime")) {
			        inWaitingTime = true;
			    }
		    }
		}
	}


	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
		//Log.d("-EndElement", localName);
		if (!inTravelProposal) return;
		
	    /*
	     * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	     * We collect all data until endElement is called, and then pass that again to characters() manually. 
	     */
    	if (tmpData.length() > 0) {
        	endData = true;
    		characters(tmpData.toCharArray(), 0, tmpData.length());
    	}
		
		
		if (localName.equals("TravelProposal")) {
			/*
			 * add the travel proposal to our master list
			 */
			inTravelProposal = false;
			travelProposalList.add(travelProposal);
		} else {
			if (!inTravelStage) {
		        if (inDepartureTime && localName.equals("DepartureTime")) {
		    		inDepartureTime = false;
			    } else if (inArrivalTime && localName.equals("ArrivalTime")) {
			        inArrivalTime = false;
			    /*} else if (inRemarks && localName.equals("Remarks")) {
		    		inRemarks = false;*/
			    }
			} else {
				/*
				 * Check if we're parsing stop data
				 */
		    	if (inDepartureStop || inArrivalStop) {
		    		/*
		    		 * We're parsing stop data.
		    		 */
		    		if (inID && localName.equals("ID")) {
		    			inID = false;
		    		} else if (inName && localName.equals("Name")) {
		    			inName = false;
					} else if (inDepartureStop && localName.equals("DepartureStop")) {
					    inDepartureStop = false;
					} else if (inArrivalStop && localName.equals("ArrivalStop")) {
					    inArrivalStop = false;
		    		}
		    	} else {
		    		/*
		    		 * We're parsing a standard TravelStage
		    		 */
					if (localName.equals("TravelStage")) {
						/*
						 * +1 travel stage, add it to the travelStageList list
						 */
						inTravelStage = false;
						travelProposal.travelStageList.add(travelStage);
					} else if (inDepartureTime && localName.equals("DepartureTime")) {
					    inDepartureTime = false;
					} else if (inArrivalTime && localName.equals("ArrivalTime")) {
					    inArrivalTime = false;
					} else if (inLineName && localName.equals("LineName")) {
					    inLineName = false;
					} else if (inDestination && localName.equals("Destination")) {
					    inDestination = false;
					/*} else if (inRemarks && localName.equals("Remarks")) {
					    inRemarks = false;*/
					} else if (inTransportation && localName.equals("Transportation")) {
					    inTransportation = false;
					} else if (inTravelTime && localName.equals("TravelTime")) {
					    inTravelTime = false;
					} else if (inWaitingTime && localName.equals("WaitingTime")) {
					    inWaitingTime = false;
					}
		    	}
			}
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		if (!inTravelProposal) return;
		
	    /*
	     * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	     * Until endElemtent is called we only collect the data.
	     */
    	if (!endData) {
    		tmpData = tmpData + new String(ch, start, length);
    		return;
    	}
    	endData = false;
		
		if (!inTravelStage) {
			/*
			 * In a travel proposal
			 */
			if (inDepartureTime) {
				Log.d("	DebugCode-TravelProposal DepartureTime",new String(ch, start, length));				
			} else if (inArrivalTime) {
				Log.d("	DebugCode-TravelProposal ArrivalTime",new String(ch, start, length));
			/*} else if (inRemarks) {
				Log.d("	DebugCode-TravelProposal Remarks",new String(ch, start, length));*/
			}
		} else {
			/*
			 * In a travel stage
			 */
		    if (inID) {
		    	Log.d("  DebugCode-TravelStage inID " + inDepartureStop + " " + inArrivalStop,new String(ch, start, length));
		    } else if (inName) {
		    	Log.d("  DebugCode-TravelStage inName " + inDepartureStop + " " + inArrivalStop,new String(ch, start, length));
		    } else if (inDepartureTime) {
		    	Log.d("  DebugCode-TravelStage DepartureTime",new String(ch, start, length));
		    } else if (inArrivalTime) {
		    	Log.d("  DebugCode-TravelStage ArrivalTime",new String(ch, start, length));	    	
		    } else if (inLineName) {
		    	Log.d("  DebugCode-TravelStage LineName",new String(ch, start, length));
		    } else if (inDestination) {
		    	Log.d("  DebugCode-TravelStage Destination",new String(ch, start, length));
		    /*} else if (inRemarks) {
		    	Log.d("  DebugCode-TravelStage Remarks",new String(ch, start, length));*/
		    } else if (inTransportation) {
		    	Log.d("  DebugCode-TravelStage Transportation",new String(ch, start, length));
		    } else if (inTravelTime) {
		    	Log.d("  DebugCode-TravelStage TravelTime",new String(ch, start, length));
		    } else if (inWaitingTime) {
		    	Log.d("  DebugCode-TravelStage WaitingTime",new String(ch, start, length));
		    }
		}
		
	    /*if (!inTravelStage) return;
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
	    	 *
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
	    	 *
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
	    }*/
	}
}
