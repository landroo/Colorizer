package org.landroo.colorizer;


import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsScreen extends PreferenceActivity 
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}