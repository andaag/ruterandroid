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

import java.text.SimpleDateFormat;

import android.content.Context;

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
    
}
