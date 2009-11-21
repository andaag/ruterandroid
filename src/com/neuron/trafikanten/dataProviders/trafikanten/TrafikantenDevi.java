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
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.methods.HttpGet;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataProviders.IDeviProvider;
import com.neuron.trafikanten.dataProviders.IDeviProvider.DeviProviderHandler;
import com.neuron.trafikanten.dataSets.DeviData;

public class TrafikantenDevi  implements IDeviProvider {
	private DeviProviderHandler handler;
	private TrafikantenDeviThread thread;
	
	public TrafikantenDevi(DeviProviderHandler handler) {
		this.handler = handler;
	}

	/*
	 * Fetch Devi data for a station id
	 * @see com.neuron.trafikanten.dataProviders.IDeviProvider#Fetch(int)
	 */
	@Override
	public void Fetch(int stationId) {
		Stop();
		thread = new TrafikantenDeviThread(handler, stationId);
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

class TrafikantenDeviThread extends Thread implements Runnable {
	private static final String TAG = "Trafikanten-T-DeviThread";
	private DeviProviderHandler handler;
	private int stationId;	
	
	public TrafikantenDeviThread(DeviProviderHandler handler, int stationId) {
		this.handler = handler;
		this.stationId = stationId;
	}
	
	/*
	 * Run current thread.
	 * This function setups the url and the xmlreader, and passes data to the DeviHandler. 
	 */
	
	public void run() {
		try {
			HttpGet request = new HttpGet("http://devi.trafikanten.no/rss.aspx?show=filter&stop=" + stationId);
			InputStream result = HelperFunctions.executeHttpRequest(request);

			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new DeviHandler(handler));
			
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
 * Devi XML Parser
 */
class DeviHandler extends DefaultHandler {
	private DeviData data;
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
	private DeviProviderHandler handler;
	
	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inMonitoredStopVisit = false; // Top block
	private boolean inPublishedLineName = false;
	private boolean inDestinationName = false;
	private boolean inMonitored = false;

	/*private boolean inAimedArrivalTime = false;
	private boolean inExpectedArrivalTime = false;*/

	private boolean inAimedDepartureTime = false;
	private boolean inExpectedDepartureTime = false;
	private boolean inDeparturePlatformName = false;

	private boolean inStopVisitNote = false;
	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();
	
	public DeviHandler(DeviProviderHandler handler)
	{
		this.handler = handler;
	}
	
	/*
	 * End of document, call onCompleted with complete realtimeList
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
	    if (!inMonitoredStopVisit) {
	        if (localName.equals("MonitoredStopVisit")) {
	            inMonitoredStopVisit = true;
	            data = new DeviData();
	        }
	    } else {
	    	if (localName.equals("PublishedLineName")) {
		        inPublishedLineName = true;
		    } else if (localName.equals("DestinationName")) {
		        inDestinationName = true;
		    } else if (localName.equals("Monitored")) {
		        inMonitored = true;
		    /*} else if (localName.equals("AimedArrivalTime")) {
		        inAimedArrivalTime = true;
		    } else if (localName.equals("ExpectedArrivalTime")) {
		        inExpectedArrivalTime = true;*/
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
	    if (inMonitoredStopVisit && localName.equals("MonitoredStopVisit")) {
	        /*
	         * on StopMatch we're at the end, and we need to add the station to the station list.
	         */
	        inMonitoredStopVisit = false;
	        final DeviData sendData = data;
			handler.post(new Runnable() {
				@Override
				public void run() {
					handler.onData(sendData);	
				}
			});
	    } else { 
	    	if (inPublishedLineName) {
		        inPublishedLineName = false;
		        data.line = buffer.toString();
		    } else if (inDestinationName) {
		        inDestinationName = false;
		        data.destination = buffer.toString();
		    } else if (inMonitored) {
		        inMonitored = false;
		    	final String monitored = buffer.toString();
		        if (monitored.equalsIgnoreCase("true")) {
		        	data.realtime = true;
		        } else {
		        	data.realtime = false;
		        }
		    /*} else if (inAimedArrivalTime) {
		        inAimedArrivalTime = false;
		        data.aimedArrival = parseDateTime(buffer.toString());
		    } else if (inExpectedArrivalTime) {
		        inExpectedArrivalTime = false;
		        data.expectedArrival = parseDateTime(buffer.toString());*/
		    } else if (inAimedDepartureTime) {
		        inAimedDepartureTime = false;
		        data.aimedDeparture = parseDateTime(buffer.toString());
		    } else if (inExpectedDepartureTime) {
		        inExpectedDepartureTime = false;
		        data.expectedDeparture = parseDateTime(buffer.toString());
		    } else if (inDeparturePlatformName) {
		        inDeparturePlatformName = false;
		        data.departurePlatform = buffer.toString();
		    } else if (inStopVisitNote) {
		        inStopVisitNote = false;
		        data.extra = buffer.toString();
		    }
	    }
		buffer.setLength(0);
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		//inAimedArrivalTime || inExpectedArrivalTime
	    if (inPublishedLineName || inDestinationName ||
	    		inMonitored || inAimedDepartureTime || inExpectedDepartureTime || inDeparturePlatformName || inStopVisitNote) {
	    	buffer.append(ch,start,length);
	    }
	}
}

