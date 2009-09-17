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

import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

public class LanguageFactory {
	private static final String TAG = "LanguageFactory";
	private static Resources res;
	
	
	public static String getString(int id) {
		return res.getString(id);
	}
	
	/*
	 * Initialize res
	 */
	public static void init(Context context) {
		if (res == null) {
			/*
			 * Get the application wide context, incase of reloads and old context being invalid.
			 * See http://android-developers.blogspot.com/2009/01/avoiding-memory-leaks.html
			 */
			res = context.getApplicationContext().getResources();
		}
	}
	
	public static void setLanguage(String language, String country) {
    	DisplayMetrics dm = res.getDisplayMetrics();
    	Configuration conf = res.getConfiguration();
    	conf.locale = new Locale(language, country);
    	res.updateConfiguration(conf, dm);
    	Log.i(TAG, "Changed language to " + language + "-" + country);
	}
}
