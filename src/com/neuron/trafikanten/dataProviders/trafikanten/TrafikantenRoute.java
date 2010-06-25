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
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;

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
	private final Context context;
	private final RouteSearchData routeSearch;
	
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
			final boolean travelAt = routeSearch.arrival == 0;
			long travelTime = travelAt ? routeSearch.departure : routeSearch.arrival;
			if (travelTime == 0) {
				travelTime = Calendar.getInstance().getTimeInMillis();
			}
			final SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
			final StringBuffer travelTimeString = dateFormater.format(new Date(travelTime), new StringBuffer(), new FieldPosition(0));
			
			/*
			 * Setup from stations
			 */
			final StringBuffer soapFromStation = new StringBuffer();
			for (StationData station : routeSearch.fromStation) {
				soapFromStation.append(HelperFunctions.mergeXmlArgument(context.getResources(), R.raw.gettravelsadvancedstation, 
						new String[] {new Integer(station.walkingDistance).toString(), new Integer(station.stationId).toString() } ));
			}
			/*
			 * Setup to stations
			 */
			final StringBuffer soapToStation = new StringBuffer();
			for (StationData station : routeSearch.toStation) {
				soapToStation.append(HelperFunctions.mergeXmlArgument(context.getResources(), R.raw.gettravelsadvancedstation, 
						new String[] {new Integer(station.walkingDistance).toString(), new Integer(station.stationId).toString() } ));
			}
			
			/*
			 * Disable advanced options  if they are not visible
			 */
			if (!routeSearch.advancedOptionsEnabled) {
				routeSearch.resetAdvancedOptions();
			}
			
			/*
			 * Change margin/change punish/proposals
			 */
			String changeMargin = new Integer(routeSearch.changeMargin).toString();
			String proposals = new Integer(routeSearch.proposals).toString();
			
			//For now using default values
			String changePunish = "2";
			String walkingFactor = "100";
			
			/*
			 * Setup args to gettravelsadvanced and send soap request
			 */
			final String[] args = new String[]{Boolean.toString(travelAt), travelTimeString.toString(), 
					soapFromStation.toString(), soapToStation.toString(),
					changeMargin, changePunish, proposals, walkingFactor};
			final InputStream result = HelperFunctions.soapRequest(context, R.raw.gettravelsadvanced, args, Trafikanten.API_URL);

			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new RouteHandler(this));
			
			reader.parse(new InputSource(result));
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

class RouteHandler extends DefaultHandler {
	private final TrafikantenRoute parent;
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
	private boolean inX = false;
	private boolean inY = false;
	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();

	public RouteHandler(TrafikantenRoute parent) {
		this.parent = parent;
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
		            travelStage.fromStation = new StationData();
		            travelStage.toStation = new StationData();
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
		    		} else if (localName.equals("X")) {
		    			inX = true;
		    		} else if (localName.equals("Y")) {
		    			inY = true;
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
			
	        //hack:
	        WaittimeBug.onSendData(travelProposal);
	        parent.ThreadHandlePostData(travelProposal);
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
		    		} else if (inX && localName.equals("X")) {
		    			inX = false;
				    	if (inDepartureStop) {
				    		travelStage.fromStation.utmCoords[0] = Integer.parseInt(buffer.toString());
				    	} else {
				    		travelStage.toStation.utmCoords[0] = Integer.parseInt(buffer.toString());
				    	}
		    		} else if (inY && localName.equals("Y")) {
		    			inY = false;
				    	if (inDepartureStop) {
				    		travelStage.fromStation.utmCoords[1] = Integer.parseInt(buffer.toString());
				    	} else {
				    		travelStage.toStation.utmCoords[1] = Integer.parseInt(buffer.toString());
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
				    	travelStage.transportType = R.drawable.icon_unknown;
				    	if (transportationString.equals("Walking")) {
				    		travelStage.transportType = R.drawable.icon_line_walk;
				    	} else if (transportationString.equals("Bus")) {
				    		travelStage.transportType = R.drawable.icon_line_bus;
				    	} else if (transportationString.equals("Train")) {
				    		travelStage.transportType = R.drawable.icon_line_train;
				    	} else if (transportationString.equals("Tram")) {
				    		travelStage.transportType = R.drawable.icon_line_tram;
				    	} else if (transportationString.equals("Metro")) {
				    		travelStage.transportType = R.drawable.icon_line_underground;
				    	} else if (transportationString.equals("Boat")) {
				    		travelStage.transportType = R.drawable.icon_line_boat;
				    	} else if (transportationString.equals("AirportTrain")) {
				    		travelStage.transportType = R.drawable.icon_line_train;
				    	} else if (transportationString.equals("AirportBus")) {
				    		travelStage.transportType = R.drawable.icon_line_bus;
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
		if (inDepartureTime || inArrivalTime || inID || inName || inX || inY || inDepartureTime || inArrivalTime ||
				inLineName || inDestination || inTransportation || inWaitingTime) {
			buffer.append(ch,start,length);
		}
	}
}
