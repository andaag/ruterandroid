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

package com.neuron.trafikanten.dataProviders;

public interface IGenericProvider {
	public static final int MESSAGE_DONE = 1;
	/*
	 * on MESSAGE_DONE call ResultsProviderFactory.Get* for results.
	 * DO NOT send MESSAGE_DONE until the thread is 100% done processing the results.
	 */
	public static final int MESSAGE_EXCEPTION = 2;
	/*
	 * on MESSAGE_EXCEPTION msg.obj == exception.
	 */
	
	public static final String KEY_EXCEPTION = "exception";
	
	public void Stop();
}
