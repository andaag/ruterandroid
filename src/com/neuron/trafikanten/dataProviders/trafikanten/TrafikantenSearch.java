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

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.neuron.trafikanten.dataProviders.ISearchProvider;
import com.neuron.trafikanten.dataSets.SearchStationData;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.UTMRef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TrafikantenSearch implements ISearchProvider {
	private Handler handler;
	private TrafikantenSearchThread thread;
	
	public TrafikantenSearch(Handler handler) {
		this.handler = handler;
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
		thread = new TrafikantenSearchThread(handler, query);
		thread.start();
	}
	
	/*
	 * Initiate a search of coordinates
	 */
	@Override
	public void Search(double latitude, double longitude) {
		Stop();
		thread = new TrafikantenSearchThread(handler, latitude, longitude);
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
	private static final String TAG = "TrafikantenSearchThread";
	private Handler handler;
	private String query;
	private double latitude;
	private double longitude;
	
	public TrafikantenSearchThread(Handler handler, String query) {
		this.handler = handler;
		this.query = query;
	}
	
	public TrafikantenSearchThread(Handler handler, double latitude, double longitude) {
		this.handler = handler;
		
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	/*
	 * Run current thread.
	 * This function setups the url and the xmlreader, and passes data to the SearchHandler. 
	 */
	public void run() {
		try {
			URL url;
			if (query != null) {
				/*
				 * Setup URL for a normal station search query.
				 */
				url = new URL("http://www5.trafikanten.no/txml/?type=1&stopname=" + URLEncoder.encode(query,"UTF-8"));
			} else {
				/*
				 * Setup URL for coordinate search.
				 */
				final LatLng latLong = new LatLng(latitude, longitude);
				final UTMRef utmRef = latLong.toUTMRef();
				url = new URL("http://www5.trafikanten.no/txml/?type=2&x=" + (int) utmRef.getEasting() + "&y=" + (int) utmRef.getNorthing());			
			}
			Log.i(TAG,"Opening url " + url.toString());
			
			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new SearchHandler(handler));
			reader.parse(new InputSource(url.openStream()));
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
	private boolean inStopMatch = false;
	private boolean inStopName = false;
	private boolean inDistrict = false;
	private boolean inStationId = false;
	private boolean inXCoordinate = false;
	private boolean inYCoordinate = false;
	private boolean inAirDistance = false;
	
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
    	if (!inStopMatch) {
    		if (localName.equals("StopMatch")) {
    			inStopMatch = true;
    			station = new SearchStationData();
    		}
    	} else if (localName.equals("StopName")) {
    			inStopName = true;
    	} else if (localName.equals("District")) {
			inDistrict = true;
    	} else if (localName.equals("fromid")) {
    		inStationId = true;
    	} else if (localName.equals("XCoordinate")) {
    		inXCoordinate = true;
    	} else if (localName.equals("YCoordinate")) {
    		inYCoordinate = true;
    	} else if (localName.equals("AirDistance")) {
    		inAirDistance = true;
    	}
    } 
    
    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
    	if (!inStopMatch) return;
    	if (localName.equals("StopMatch")) {
    		/*
    		 * on StopMatch we're at the end, and we need to add the station to the station list.
    		 */
    		inStopMatch = false;
    		stationList.add(station);
    	} else if (localName.equals("StopName")) {
    		inStopName = false;
		} else if (localName.equals("District")) {
			inDistrict = false;
		} else if (localName.equals("fromid")) {
			inStationId = false;
		} else if (localName.equals("XCoordinate")) {
			inXCoordinate = false;
		} else if (localName.equals("YCoordinate")) {
			inYCoordinate = false;
		} else if (localName.equals("AirDistance")) {
			inAirDistance = false;
		}
    }
    
    @Override
    public void characters(char ch[], int start, int length) { 
    	if (!inStopMatch) return;
    	if (inStopName) {
    		station.stopName = new String(ch, start, length);
    		searchForAddress();
    	} else if (inDistrict) {
    		if (station.extra == null) {
    			station.extra = new String(ch, start, length);
    		} else {
    			station.extra = station.extra + ", " + new String(ch, start, length);
    			
    		}
    	} else if (inStationId) {
    		station.stationId = Integer.parseInt(new String(ch, start, length));
    	} else if (inXCoordinate) {
    		station.utmCoords[0] = Integer.parseInt(new String(ch, start, length));
    	} else if (inYCoordinate) {
    		station.utmCoords[1] = Integer.parseInt(new String(ch, start, length));
    	} else if (inAirDistance) {
    		station.setAirDistance(Integer.parseInt(new String(ch, start, length)));
    	}
    }
}