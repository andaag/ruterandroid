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
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.client.methods.HttpGet;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.dataProviders.GenericDataProviderThread;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataSets.DeviData;

public class TrafikantenDevi extends GenericDataProviderThread<DeviData> {
	private static final String TAG = "Trafikanten-T-DeviThread";
	private Context context;
	private final int stationId;
	private final String lines;
	
	public TrafikantenDevi(Context context, int stationId, String lines, IGenericProviderHandler<DeviData> handler) {
		super(handler);
		this.context = context;
		this.stationId = stationId;
		this.lines = lines;
		start();
	}
	
    @Override
	public void run() {
		try {
			final String urlString = "http://devi.trafikanten.no/rss.aspx?show=filter&stop=" + stationId + "&linename=" + URLEncoder.encode(lines,"UTF-8");
			Log.i(TAG,"Loading devi data : " + urlString);
			HttpGet request = new HttpGet(urlString);
			InputStream result = HelperFunctions.executeHttpRequest(context, request);

			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new DeviHandler(this));
			
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
 * Devi XML Parser
 */
class DeviHandler extends DefaultHandler {
	private DeviData data;
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
	private final TrafikantenDevi parent;
	
	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inItem = false; // Block data (contains everything under)
	private boolean inTitle = false;
	private boolean inDescription = false;
	private boolean inBody = false;
	private boolean inStops = false; // Block data (contains stop)
	private boolean inStop = false;
	private boolean inLines = false; // Block data (contains line)
	private boolean inLine = false;
	private boolean inValidFrom = false;
	private boolean inValidTo = false;
	private boolean inPublished = false;
	private boolean inImportant = false;
	
	
	private boolean skip = false; // if true skip sending data
	private boolean hasPublished = false; // if true skip sending data

	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();
	
	public DeviHandler(TrafikantenDevi parent)
	{
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
	    if (!inItem) {
	        if (localName.equals("item")) {
	        	inItem = true;
	            data = new DeviData();
	            skip = false;
	            hasPublished = false;
	        }
	    } else {
	    	if (localName.equals("title")) {
	    		inTitle = true;
		    } else if (localName.equals("description")) {
		    	inDescription = true;
	    	} else if (localName.equals("body")) {
	    		inBody = true;
		    } else if (localName.equals("lines")) {
		    	inLines = true;
		    } else if (inLines && localName.equals("line")) {
		        inLine = true;
		    } else if (localName.equals("stops")) {
		    	inStops = true;
		    } else if (inStops && localName.equals("stop")) {
		        inStop = true;
		    } else if (localName.equals("ValidFrom")) {
		    	inValidFrom = true;
		    } else if (localName.equals("ValidTo")) {
		    	inValidTo = true;
		    } else if (localName.equals("Published")) {
		    	inPublished = true;
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
	    	if (!skip) {
	    		parent.ThreadHandlePostData(data);
	    	}
	    } else { 
	    	if (inTitle) {
	    		inTitle = false;
		        data.title = buffer.toString();
		    } else if (inDescription) {
		    	inDescription = false;
		        data.description = buffer.toString();
	    	} else if (inBody) {
	    		inBody = false;
	    		data.body = buffer.toString();
		    } else if (inLine) { // 
		    	inLine = false;
		        data.lines.add(buffer.toString());
		    } else if (inLines) { //  && localName.equals("lines") is unneeded, as we check inLine first, and if we have no "line" data we must have "lines" data
		    	inLines = false;
		    } else if (inStop) { // 
		    	inStop = false;
		    	
		    	/*
		    	 * We filter (region) from data so we're forced to do it here as well. It should not be an issue as devi is filtered on line + station already.
		    	 */
		    	String stopName = buffer.toString();
				int startAddress = stopName.indexOf('(');
				if (startAddress > 0) {
					stopName = stopName.substring(0, startAddress - 1);
				}
					
		        data.stops.add(stopName);
		    } else if (inStops) { //  && localName.equals("stops") is unneeded, as we check inLine first, and if we have no "stop" data we must have "stops" data
		    	inStops = false;
		    } else if (inValidFrom) {
		    	inValidFrom = false;
		        data.validFrom = parseDateTime(buffer.toString());
		        
		        /*
		         * If data is validFrom in the next 3 hours we should show it, as realtime data can show 3 hours into the future.
		         */
		        long dateDiffHours = (data.validFrom - Calendar.getInstance().getTimeInMillis()) / HelperFunctions.HOUR;
		        if (!hasPublished && dateDiffHours > 3) {
		        	/*
		        	 * This data starts more than 3 hours into the future, hide it.
		        	 */
		        	skip = true;
		        }
		    } else if (inValidTo) {
		    	inValidTo = false;
		        data.validTo = parseDateTime(buffer.toString());
		    } else if (inPublished) {
		    	// incase validFrom has been parsed, we ignore validFrom if published exists.
		    	hasPublished = true;
		    	skip = false;
		    	
		    	inPublished = false;
		        long publishDate = parseDateTime(buffer.toString());
		        if (System.currentTimeMillis() > publishDate) {
		        	skip = true;
		        }
		    } else if (inImportant) {
		        inImportant = false;
		        data.important = buffer.toString().equals("True");
		    }
	    }
		buffer.setLength(0);
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
	    if (inTitle || inBody || inDescription ||
	    		inStop || inLine || inValidFrom || inValidTo ||  inPublished || inImportant) {
	    	buffer.append(ch,start,length);
	    }
	}
}

