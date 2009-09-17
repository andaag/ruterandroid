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

import com.neuron.trafikanten.dataSets.SearchStationData;

import android.content.Intent;
import android.os.Bundle;

/*
 * This is the generic SelectStationView view, that returns with results.
 */
public class SelectStationView extends GenericSelectStationView {
	/*
	 * Handler for station selected
	 */
	@Override
	public void stationSelected(SearchStationData station) {
		if (!station.isFavorite) {
			historyDbAdapter.updateHistory(station);
		}
		favoriteDbAdapter.close();
		historyDbAdapter.close();
		
		Bundle bundle = new Bundle();
		bundle.putParcelable(SearchStationData.PARCELABLE, station);
		
		final Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();		
	}
}