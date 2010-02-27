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
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.content.res.Resources;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.ISearchProvider;
import com.neuron.trafikanten.dataProviders.ISearchProvider.SearchProviderHandler;
import com.neuron.trafikanten.dataSets.StationData;

public class TrafikantenSearch implements ISearchProvider {
	private static final String TAG = "Trafikanten-TrafikantenSearch";
	private SearchProviderHandler handler;
	private Resources resources;
	private TrafikantenSearchThread thread;
	
	public TrafikantenSearch(Resources resources, SearchProviderHandler handler) {
		this.handler = handler;
		this.resources = resources;
	}
	
	/*
	 * Kill off running thread to stop current search. 
	 */
	@Override
	public void Stop() {
		if (thread != null) {
			thread.interrupt();
			try {thread.join();
			} catch (InterruptedException e) {}
			thread = null;
		}
	}

	/*
	 * Initiate a search of string query
	 */
	@Override
	public void Search(String query, boolean isRealtimeStopFiltered) {
		Stop();
		Log.i(TAG,"Searching for station " + query);
		thread = new TrafikantenSearchThread(resources, isRealtimeStopFiltered, handler, query);
		handler.onStarted();
		thread.start();
	}
	
	/*
	 * Initiate a search of coordinates
	 */
	@Override
	public void Search(double latitude, double longitude) {
		Stop();
		Log.i(TAG,"Searching for coordinates " + latitude + " " + longitude);
		thread = new TrafikantenSearchThread(resources, handler, latitude, longitude);
		handler.onStarted();
		thread.start();
	}
	
	/*
	 * Call a normal stop on finalize
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		Stop();
		super.finalize();
	}
}


class TrafikantenSearchThread extends Thread implements Runnable {
	private static final String TAG = "Trafikanten-T-SearchThread";
	private SearchProviderHandler handler;
	private Resources resources;
	private String query;
	private boolean isRealtimeStopFiltered;
	private double latitude;
	private double longitude;
	
	public TrafikantenSearchThread(Resources resources, boolean isRealtimeStopFiltered, SearchProviderHandler handler, String query) {
		this.handler = handler;
		this.resources = resources;
		this.query = query;
		this.isRealtimeStopFiltered = isRealtimeStopFiltered;
	}
	
	public TrafikantenSearchThread(Resources resources, SearchProviderHandler handler, double latitude, double longitude) {
		this.handler = handler;
		this.resources = resources;
		
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	/*
	 * Run current thread.
	 * This function setups the url and the xmlreader, and passes data to the SearchHandler. 
	 */
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
					result = HelperFunctions.soapRequest(resources, R.raw.getmatches, new String[]{query}, Trafikanten.API_URL);
				}
			} else {
				/*
				 * Setup URL for coordinate search.
				 */
				final LatLng latLong = new LatLng(latitude, longitude);
				final UTMRef utmRef = latLong.toUTMRef();
				                
				final String urlString = "http://reis.trafikanten.no/topp2009/getcloseststops.aspx?x="+ (int)utmRef.getEasting() + "&y="+ (int) utmRef.getNorthing() + "&proposals=10";
				Log.i(TAG,"Searching with url " + urlString);
                final URL url = new URL(urlString);
                result = url.openStream();
			}
			
			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new SearchHandler(handler, isRealtimeStopFiltered));
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

/*
 * Search XML Parser
 */
class SearchHandler extends DefaultHandler {
	private StationData station;
	private SearchProviderHandler handler;
	
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
	private boolean isRealtimeStopFiltered; // if isRealtimeStopFiltered station.realtime = true always.
	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();
	
	public SearchHandler(SearchProviderHandler handler, boolean isRealtimeStopFiltered)
	{
		this.isRealtimeStopFiltered = isRealtimeStopFiltered;
		this.handler = handler;
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
    	        final StationData sendData = station;
    	        if (isRealtimeStopFiltered)
    	        	sendData.realtimeStop = true;
    			handler.post(new Runnable() {
    				@Override
    				public void run() {
    					handler.onData(sendData);	
    				}
    			});
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