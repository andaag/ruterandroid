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

import android.os.AsyncTask;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataProviders.IRealtimeProvider.RealtimeProviderHandler;
import com.neuron.trafikanten.dataSets.RealtimeData;

public class TrafikantenRealtime extends AsyncTask<Void, RealtimeData, Void> {
	private static final String TAG = "Trafikanten-T-RealtimeThread";
	private Exception exception = null;
	
	private final int stationId;
	private final RealtimeProviderHandler handler;
	
	public TrafikantenRealtime(int stationId, RealtimeProviderHandler handler) {
		this.stationId = stationId;
		this.handler = handler;
	}
	
    protected Void doInBackground(Void... unused) {
		try {
			final String urlString = "http://reis.trafikanten.no/siri/sm.aspx?id=" + stationId;
			Log.i(TAG,"Loading realtime data : " + urlString);
			HttpGet request = new HttpGet(urlString);
			InputStream result = HelperFunctions.executeHttpRequest(request);

			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new RealtimeHandler(this));
			reader.parse(new InputSource(result));
		} catch(Exception e) {
			if (e.getClass() == InterruptedException.class)
				return null;
			this.exception = e;
		}
		return null;
    }
    
    /*
     * Comes from the thread
     */
    public void addData(RealtimeData data) {
    	publishProgress(data);
    }

    /*
     * Anything under this updates the ui 
     */
    protected void onProgressUpdate(RealtimeData... progress) {
        handler.onData(progress[0]);
    }

    @Override
	protected void onPreExecute() {
    	handler.onPreExecute();
	}

	protected void onPostExecute(Void unused) {
		handler.onPostExecute(exception);
    }
}

/*
 * Realtime XML Parser
 */
class RealtimeHandler extends DefaultHandler {
	private RealtimeData data;
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
	private final TrafikantenRealtime asyncTask;
	
	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inMonitoredStopVisit = false; // Top block
	private boolean inPublishedLineName = false;
	private boolean inDestinationName = false;
	private boolean inMonitored = false;

	private boolean inAimedDepartureTime = false;
	private boolean inExpectedDepartureTime = false;
	private boolean inDeparturePlatformName = false;

	private boolean inStopVisitNote = false;
	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();
	
	public RealtimeHandler(TrafikantenRealtime asyncTask)
	{
		this.asyncTask = asyncTask;
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
	            data = new RealtimeData();
	        }
	    } else {
	    	if (localName.equals("PublishedLineName")) {
		        inPublishedLineName = true;
		    } else if (localName.equals("DestinationName")) {
		        inDestinationName = true;
		    } else if (localName.equals("Monitored")) {
		        inMonitored = true;
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
	        asyncTask.addData(data);
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
		    } else if (inExpectedDepartureTime) {
		        inExpectedDepartureTime = false;
		        data.expectedDeparture = parseDateTime(buffer.toString());
		    } else if (inDeparturePlatformName) {
		        inDeparturePlatformName = false;
		        /*
		         * platform should be parsed as int, they recomend parsing them as int.
		         * 
					from	Torbjørn Barslett <tb@trafikanten.no>
					sender-time	Sent at 4:02 PM (GMT+01:00). Current time there: 5:09 PM. ✆
					to	Anders Aagaard <aagaande@gmail.com>,
					Bent Flyen <bent.flyen@trafikanten.no>
					cc	QA <qa@trafikanten.no>
					date	Tue, Jan 26, 2010 at 4:02 PM
					subject	SV: Trafikanten oppdatering
		         */
		        try {
		        	data.departurePlatform = Integer.parseInt(buffer.toString());
		        } catch (NumberFormatException e) {
		        	data.departurePlatform = 0;
		        }
		    } else if (inStopVisitNote) {
		        inStopVisitNote = false;
		        data.stopVisitNote = buffer.toString();
		    }
	    }
		buffer.setLength(0);
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
	    if (inPublishedLineName || inDestinationName ||
	    		inMonitored || inAimedDepartureTime || inExpectedDepartureTime || inDeparturePlatformName || inStopVisitNote) {
	    	buffer.append(ch,start,length);
	    }
	}
}
