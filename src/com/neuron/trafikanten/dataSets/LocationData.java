package com.neuron.trafikanten.dataSets;

public class LocationData {
	public final double latitude;
	public final double longitude;
	public final double accuracy;
	
	public LocationData(double latitude, double longitude, double accuracy) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
	}
}