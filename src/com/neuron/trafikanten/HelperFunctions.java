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
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

/*
 * Small helper functions used by multiple classes.
 */
public class HelperFunctions {
	private final static String TAG = "Trafikanten-HelperFunctions";
	public final static SimpleDateFormat hourFormater = new SimpleDateFormat("HH:mm");
	public static final String KEY_DOWNLOADBYTE = "downloadkb";
	public static final int SECOND = 1000;
	public static final int MINUTE = 60 * SECOND;
	public static final int HOUR = 60 * MINUTE;

	/*
	 * Render time in the way trafikanten.no wants
	 *   From 1-9 minutes we use "X m", above that we use HH:MM
	 */
    public static String renderTime(Long currentTime, Context context, long time) {
		int diffMinutes = Math.round(((float)(time - currentTime)) / MINUTE);

		if (diffMinutes < -1) {
			// Negative time!
			diffMinutes = diffMinutes * -1;
			return "-" + diffMinutes + " min";
		} else if (diffMinutes < 1) {
			return context.getText(R.string.now).toString();
		} else if (diffMinutes <= 9) {
			return diffMinutes + " min";
		}
		return hourFormater.format(time).toString();
    }
    
    /*
     * Replace % with arguments, performance version of http://www.mail-archive.com/android-developers@googlegroups.com/msg02846.html
     */
	public static String mergeXmlArgument(Resources resources, int rId, String []args) throws IOException {
		int currentArgIndex = 0;
        InputStream xmlStream = resources.openRawResource( rId );
        StringBuffer xml = new StringBuffer(xmlStream.available() + 100);
        
		while (true) {
			final int b = xmlStream.read();
			if (b < 1) break; // EOF
			
			if ((char)b == '%') {
				/*
				 * Replace next % with args[i]
				 */
				xml.append(args[currentArgIndex]);
				currentArgIndex++; // No checking, will cause exception if % and files and args # dont match up.
			}
			else xml.append((char)b);
		} 
		xmlStream.close();

		return(xml.toString());
	}
	
	private static String userAgentString = null;
	private static String getUserAgent(Context context) {
		if (userAgentString == null) {
			CharSequence appVersion = context.getText(R.string.app_version);

			userAgentString = "TrafikantenAndroid/" + appVersion + " (aagaande) Device/" + 
				Build.VERSION.RELEASE + " (" + Locale.getDefault() + "; " + Build.MODEL + ")";			
			
			// + Locale.getDefault() + ")";
		}
		return userAgentString;
	}
	
	
	/*
	 * Updates statistics for byte downloaded.
	 */
	private static void updateStatistics(Context context, long size) {
		if (size == 0) return;
		final SharedPreferences preferences = context.getSharedPreferences("trafikantenandroid", Context.MODE_PRIVATE);
		Long downloadByte = preferences.getLong(KEY_DOWNLOADBYTE, 0) + size;
		
		final SharedPreferences.Editor editor = preferences.edit();
    	editor.putLong(KEY_DOWNLOADBYTE, downloadByte);
		editor.commit();
	}
	
	/*
	 * Decides on whether or not we should use gzip compression or not.
	 */
	public static InputStream executeHttpRequest(Context context, HttpUriRequest request) throws IOException {
		/*
		 * Add gzip header
		 */
		request.setHeader("Accept-Encoding", "gzip");
		request.setHeader("User-Agent",getUserAgent(context));
		
		/*
		 * Get the response, if we use gzip use the GZIPInputStream
		 */
		HttpResponse response = new DefaultHttpClient().execute(request);
		InputStream content = response.getEntity().getContent();
		
		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		Header contentLength = response.getFirstHeader("Content-Length");
		if (contentLength != null) {
			updateStatistics(context, Long.parseLong(contentLength.getValue()));
		} else {
			Log.e(TAG,"Contentlength is invalid!");
		}
		
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			content = new GZIPInputStream(content);
			Log.i(TAG,"Recieved compressed data - OK");
		} else {
			Log.i(TAG,"Recieved UNCOMPRESSED data - Problem server side");
		}
		
		return content;
	}
	
	
	/*
	 * Send a soap request to the server
	 */
	public static InputStream soapRequest(Context context, String soap, final String url) throws IOException {
        HttpPost httppost = new HttpPost(url);
        httppost.setHeader("Content-Type", "text/xml; charset=utf-8");
        httppost.setEntity(new StringEntity(soap));
    	//Log.d("Trafikanten - DEBUG CODE", "Soap request : " + soap);
    	
    	return executeHttpRequest(context, httppost);
	}
	/*
	 * Send a soap request, takes resource id, arguments and the soap url, returns inputStream.
	 */
	public static InputStream soapRequest(Context context, final int rid, final String[] args, final String url) throws IOException {
        final String soap = mergeXmlArgument(context.getResources(), rid, args);
        return soapRequest(context, soap, url);
	}
}
