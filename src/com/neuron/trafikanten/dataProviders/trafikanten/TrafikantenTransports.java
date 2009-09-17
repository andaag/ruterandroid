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

package com.neuron.trafikanten.dataProviders.trafikanten;

import com.neuron.trafikanten.R;
import com.neuron.trafikanten.dataProviders.IRouteProvider;

public class TrafikantenTransports {
	static public int getImageResource(int transportType) {
		switch (transportType) {
		case IRouteProvider.TRANSPORT_TRAIN:
			return R.drawable.icon_train;
		case IRouteProvider.TRANSPORT_TRAM:
			return R.drawable.icon_tram;
		case IRouteProvider.TRANSPORT_BUS:
			return R.drawable.icon_bus;
		case IRouteProvider.TRANSPORT_SUBWAY:
			return R.drawable.icon_subway;
		}
		return R.drawable.icon_unknown;
	}
}
