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

package com.neuron.trafikanten;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.res.Resources;

/*
 * Small helper functions used by multiple classes.
 */
public class HelperFunctions {
	public final static SimpleDateFormat hourFormater = new SimpleDateFormat("HH:mm");
	public static final int SECOND = 1000;
	public static final int MINUTE = 60 * SECOND;
	public static final int HOUR = 60 * MINUTE;

	/*
	 * Render time in the way trafikanten.no wants
	 *   From 1-9 minutes we use "X m", above that we use HH:MM
	 */
    public static String renderTime(Context context, long time) {
		long diffMinutes = (time - System.currentTimeMillis()) / MINUTE;
		if (diffMinutes < 1) {
			return context.getText(R.string.now).toString();
		} else if (diffMinutes < 9) {
			return diffMinutes + "m";
		}
		return hourFormater.format(time).toString();
    }
    
    /*
     * Render the time at HH:MM if possible, if not use 34m
     */
    public static String renderAccurate(long time) {
		if (time < MINUTE)
			return null;
		if (time < HOUR) {
			return "" + time / MINUTE + "m";
		}
		return hourFormater.format(time).toString();    	
    }
    
    /*
     * Replace % with arguments, simplified version of http://www.mail-archive.com/android-developers@googlegroups.com/msg02846.html
     */
	public static String mergeXmlArgument(Resources resources, int rId, String []args) throws IOException {
		int currentArgIndex = 0;
        InputStream xmlStream = resources.openRawResource( rId );
        StringBuffer xml = new StringBuffer(xmlStream.available() + 100);
        
		while ( true ) {
			final int b = xmlStream.read();
			if (b < 1) break; // EOF
			
			if ( (char)b == '%' ) { // substitute next args[]
				xml.append(args[currentArgIndex]);
				currentArgIndex++;
			}
			else xml.append( (char)b);
		} 
		xmlStream.close();

		return(xml.toString());
	}
	
	/*
	 * Send a soap request, takes resource id, arguments and the soap url, returns inputStream.
	 */
	public static InputStream soapRequest(final Resources resources, final int rid, final String[] args, final String url) throws IOException {
        final String soap = mergeXmlArgument(resources, rid, args);

        HttpPost httppost = new HttpPost(url);
    	httppost.setHeader("Content-Type", "text/xml; charset=utf-8");
        httppost.setEntity(new StringEntity(soap));
    	//Log.d("DEBUG CODE", "Soap request : " + soap);
    	
    	HttpResponse response = new DefaultHttpClient().execute(httppost);
    	return response.getEntity().getContent();
	}
}
