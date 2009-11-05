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
	private static final String TAG = "Trafikanten-T-RealtimeThread";
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
			URL url = new URL("http://reis.trafikanten.no/siri/sm.aspx?id=" + stationId);
			Log.i(TAG,"Realtime url : " + url);
			
			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new RealtimeHandler(handler));
			
			reader.parse(new InputSource(url.openStream()));
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
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
	private Handler handler;
	
	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inMonitoredStopVisit = false;
	private boolean inPublishedLineName = false;
	private boolean inDestinationName = false;
	private boolean inMonitored = false;

	private boolean inAimedArrivalTime = false;
	private boolean inExpectedArrivalTime = false;

	private boolean inAimedDepartureTime = false;
	private boolean inExpectedDepartureTime = false;
	private boolean inDeparturePlatformName = false;

	private boolean inStopVisitNote = false;
	
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
	
	@Override
	public void startElement(String namespaceURI, String localName, 
	              String qName, Attributes atts) throws SAXException {
		tmpData = "";
	    if (!inMonitoredStopVisit) {
	        if (localName.equals("MonitoredStopVisit")) {
	            inMonitoredStopVisit = true;
	            data = new RealtimeData();
	        }
	    } else {
	    	if (localName.equals("PublishedLineName")) {
		        inPublishedLineName = true;
		    } else if (localName.equals("DestinationName")) {
		        inDestinationName = true;
		    } else if (localName.equals("Monitored")) {
		        inMonitored = true;
		    } else if (localName.equals("AimedArrivalTime")) {
		        inAimedArrivalTime = true;
		    } else if (localName.equals("ExpectedArrivalTime")) {
		        inExpectedArrivalTime = true;
		    } else if (localName.equals("AimedDepartureTime")) {
		        inAimedDepartureTime = true;
		    } else if (localName.equals("ExpectedDepartureTime")) {
		        inExpectedDepartureTime = true;
		    } else if (localName.equals("DeparturePlatformName")) {
		        inDeparturePlatformName = true;
		    } else if (localName.equals("StopVisitNote")) {
		        inStopVisitNote = true;
		    }
	    }
	}


	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
	    if (!inMonitoredStopVisit) return;
	    /*
	     * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	     * We collect all data until endElement is called, and then pass that again to characters() manually. 
	     */
    	if (tmpData.length() > 0) {
        	endData = true;
    		characters(tmpData.toCharArray(), 0, tmpData.length());
    	}
	    
	    if (localName.equals("MonitoredStopVisit")) {
	        /*
	         * on StopMatch we're at the end, and we need to add the station to the station list.
	         */
	        inMonitoredStopVisit = false;
	        addData();
	    } else { 
	    	if (inPublishedLineName && localName.equals("PublishedLineName")) {
		        inPublishedLineName = false;
		    } else if (inDestinationName && localName.equals("DestinationName")) {
		        inDestinationName = false;
		    } else if (inMonitored && localName.equals("Monitored")) {
		        inMonitored = false;
		    } else if (inAimedArrivalTime && localName.equals("AimedArrivalTime")) {
		        inAimedArrivalTime = false;
		    } else if (inExpectedArrivalTime && localName.equals("ExpectedArrivalTime")) {
		        inExpectedArrivalTime = false;
		    } else if (inAimedDepartureTime && localName.equals("AimedDepartureTime")) {
		        inAimedDepartureTime = false;
		    } else if (inExpectedDepartureTime && localName.equals("ExpectedDepartureTime")) {
		        inExpectedDepartureTime = false;
		    } else if (inDeparturePlatformName && localName.equals("DeparturePlatformName")) {
		        inDeparturePlatformName = false;
		    } else if (inStopVisitNote && localName.equals("StopVisitNote")) {
		        inStopVisitNote = false;
		    }
	    }
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException { 
	    if (!inMonitoredStopVisit) return;
	    /*
	     * Workaround for bug http://code.google.com/p/android/issues/detail?id=2459
	     * Until endElemtent is called we only collect the data.
	     */
    	if (!endData) {
    		tmpData = tmpData + new String(ch, start, length);
    		return;
    	}
    	endData = false;
	    
	    if (inPublishedLineName) {
	        data.line = new String(ch, start, length);
	    } else if (inDestinationName) {
	        data.destination = new String(ch, start, length);
	    } else if (inMonitored) {
	        final String monitored = new String(ch, start, length);
	        if (monitored.equalsIgnoreCase("true") || monitored.equals("1")) {
	        	data.realtime = true;
	        } else {
	        	data.realtime = false;
	        }
	    } else if (inAimedArrivalTime) {
	    	data.aimedArrival = parseDateTime(new String(ch, start, length));
	    } else if (inExpectedArrivalTime) {
	    	data.expectedArrival = parseDateTime(new String(ch, start, length));
	    } else if (inAimedDepartureTime) {
	    	data.aimedDeparture = parseDateTime(new String(ch, start, length));
	    } else if (inExpectedDepartureTime) {
	    	data.expectedDeparture = parseDateTime(new String(ch, start, length));
	    } else if (inDeparturePlatformName) {
	        data.departurePlatform = new String(ch, start, length);
	    } else if (inStopVisitNote) {
	    	data.extra = new String(ch, start, length);
	    }
	}
}
