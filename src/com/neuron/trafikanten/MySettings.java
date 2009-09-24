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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.neuron.trafikanten.dataProviders.DataProviderFactory;
import com.neuron.trafikanten.locationProviders.LocationProviderFactory;

public final class MySettings {
	private static final String TAG = "MySettings";
	public static final String KEY_DATA_PROVIDER = "data_provider";
	public static final String KEY_LOCATION_PROVIDER = "location_provider";
	
	/*
	 * Settings we store:
	 */
	// DataProvider:
	public static int SETTING_DATAPROVIDER = 0;
	// LocationProvider:

	public static int SETTING_LOCATIONPROVIDER = 0;
	
	/*
	 * Using a static variable loaded to only reload when application was killed during onPause
	 */
	private static boolean loaded = false;
	
	/*
	 * Refresh all settings, needs to be called on every onResume incase of kill & restart of application.
	 */
	public static void refresh(Context context) {
		Configuration conf = context.getResources().getConfiguration();
		Log.i("TEMP","Lang : " + conf.locale.toString());
		
		
		context = context.getApplicationContext();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (loaded) return;
		preferencesChanged(preferences, KEY_DATA_PROVIDER);
        preferencesChanged(preferences, KEY_LOCATION_PROVIDER);
		loaded = true;
	}
	
	
	/*
	 * set data provider
	 */
	private static void setDataProvider(String newProvider) {
		/*
		 * If no provider set, default to trafikanten
		 */
		if (newProvider == null) {
			setDataProvider("Trafikanten");
			return;
		}
		
		/*
		 * Scan through the list and set it to the right number.
		 */
		String[] dataProviders = DataProviderFactory.getDataProviders();
		for (int i = 0; i < dataProviders.length; i++) {
			if (newProvider.equals(dataProviders[i])) {
				SETTING_DATAPROVIDER = i + 1;
				return;
			}
		}
		
		Log.e(TAG, "setDataProvider(" + newProvider + ") failed this should never happen!");
		setDataProvider("Trafikanten");
	}
	
	/*
	 * set location provider
	 */
	private static void setLocationProvider(String newProvider) {
		SETTING_LOCATIONPROVIDER = 0;
		
		if (newProvider == null) {
			setLocationProvider("Skyhook");
			return;
		}
		
		/*
		 * Scan through the list and set it to the right number.
		 */
		String[] locationProviders = LocationProviderFactory.getLocationProviders();
		for (int i = 0; i < locationProviders.length; i++) {
			if (newProvider.equals(locationProviders[i])) {
				SETTING_LOCATIONPROVIDER = i + 1;
				return;
			}
		}
		
		Log.e(TAG, "setLocationProvider(" + newProvider + ") failed this should never happen!");
		setLocationProvider(null);
	}
	
	/*
	 * preferencesChanged is called when preferences are updated.
	 */
	public static void preferencesChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(KEY_DATA_PROVIDER)) {
			final String dataProvider = sharedPreferences.getString(KEY_DATA_PROVIDER, null);
			setDataProvider(dataProvider);
		} else if (key.equals(KEY_LOCATION_PROVIDER)) {
			final String locationProvider = sharedPreferences.getString(KEY_LOCATION_PROVIDER, null);
			setLocationProvider(locationProvider);
		}
	}
}
