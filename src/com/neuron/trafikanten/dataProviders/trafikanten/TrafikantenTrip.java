package com.neuron.trafikanten.dataProviders.trafikanten;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;

import com.neuron.trafikanten.HelperFunctions;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.GenericDataProviderThread;
import com.neuron.trafikanten.dataProviders.IGenericProviderHandler;
import com.neuron.trafikanten.dataSets.StationData;

public class TrafikantenTrip extends GenericDataProviderThread<StationData> {
	private final Context context;
	private final int tourID;
	
	public TrafikantenTrip(Context context, int tourID, IGenericProviderHandler<StationData> handler) {
		super(handler);
		this.context = context;
		this.tourID = tourID;
		start();
	}
	
    @Override
	public void run() {
		try {
			/*
			 * Setup args and send soap request
			 */
			final String[] args = new String[]{new Integer(tourID).toString()};
			final InputStream result = HelperFunctions.soapRequest(context, R.raw.gettrip, args, Trafikanten.API_URL);

			/*
			 * Setup SAXParser and XMLReader
			 */
			final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			final SAXParser parser = parserFactory.newSAXParser();
			
			final XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(new TripHandler(this));
			
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

class TripHandler extends DefaultHandler {
	/*
	 * This shares a ton of code with TrafikantenSearch, but I'm keeping them seperate and not sharing code due to some odd pieces in the gettrip api.
	 *   - for example the structure stops -> stop -> (with optional stops), instead of the "normal" place -> stop (with optional stops)
	 */
	private StationData station;
	private final TrafikantenTrip parent;

	/*
	 * Temporary variables for parsing. 
	 */
	private boolean inStop = false;
	private boolean inX = false;
	private boolean inY = false;
	private boolean inID = false;
	private boolean inName = false;
	private boolean inDistrict = false;
	private boolean inRealTimeStop = false;
	
	// Ignore is used to ignore anything except type Stop.
	private boolean ignore = false;
	
	//Temporary variable for character data:
	private StringBuffer buffer = new StringBuffer();
	
	public TripHandler(TrafikantenTrip parent)
	{

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
        if (!inStop) {
            if (localName.equals("Stop")) {
            	inStop = true;
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
			} else if (localName.equals("RealTimeStop")) {
				inRealTimeStop = true;
			}
		}
    } 
    
    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        if (!inStop) return;
        if (localName.equals("Stop")) {
            /*
             * on StopMatch we're at the end, and we need to add the station to the station list.
             */
        	inStop = false;
            if (!ignore) {
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
	        } else if (inRealTimeStop && localName.equals("RealTimeStop")) {
	        	inRealTimeStop = false;
	        	// TODO : Should get api clearification here, this should not be neccesary...
	        	station.realtimeStop = buffer.toString().toLowerCase().equals("true");
	        }
        }
        buffer.setLength(0);
    }
    
    @Override
    public void characters(char ch[], int start, int length) {
    	if (ignore) return;
    	if (inX || inY || inID || inName || inDistrict || inRealTimeStop) {
    		buffer.append(ch, start, length);
    	}
    }
}