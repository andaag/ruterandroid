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

package com.neuron.trafikanten.views;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.neuron.trafikanten.MySettings;
import com.neuron.trafikanten.R;
import com.neuron.trafikanten.locationProviders.LocationProviderFactory;

public class SettingsView extends PreferenceActivity implements OnSharedPreferenceChangeListener  {
	private String oldLanguage;
	
	public static void Show(Activity activity, int what) {
		Intent intent = new Intent(activity, SettingsView.class);
		activity.startActivityForResult(intent, what);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		/*
		 * Setup list view for data providers
		 */
/*		ListPreference dataProviderList = (ListPreference) findPreference(MySettings.KEY_DATA_PROVIDER);
		dataProviderList.setDefaultValue(null);
		dataProviderList.setEntries(DataProviderFactory.getDataProviders());
		dataProviderList.setEntryValues(DataProviderFactory.getDataProviders());*/
		
		/*
		 * Setup list view for location providers
		 */
		ListPreference locationProviderList = (ListPreference) findPreference(MySettings.KEY_LOCATION_PROVIDER);
		locationProviderList.setDefaultValue(null);
		locationProviderList.setEntries(LocationProviderFactory.getLocationProviders());
		locationProviderList.setEntryValues(LocationProviderFactory.getLocationProviders());
		
		/*
		 * Setup list view for language
		 */
		ListPreference languageProviderList = (ListPreference) findPreference(MySettings.KEY_LANGUAGE);
		languageProviderList.setDefaultValue(null);
		final String[] language = new String[] { "English", "Norsk" };
		languageProviderList.setEntries(language);
		languageProviderList.setEntryValues(language);
		
		oldLanguage = getLanguage();
		Toast.makeText(this, R.string.languageWarning, Toast.LENGTH_LONG).show();
	}
	
	/*
	 * Get current set language
	 */
	private String getLanguage() {
		Configuration conf = getResources().getConfiguration();
		return conf.locale.toString();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		MySettings.preferencesChanged(sharedPreferences, key);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MySettings.refresh(this);
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onStop() {
		if (!oldLanguage.equals(getLanguage())) {
			/*
			 * Force activity reload
			 */
			ActivityManager activityManager = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE); 
			activityManager.restartPackage(getPackageName());
		}
		super.onStop();
	}
	
	

}
