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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.neuron.trafikanten.dataProviders.IRealtimeProvider;
import com.neuron.trafikanten.dataSets.RealtimeData;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TrafikantenRealtime implements IRealtimeProvider {
	private Handler handler;
	private TrafikantenRealtimeThread thread;
	
	public TrafikantenRealtime(Handler handler) {
		this.handler = handler;
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
	 * Fetch Realtime information for stationId
	 * @see com.neuron.trafikanten.dataProviders.IRealtimeProvider#Fetch(int)
	 */
	@Override
	public void Fetch(int stationId) {
		Stop();
		thread = new TrafikantenRealtimeThread(handler, stationId);
		thread.start();
	}

	/*
	 * Get results (note that this MUST NOT be called while thread is running)
	 */
	public static ArrayList<RealtimeData> GetRealtimeList() {
		final ArrayList<RealtimeData> results = RealtimeHandler.realtimeList;
		RealtimeHandler.realtimeList = null;
		return results; 
	}
}

class TrafikantenRealtimeThread extends Thread implements Runnable {
	private static final String TAG = "TrafikantenRealtimeThread";
	private Handler handler;
	private int stationId;	
	
	public TrafikantenRealtimeThread(Handler handler, int stationId) {
		this.handler = handler;
		this.stationId = stationId;
	}
	
	/*
	 * Run current thread.
	 * This function setups the url and the xmlreader, and passes data to the RealtimeHandler. 
	 */
	
	public void run() {
		try {
			URL url = new URL("http://195.1.22.228/topp2009/siri/sm.aspx?id=" + stationId);
			Log.i(TAG,"Realtime url : " + url);
			
			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new RealtimeHandler(handler));
			
			final InputSource inputSource = new InputSource(url.openStream());
			inputSource.setEncoding("iso-8859-1");
			reader.parse(inputSource);
		} catch(Exception e) {
			/*
			 * All exceptions except thread interruptions are passed to callback.
			 */
			if (e.getClass() == InterruptedException.class)
				return;

			final Message msg = handler.obtainMessage(IRealtimeProvider.MESSAGE_EXCEPTION);
			final Bundle bundle = new Bundle();
			bundle.putString(IRealtimeProvider.KEY_EXCEPTION, e.toString());
			msg.setData(bundle);
			handler.sendMessage(msg);
		}
	}
}

/*
 * Realtime XML Parser
 */
class RealtimeHandler extends DefaultHandler {
	private RealtimeData data;
	public static ArrayList<RealtimeData> realtimeList;
	private final static DateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS");
	
	private Handler handler;
	
	/*
	 * Temporary variables for parsing. 
	 */
    private boolean inDISDeviation = false;
    private boolean inLineText = true;
    private boolean inDestinationStop = true;
    private boolean inTripStatus = true;
    private boolean inScheduledDISArrivalTime = true;
    private boolean inExpectedDISArrivalTime = true;
    private boolean inScheduledDISDepartureTime = true;
    private boolean inExpectedDISDepartureTime = true;
    private boolean inStopPosition = true;
	
	/*
	 * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	 */
	private String tmpData;
	private boolean endData;
	
	public RealtimeHandler(Handler handler)
	{
		realtimeList = new ArrayList<RealtimeData>();
		this.handler = handler;
	}
	
	/*
	 * Add data to realtimeList
	 */
	private void addData() {
		/*
		 * First search through and check if any previous match has the same line and destination.
		 * If it does, add us to that instead of making a huge list of duplicates.
		 */
		for (RealtimeData d : realtimeList) {
			if (d.line.equals(data.line) && d.destination.equals(data.destination)) {
				d.arrivalList.add(data);
				return;
			}
		}
		
		realtimeList.add(data);
	}
	
	/*
	 * End of document, call onCompleted with complete realtimeList
	 */
	@Override
	public void endDocument() throws SAXException {
		handler.sendEmptyMessage(IRealtimeProvider.MESSAGE_DONE);
	}
	
	/*
	 * Parse DateTime and convert ParseException to SAXException
	 * TODO FUTURE : Parse DateTime and convert ParseException to SAXException, Should be a better way of doing this?
	 */
	private long parseDateTime(String dateTime) throws SAXException {
		try {
			return dateFormater.parse(dateTime).getTime();
		} catch (ParseException e) {
			throw new SAXException(e);
		}
	}
	
	/*
	 * CODE BLOCK GENERATED BY SCRIPT scripts/realtime.sh
	 */
	@Override
	public void startElement(String namespaceURI, String localName, 
	              String qName, Attributes atts) throws SAXException {
	    if (!inDISDeviation) {
	        if (localName.equals("DISDeviation")) {
	            inDISDeviation = true;
	            data = new RealtimeData();
	        }
	    } else if (localName.equals("LineText")) {
	        inLineText = true;
	    } else if (localName.equals("DestinationStop")) {
	        inDestinationStop = true;
	    } else if (localName.equals("TripStatus")) {
	        inTripStatus = true;
	    } else if (localName.equals("ScheduledDISArrivalTime")) {
	        inScheduledDISArrivalTime = true;
	    } else if (localName.equals("ExpectedDISArrivalTime")) {
	        inExpectedDISArrivalTime = true;
	    } else if (localName.equals("ScheduledDISDepartureTime")) {
	        inScheduledDISDepartureTime = true;
	    } else if (localName.equals("ExpectedDISDepartureTime")) {
	        inExpectedDISDepartureTime = true;
	    } else if (localName.equals("StopPosition")) {
	        inStopPosition = true;
	    }
	}


	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
	    if (!inDISDeviation) return;
	    
	    /*
	     * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	     * We collect all data until endElement is called, and then pass that again to characters() manually. 
	     */
    	if (tmpData.length() > 0) {
        	endData = true;
    		characters(tmpData.toCharArray(), 0, tmpData.length());
    	}
	    
	    if (localName.equals("DISDeviation")) {
	        /*
	         * on StopMatch we're at the end, and we need to add the station to the station list.
	         */
	        inDISDeviation = false;
	        addData();
	    } else if (localName.equals("LineText")) {
	        inLineText = false;
	    } else if (localName.equals("DestinationStop")) {
	        inDestinationStop = false;
	    } else if (localName.equals("TripStatus")) {
	        inTripStatus = false;
	    } else if (localName.equals("ScheduledDISArrivalTime")) {
	        inScheduledDISArrivalTime = false;
	    } else if (localName.equals("ExpectedDISArrivalTime")) {
	        inExpectedDISArrivalTime = false;
	    } else if (localName.equals("ScheduledDISDepartureTime")) {
	        inScheduledDISDepartureTime = false;
	    } else if (localName.equals("ExpectedDISDepartureTime")) {
	        inExpectedDISDepartureTime = false;
	    } else if (localName.equals("StopPosition")) {
	        inStopPosition = false;
	    }
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException { 
	    if (!inDISDeviation) return;
	    /*
	     * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	     * Until endElemtent is called we only collect the data.
	     */
    	if (!endData) {
    		tmpData = tmpData + new String(ch, start, length);
    		return;
    	}
    	endData = false;
	    
	    if (inLineText) {
	        data.line = new String(ch, start, length);
	    } else if (inDestinationStop) {
	        data.destination = new String(ch, start, length);
	    } else if (inTripStatus) {
	        final String monitored = new String(ch, start, length);
	        if (monitored.equalsIgnoreCase("Real")) {
	        	data.realtime = true;
	        } else {
	        	data.realtime = false;
	        }
	    } else if (inScheduledDISArrivalTime) {
	    	data.aimedArrival = parseDateTime(new String(ch, start, length));
	    } else if (inExpectedDISArrivalTime) {
	    	data.expectedArrival = parseDateTime(new String(ch, start, length));
	    } else if (inScheduledDISDepartureTime) {
	    	data.aimedDeparture = parseDateTime(new String(ch, start, length));
	    } else if (inExpectedDISDepartureTime) {
	    	data.expectedDeparture = parseDateTime(new String(ch, start, length));
	    } else if (inStopPosition) {
	        data.departurePlatform = new String(ch, start, length);
	    }
	}
	/*
	 * END CODE BLOCK GENERATED BY SCRIPT scripts/realtime.sh
	 */

	
}
