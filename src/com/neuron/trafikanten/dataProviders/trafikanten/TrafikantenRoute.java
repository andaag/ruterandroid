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

import java.io.InputStream;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.res.Resources;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IRouteProvider;
import com.neuron.trafikanten.dataProviders.IRouteProvider.RouteProviderHandler;
import com.neuron.trafikanten.dataSets.RouteData;
import com.neuron.trafikanten.dataSets.RouteProposal;
import com.neuron.trafikanten.dataSets.SearchStationData;

public class TrafikantenRoute implements IRouteProvider {
	private RouteProviderHandler handler;
	private TrafikantenRouteThread thread;
	private Resources resources;
	
	public TrafikantenRoute(Resources resources, RouteProviderHandler handler) {
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
}

class TrafikantenRouteThread extends Thread implements Runnable {
	//private final static String TAG = "Trafikanten-T-RouteThread";
	private RouteProviderHandler handler;
	private Resources resources;
	public static RouteData routeData;
	
	public TrafikantenRouteThread(Resources resources, RouteProviderHandler handler, RouteData routeData) {
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

			/*
			 * Send exception
			 */
			final Exception sendException = e;
			handler.post(new Runnable() {
				@Override
				public void run() {
					handler.onError(sendException);
				}
			});
		}
	}
}

class RouteHandler extends DefaultHandler {
	private RouteProviderHandler handler;
	// For parsing:
	private static RouteProposal travelProposal;
	private RouteData travelStage; // temporary only, these are moved to travelStageList

	
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");

	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inTravelProposal = false;
	private boolean inTravelStage = false;
	private boolean inDepartureStop = false;
	private boolean inArrivalStop = false;
	private boolean inActualStop = false;
	private boolean inDepartureTime = false;
	private boolean inArrivalTime = false;
	private boolean inLineName = false;
	private boolean inDestination = false;
	private boolean inTransportation = false;
	private boolean inWaitingTime = false;
	
	// Stop data:
	private boolean inID = false;
	private boolean inName = false;
	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();

	public RouteHandler(RouteProviderHandler handler) {
		this.handler = handler;
	}
	
	
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
	}



	/*
	 * End of document, call onCompleted with complete stationList
	 */
	@Override
	public void endDocument() throws SAXException {
		handler.post(new Runnable() {
			@Override
			public void run() {
				handler.onFinished();			
			}
		});
	}
	
	/*
	 * Parse DateTime and convert ParseException to SAXException
	 */
	private long parseDateTime(String dateTime) throws SAXException {
		try {
			return dateFormater.parse(dateTime).getTime();
		} catch (ParseException e) {
			throw new SAXException(e);
		}
	}

	
	@Override
	public void startElement(String namespaceURI, String localName, 
	              String qName, Attributes atts) throws SAXException {
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
			    }
		    } else {
		    	/*
		    	 * We're in a travel stage
		    	 */
		    	if (inActualStop) return;
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
			    } else if (localName.equals("Transportation")) {
			        inTransportation = true;
			    } else if (localName.equals("WaitingTime")) {
			        inWaitingTime = true;
			    } else if (localName.equals("inActualStop")) {
			    	inActualStop = false;
			    }
		    }
		}
	}


	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (!inTravelProposal) return;
		
		if (localName.equals("TravelProposal")) {
			/*
			 * add the travel proposal to our master list
			 */
			inTravelProposal = false;
			
	        final RouteProposal sendData = travelProposal;
			handler.post(new Runnable() {
				@Override
				public void run() {
					handler.onData(sendData);	
				}
			});
			
		} else {
			if (!inTravelStage) {
		        if (inDepartureTime && localName.equals("DepartureTime")) {
		    		inDepartureTime = false;
		    		// TODO : Parse this
			    } else if (inArrivalTime && localName.equals("ArrivalTime")) {
			        inArrivalTime = false;
			        // TODO : Parse this
			    }
			} else {
				/*
				 * Check if we're parsing stop data
				 */
				if (inActualStop) {
					if (localName.equals("ActualStop")) {
						inActualStop = false;
					}
				} else if (inDepartureStop || inArrivalStop) {
		    		/*
		    		 * We're parsing stop data.
		    		 */
		    		if (inID && localName.equals("ID")) {
		    			inID = false;
				    	if (inDepartureStop) {
				    		travelStage.fromStation.stationId = Integer.parseInt(buffer.toString());
				    	} else {
				    		travelStage.toStation.stationId = Integer.parseInt(buffer.toString());
				    	}
		    		} else if (inName && localName.equals("Name")) {
		    			inName = false;
				    	if (inDepartureStop) {
				    		travelStage.fromStation.stopName = buffer.toString();
				    	} else {
				    		travelStage.toStation.stopName = buffer.toString();
				    	}
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
						/*Log.d("DEBUG CODE", "Adding travelstage : " + travelStage.line + " " + travelStage.destination + "\n" + 
								travelStage.departure + " " + travelStage.arrival + " " + travelStage.waitTime);*/
						travelProposal.travelStageList.add(travelStage);
					} else if (inDepartureTime && localName.equals("DepartureTime")) {
					    inDepartureTime = false;
					    if (buffer.length() > 0)
					    	travelStage.departure = parseDateTime(buffer.toString());
					} else if (inArrivalTime && localName.equals("ArrivalTime")) {
					    inArrivalTime = false;
					    if (buffer.length() > 0)
					    	travelStage.arrival = parseDateTime(buffer.toString());
					} else if (inLineName && localName.equals("LineName")) {
					    inLineName = false;
				    	travelStage.line = buffer.toString();
					} else if (inDestination && localName.equals("Destination")) {
					    inDestination = false;
				    	travelStage.destination = buffer.toString();
					} else if (inTransportation && localName.equals("Transportation")) {
					    inTransportation = false;
				    	final String transportationString = buffer.toString();
				    	travelStage.transportType = IRouteProvider.TRANSPORT_UNKNOWN;
				    	if (transportationString.equals("Walking")) {
				    		travelStage.transportType = IRouteProvider.TRANSPORT_WALK;
				    	} else if (transportationString.equals("Bus")) {
				    		travelStage.transportType = IRouteProvider.TRANSPORT_BUS;
				    	} else if (transportationString.equals("Train")) {
				    		travelStage.transportType = IRouteProvider.TRANSPORT_TRAIN;
				    	} else if (transportationString.equals("Tram")) {
				    		travelStage.transportType = IRouteProvider.TRANSPORT_TRAM;
				    	} else if (transportationString.equals("Metro")) {
				    		// TODO : Parse this
				    	} else if (transportationString.equals("Boat")) {
				    		// TODO : Parse this
				    	} else if (transportationString.equals("AirportTrain")) {
				    		// TODO : Parse this
				    	} else if (transportationString.equals("AirportBus")) {
				    		// TODO : Parse this
				    	}
					} else if (inWaitingTime && localName.equals("WaitingTime")) {
					    inWaitingTime = false;
				    	/*
				    	 * Parse time and add wait time in minutes to route data
				    	 */
					    if (buffer.length() > 0) {
							try {
								Date parsedDate = dateFormater.parse(buffer.toString());
								travelStage.waitTime = parsedDate.getMinutes() + (parsedDate.getHours() * 60);
							} catch (ParseException e) {
								throw new SAXException(e);
							}
					}
					}
		    	}
			}
		}
		buffer.setLength(0);
	}

	@Override
	public void characters(char ch[], int start, int length) {
		if (inDepartureTime || inArrivalTime || inID || inName || inDepartureTime || inArrivalTime ||
				inLineName || inDestination || inTransportation || inWaitingTime) {
			buffer.append(ch,start,length);
		}
	}
}
