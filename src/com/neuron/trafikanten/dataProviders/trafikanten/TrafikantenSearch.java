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
import java.util.ArrayList;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.ISearchProvider;
import com.neuron.trafikanten.dataSets.SearchStationData;

public class TrafikantenSearch implements ISearchProvider {
	private Handler handler;
	private Resources resources;
	private TrafikantenSearchThread thread;
	
	public TrafikantenSearch(Resources resources, Handler handler) {
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
			thread = null;
		}
	}

	/*
	 * Initiate a search of string query
	 */
	@Override
	public void Search(String query) {
		Stop();
		thread = new TrafikantenSearchThread(resources, handler, query);
		thread.start();
	}
	
	/*
	 * Initiate a search of coordinates
	 */
	@Override
	public void Search(double latitude, double longitude) {
		Stop();
		thread = new TrafikantenSearchThread(resources, handler, latitude, longitude);
		thread.start();
	}

	/*
	 * Get results (note that this MUST NOT be called while thread is running)
	 */
	public static ArrayList<SearchStationData> GetStationList() {
		final ArrayList<SearchStationData> results = SearchHandler.stationList;
		SearchHandler.stationList = null;
		return results; 
	}
}


class TrafikantenSearchThread extends Thread implements Runnable {
	private static final String TAG = "Trafikanten-T-SearchThread";
	private Handler handler;
	private Resources resources;
	private String query;
	private double latitude;
	private double longitude;
	
	public TrafikantenSearchThread(Resources resources, Handler handler, String query) {
		this.handler = handler;
		this.resources = resources;
		this.query = query;
	}
	
	public TrafikantenSearchThread(Resources resources, Handler handler, double latitude, double longitude) {
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
            	result = HelperFunctions.soapRequest(resources, R.raw.getmatches, new String[]{query}, Trafikanten.API_URL);
			} else {
				/*
				 * Setup URL for coordinate search.
				 */
				final LatLng latLong = new LatLng(latitude, longitude);
				final UTMRef utmRef = latLong.toUTMRef();
				
                final String x = new Integer((int)utmRef.getEasting()).toString();
                final String y = new Integer((int)utmRef.getNorthing()).toString();
                
                result = HelperFunctions.soapRequest(resources, R.raw.getcloseststops, new String[]{x,y}, Trafikanten.API_URL);
			}
			
			/*
			 * Setup SAXParser and XMLReader
			 */
			
			/*
			result.wait(2000);
			String debug = "";
			while (true) {
				int b = result.read();
				if (b < 1) break; // EOF
				debug += String.valueOf( ( char )b );
				
			}
			Log.d("DEBUG CODE", "RAW : " + debug);*/
			
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new SearchHandler(handler));
			reader.parse(new InputSource(result));
		} catch(Exception e) {
			/*
			 * All exceptions except thread interruptions are passed to callback.
			 */
			if (e.getClass() == InterruptedException.class)
				return;
		
			final Message msg = handler.obtainMessage(ISearchProvider.MESSAGE_EXCEPTION);
			final Bundle bundle = new Bundle();
			bundle.putString(ISearchProvider.KEY_EXCEPTION, e.toString());
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
}

/*
 * Search XML Parser
 */
class SearchHandler extends DefaultHandler {
	private SearchStationData station;
	public static ArrayList<SearchStationData> stationList;
	private Handler handler;
	
	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inPlace = false;
	private boolean inZone = false;
	private boolean inX = false;
	private boolean inY = false;
	private boolean inID = false;
	private boolean inName = false;
	private boolean inDistrict = false;
	private boolean inType = false;
	private boolean inStops = false;
	
	// Ignore is used to ignore anything except type Stop.
	private boolean ignore = false;
	
	public SearchHandler(Handler handler)
	{
		stationList = new ArrayList<SearchStationData>();
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
		handler.sendEmptyMessage(ISearchProvider.MESSAGE_DONE);
	}
	

    @Override 
    public void startElement(String namespaceURI, String localName, 
              String qName, Attributes atts) throws SAXException {
    	if (ignore) return;
    	if (inStops) return;
        if (!inPlace) {
            if (localName.equals("Place")) {
                inPlace = true;
    			station = new SearchStationData();
    		}
		} else {
			if (localName.equals("Zone")) {
			    inZone = true;
			} else if (localName.equals("X")) {
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
            if (!ignore)
            	stationList.add(station);
            ignore = false;
        } else {
        	if (ignore) return;
        	if (inZone && localName.equals("Zone")) {
	            inZone = false;
	        } else if (inX && localName.equals("X")) {
	            inX = false;
	        } else if (inY && localName.equals("Y")) {
	            inY = false;
	        } else if (inID && localName.equals("ID")) {
	            inID = false;
	        } else if (inName && localName.equals("Name")) {
	            inName = false;
	        } else if (inDistrict && localName.equals("District")) {
	            inDistrict = false;
	        } else if (inType && localName.equals("Type")) {
	            inType = false;
	        } else if (inStops && localName.equals("Stops")) {
	        	inStops = false;
	        }
        }
    }
    
    @Override
    public void characters(char ch[], int start, int length) {
    	if (ignore) return;
    	//Log.d("DEBUG CODE", "Recieved : " + new String(ch, start, length));
        if (!inPlace) return;
	    if (inZone) {
	    	// Zone is  currently ignored and not shown.
	    } else if (inX) {
	    	station.utmCoords[0] = Integer.parseInt(new String(ch, start, length));
	    } else if (inY) {
	    	station.utmCoords[1] = Integer.parseInt(new String(ch, start, length));
	    } else if (inID) {
	    	station.stationId = Integer.parseInt(new String(ch, start, length));
	    } else if (inName) {
	    	station.stopName = new String(ch, start, length);
    		searchForAddress();
	    } else if (inDistrict) {
	    	if (station.extra == null) {
    			station.extra = new String(ch, start, length);
    		} else {
    			station.extra = station.extra + ", " + new String(ch, start, length);
    		}
	    } else if (inType) {
	    	Log.d("DEBUG CODE","Type : " + new String(ch, start, length) + " " + ch[0] + " " + length);
	    	if (length != 4 && ch[0] != 'S') {
	    		Log.d("DEBUG CODE","  - Ignoring");
	    		ignore = true;
	    	}
	    }
	    //station.setAirDistance(Integer.parseInt(new String(ch, start, length))); // TODO

    }
}