package com.neuron.trafikanten.dataProviders;

public interface IGenericProviderHandler<T> {
    public abstract void onData(T data);
    public abstract void onExtra(int i, Object data);
    public abstract void onPostExecute(Exception e);
    public abstract void onPreExecute();
}
