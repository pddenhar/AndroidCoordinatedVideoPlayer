package com.fewstreet.coordinatedvideoplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by peter on 8/17/16.
 */
public class SettingsActivity extends PreferenceActivity {
    private final String TAG = "SettingsActivity";
    private static final int FILE_SELECT_CODE = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceFragment frag = new MyPreferenceFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, frag).commit();
    }

    @Override
    public void startActivity(Intent intent) {
        if (intent.hasExtra("hasResult")) {
            super.startActivityForResult(intent, FILE_SELECT_CODE);
        } else {
            super.startActivity(intent);
        }
    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    String filePath = null;
                    Uri _uri = data.getData();
                    getContentResolver().takePersistableUriPermission(_uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("filePicker", _uri.toString());
                    editor.commit();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
