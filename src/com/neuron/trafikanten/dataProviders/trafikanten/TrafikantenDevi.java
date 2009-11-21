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
			final String urlString = "http://devi.trafikanten.no/rss.aspx?show=filter&stop=" + stationId;
			Log.i(TAG,"Loading devi data : " + urlString);
			HttpGet request = new HttpGet(urlString);
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
	private boolean inItem = false; // Block data (contains everything under)
	private boolean inTitle = false;
	private boolean inLink = false;
	private boolean inDescription = false;
	private boolean inLines = false; // Block data (contains line)
	private boolean inLine = false;
	private boolean inValidFrom = false;
	private boolean inValidTo = false;
	private boolean inImportant = false;

	
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
	    if (!inItem) {
	        if (localName.equals("item")) {
	        	inItem = true;
	            data = new DeviData();
	        }
	    } else {
	    	if (localName.equals("title")) {
	    		inTitle = true;
	    	} else if (localName.equals("link")) {
	    		inLink = true;
		    } else if (localName.equals("description")) {
		    	inDescription = true;
		    } else if (localName.equals("lines")) {
		    	inLines = true;
		    } else if (inLines && localName.equals("line")) {
		        inLine = true;
		    } else if (localName.equals("ValidFrom")) {
		    	inValidFrom = true;
		    } else if (localName.equals("ValidTo")) {
		    	inValidTo = true;
		    } else if (localName.equals("Important")) {
		    	inImportant = true;
		    }
	    }
	}


	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
	    if (!inItem) return;
	    if (inItem && localName.equals("item")) {
	        /*
	         * on StopMatch we're at the end, and we need to add the station to the station list.
	         */
	    	inItem = false;
	        final DeviData sendData = data;
			handler.post(new Runnable() {
				@Override
				public void run() {
					handler.onData(sendData);	
				}
			});
	    } else { 
	    	if (inTitle) {
	    		inTitle = false;
		        data.title = buffer.toString();
	    	} else if (inLink) {
	    		inLink = false;
	    		data.link = buffer.toString();
		    } else if (inDescription) {
		    	inDescription = false;
		        data.description = buffer.toString();
		    } else if (inLine) { // 
		    	inLine = false;
		        data.lines.add(buffer.toString());
		    } else if (inLines) { //  && localName.equals("lines") is unneeded, as we check inLine first, and if we have no "line" data we must have "lines" data
		    	inLines = false;
		    } else if (inValidFrom) {
		    	inValidFrom = false;
		        data.validFrom = parseDateTime(buffer.toString());
		    } else if (inValidTo) {
		    	inValidTo = false;
		        data.validTo = parseDateTime(buffer.toString());
		    } else if (inImportant) {
		        inImportant = false;
		        data.important = buffer.toString().equals("True");
		    }
	    }
		buffer.setLength(0);
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		//inAimedArrivalTime || inExpectedArrivalTime
	    if (inTitle || inLink || inDescription ||
	    		inLine || inValidFrom || inValidTo || inImportant) {
	    	buffer.append(ch,start,length);
	    }
	}
}

