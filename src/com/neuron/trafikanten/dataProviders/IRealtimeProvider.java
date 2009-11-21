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

import com.neuron.trafikanten.dataSets.RealtimeData;

import android.os.Handler;


/*
 * Realtime provider
 */
public interface IRealtimeProvider extends IGenericProvider {
	public void Fetch(int stationId);
	
	abstract class RealtimeProviderHandler extends Handler {
	    public abstract void onData(RealtimeData realtimeData);
	    public abstract void onError(Exception e);
	    public abstract void onFinished();
	}
}
