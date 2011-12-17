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
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;

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
	private final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd");

	
	public TrafikantenDevi(Context context, int stationId, String lines, IGenericProviderHandler<DeviData> handler) {
		super();
		this.context = context;
		this.stationId = stationId;
		this.lines = lines;
		start(handler);
	}
	
    @Override
	public void run() {
		try {
			//String urlString = "http://devi.trafikanten.no/devirest.svc/json/linenames/";
			String urlString = "http://devi.trafikanten.no/devirest.svc/json/lineids/";
			if (lines.length() > 0) {
				urlString = urlString + URLEncoder.encode(lines,"UTF-8");
			} else {
				urlString = urlString + "null";
			}
			urlString = urlString + "/stopids/" + stationId + "/";
			urlString = urlString + "from/" + dateFormater.format(System.currentTimeMillis()) + "/to/2030-12-31";
			Log.i(TAG,"Loading devi data : " + urlString);
			
			final InputStream stream = HelperFunctions.executeHttpRequest(context, new HttpGet(urlString), false).stream;
			
			/*
			 * Parse json
			 */
			final JSONArray jsonArray = new JSONArray(HelperFunctions.InputStreamToString(stream));
			final int arraySize = jsonArray.length();
			for (int i = 0; i < arraySize; i++) {
				final JSONObject json = jsonArray.getJSONObject(i);
				DeviData deviData = new DeviData();
				
				{
					/*
					 * Grab validFrom and check if we should show it or not.
					 */
					deviData.validFrom = HelperFunctions.jsonToDate(json.getString("validFrom"));
			        final long dateDiffHours = (deviData.validFrom - Calendar.getInstance().getTimeInMillis()) / HelperFunctions.HOUR;
			        if (dateDiffHours > 3) {
			        	/*
			        	 * This data starts more than 3 hours into the future, hide it.
			        	 */
			        	continue;
			        }
				}
				{
					/*
					 * Grab published, and see if this should be visible yet.
					 */
					final long publishDate = HelperFunctions.jsonToDate(json.getString("published"));
			        if (System.currentTimeMillis() < publishDate) {
			        	continue;
			        }
				}
				{
					/*
					 * Check validto, and see if this should be visible yet.
					 */
					final long validToDate = HelperFunctions.jsonToDate(json.getString("validTo"));
			        if (System.currentTimeMillis() > validToDate) {
			        	continue;
			        }
				}
				

				/*
				 * Grab the easy data
				 */
				deviData.title = json.getString("header");
				deviData.description = json.getString("lead");
				deviData.body = json.getString("body");
				deviData.validTo = HelperFunctions.jsonToDate(json.getString("validTo"));
				deviData.important = json.getBoolean("important");
				deviData.id = json.getInt("id");
				
				{
					/*
					 * Grab lines array
					 */
					final JSONArray jsonLines = json.getJSONArray("lines");
					final int jsonLinesSize = jsonLines.length();
					for (int j = 0; j < jsonLinesSize; j++) {
						deviData.lines.add(jsonLines.getJSONObject(j).getInt("lineID"));
					}
				}
				
				
				{
					/*
					 * Grab stops array
					 */
					final JSONArray jsonLines = json.getJSONArray("stops");
					final int jsonLinesSize = jsonLines.length();
					for (int j = 0; j < jsonLinesSize; j++) {
						deviData.stops.add(jsonLines.getJSONObject(j).getInt("stopID"));
					}
				}
				
				ThreadHandlePostData(deviData);
			}
	
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
