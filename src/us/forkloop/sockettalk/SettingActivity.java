package us.forkloop.sockettalk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingActivity extends PreferenceActivity {
	
	SharedPreferences sharePref;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preference);
        PreferenceManager.setDefaultValues(this, R.layout.preference, false);
        sharePref = PreferenceManager.getDefaultSharedPreferences(this);
        
        //setContentView(R.layout.preference);
    }
}
