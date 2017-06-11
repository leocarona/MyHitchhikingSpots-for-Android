package com.myhitchhikingspots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class Utils
{
	//metric radius
	public static double radiusToMeters(double radius)
	{
		return ((radius/0.015) * 1700); 
	}
	
	//testing if markers are within range:
	public static boolean isInRectangle(double centerX, double centerY, double radius, double x, double y) 
	{
		return x >= centerX - radius && x <= centerX + radius
				&& y >= centerY - radius && y <= centerY + radius;
	}

	// test if coordinate (x, y) is within a radius from coordinate (centerX, centerY)
	public static boolean isPointInCircle(double centerX, double centerY, double radius, double x, double y) 
	{
		if (isInRectangle(centerX, centerY, radius, x, y)) 
		{
			double dx = centerX - x;
			double dy = centerY - y;
			dx *= dx;
			dy *= dy;
			double distanceSquared = dx + dy;
			double radiusSquared = radius * radius;
			return distanceSquared <= radiusSquared;
		}
		return false;
	}
	
	//calculation of distance between two gps points using Haversine formula
	//http://en.wikipedia.org/wiki/Haversine_formula
	@SuppressLint("UseValueOf")
	public static double haversineDistance(double lat1, double lng1, double lat2, double lng2) 
	{
	    double earthRadius = 3958.75;
	    
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    
	    double dist = earthRadius * c;

	    int meterConversion = 1609;

	    //result returns in meters
	    return (dist * meterConversion);
	}
	
	public static boolean isNetworkAvailable(Context context) 
	{
	    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	public static boolean hasActiveInternetConnection(Context context) 
	{
	    if (isNetworkAvailable(context)) 
	    {
	        try 
	        {
	            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
	            urlc.setRequestProperty("User-Agent", "Test");
	            urlc.setRequestProperty("Connection", "close");
	            urlc.setConnectTimeout(1500); 
	            urlc.connect();
	            return (urlc.getResponseCode() == 200);
	        } 
	        catch (IOException e) 
	        {
	            Log.e("active internet check", "Error checking internet connection", e);
	        }
	    } 
	    else 
	    {
	        Log.d("active internet check", "No network available!");
	    }
	    return false;
	}
	
	//converts Stream to String
	public static String convertStreamToString(InputStream is) throws Exception
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		
		while ((line = reader.readLine()) != null)
		{
			sb.append(line).append("\n");
		}
		
		reader.close();
		
		return sb.toString();
	}
	
	//removes &quot; artefact strings from String values, also possibly other stuff
	public static String stringBeautifier(String stringToGetCorrected)
	{
		String quotesArtefact = "&quot;";
		String quotesArtefactCorrected = "\"";
		
		stringToGetCorrected = stringToGetCorrected.replaceAll(quotesArtefact, quotesArtefactCorrected);
		
		return stringToGetCorrected;
	}
	
	//comparing values
	public static double getMax(double a, double b)
	{
	    if (a > b) 
	    {
	        return a;
	    }
	    return b;
	}
	
	//comparing values
	public static double getMin(double a, double b)
	{
	    if (a < b) 
	    {
	        return a;
	    }
	    return b;
	}
	
}
