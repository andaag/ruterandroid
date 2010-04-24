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
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.methods.HttpGet;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.content.Context;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
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
	
	public TrafikantenSearch(Context context, double latitude, double longitude, IGenericProviderHandler<StationData> handler) {
		super(handler);
		this.context = context;
		this.latitude = latitude;
		this.longitude = longitude;
		start();
	}
	
	public TrafikantenSearch(Context context, String query, boolean isRealtimeStopFiltered, IGenericProviderHandler<StationData> handler) {
		super(handler);
		this.context = context;
		this.query = query;
		this.isRealtimeStopFiltered = isRealtimeStopFiltered;
		start();
	}
	
    @Override
	public void run() {
		try {
			InputStream result;
			if (query != null) {
				/*
				 * Setup URL for a normal station search query.
				 */
				if (isRealtimeStopFiltered) {
					final String urlString = "http://reis.trafikanten.no/siri/checkrealtimestop.aspx?name=" + URLEncoder.encode(query, "UTF-8");
					Log.i(TAG,"Searching with url " + urlString);
	                final URL url = new URL(urlString);
	                result = url.openStream();
				} else {
					result = HelperFunctions.soapRequest(context, R.raw.getmatches, new String[]{query}, Trafikanten.API_URL);
				}
			} else {
				/*
				 * Setup URL for coordinate search.
				 */
				final LatLng latLong = new LatLng(latitude, longitude);
				final UTMRef utmRef = latLong.toUTMRef();
				                
				final String urlString = "http://reis.trafikanten.no/topp2009/getcloseststops.aspx?x="+ (int)utmRef.getEasting() + "&y="+ (int) utmRef.getNorthing() + "&proposals=10";
				Log.i(TAG,"Searching with url " + urlString);
				final HttpGet request = new HttpGet(urlString);
				result = HelperFunctions.executeHttpRequest(context, request);
			}
			
			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new SearchHandler(this, isRealtimeStopFiltered));
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

/*
 * Search XML Parser
 */
class SearchHandler extends DefaultHandler {
	private StationData station;
	private final TrafikantenSearch parent;
	private final boolean isRealtimeStopFiltered; // if isRealtimeStopFiltered station.realtime = true always.
	
	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inPlace = false;
	private boolean inX = false;
	private boolean inY = false;
	private boolean inID = false;
	private boolean inName = false;
	private boolean inDistrict = false;
	private boolean inType = false;
	private boolean inStops = false;
	private boolean inRealTimeStop = false;
	private boolean inWalkingDistance = false;
	
	// Ignore is used to ignore anything except type Stop.
	private boolean ignore = false;
	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();
	
	public SearchHandler(TrafikantenSearch parent, boolean isRealtimeStopFiltered)
	{
		this.isRealtimeStopFiltered = isRealtimeStopFiltered;
		this.parent = parent;
	}
	
	/*
	 * This searches stopName for address, and puts address in extra (the next line).
	 * As station names are sometimes StationName (address)
	 */
	private void searchForAddress() {
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
    public void startElement(String namespaceURI, String localName, 
              String qName, Attributes atts) throws SAXException {
    	if (ignore) return;
        if (inStops) return;
        if (!inPlace) {
            if (localName.equals("Place")) {
                inPlace = true;
    			station = new StationData();
    		}
		} else {
			if (localName.equals("X")) {
			    inX = true;
			} else if (localName.equals("Y")) {
			    inY = true;
			} else if (localName.equals("ID")) {
			    inID = true;
			} else if (localName.equals("Name")) {
			    inName = true;
			} else if (localName.equals("District")) {
			    inDistrict = true;
			} else if (localName.equals("Type")) {
			    inType = true;
			} else if (localName.equals("Stops")) {
			    inStops = true;
			} else if (localName.equals("RealTimeStop")) {
				inRealTimeStop = true;
			} else if (localName.equals("WalkingDistance")) {
				inWalkingDistance = true;
			}
		}
    } 
    
    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        if (!inPlace) return;
        if (localName.equals("Place")) {
            /*
             * on StopMatch we're at the end, and we need to add the station to the station list.
             */
            inPlace = false;
            if (!ignore) {
    	        if (isRealtimeStopFiltered)
    	        	station.realtimeStop = true;
    	        parent.ThreadHandlePostData(station);
            }
            ignore = false;
        } else {
        	if (ignore) return;
        	if (inX && localName.equals("X")) {
	            inX = false;
	            station.utmCoords[0] = Integer.parseInt(buffer.toString());
	        } else if (inY && localName.equals("Y")) {
	            inY = false;
	            station.utmCoords[1] = Integer.parseInt(buffer.toString());
	        } else if (inID && localName.equals("ID")) {
	            inID = false;
	            station.stationId = Integer.parseInt(buffer.toString());
	        } else if (inName && localName.equals("Name")) {
	            inName = false;
		    	station.stopName = buffer.toString();
	    		searchForAddress();
	        } else if (inDistrict && localName.equals("District")) {
	            inDistrict = false;
		    	if (station.extra == null) {
	    			station.extra = buffer.toString();
	    		} else {
	    			station.extra = station.extra + ", " + buffer.toString();
	    		}
	        } else if (inType && localName.equals("Type")) {
	            inType = false;
		    	//Log.d("DEBUG CODE","Type : " + new String(ch, start, length) + " " + ch[0] + " " + length);
	            if (buffer.length() != 4 && !buffer.toString().equals("Stop")) {
		    		//Log.d("DEBUG CODE","  - Ignoring");
		    		ignore = true;
		    	}
	        } else if (inStops && localName.equals("Stops")) {
	        	inStops = false;
	        } else if (inRealTimeStop && localName.equals("RealTimeStop")) {
	        	inRealTimeStop = false;
	        	// TODO : Should get api clearification here, this should not be neccesary...
	        	station.realtimeStop = buffer.toString().toLowerCase().equals("true");
	        } else if (inWalkingDistance && localName.equals("WalkingDistance")) {
	        	inWalkingDistance = false;
	        	station.walkingDistance = Integer.parseInt(buffer.toString());
	        }
        }
        buffer.setLength(0);
    }
    
    @Override
    public void characters(char ch[], int start, int length) {
    	if (ignore) return;
    	if (inX || inY || inID || inName || inDistrict || inType || inRealTimeStop || inWalkingDistance) {
    		buffer.append(ch, start, length);
    	}
    }
}