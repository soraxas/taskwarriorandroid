package soraxas.taskw.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.kvj.bravo7.log.Logger;

import java.lang.reflect.Method;

import soraxas.taskw.App;
import soraxas.taskw.R;
import soraxas.taskw.data.AccountController;
import soraxas.taskw.data.Controller;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        Logger logger = Logger.forInstance(this);
        Controller controller = App.controller();

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Button to edit .taskrc
            Preference button = findPreference(getString(R.string.editTaskrc));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // hacky way to disable the runtime check of URI expose
                    // https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed/38858040
                    if (Build.VERSION.SDK_INT >= 24) {
                        try {
                            Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                            m.invoke(null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // Open taskrc for editing
                    Intent intent = new Intent(Intent.ACTION_EDIT);
//                Uri uri = FileProvider.getUriForFile(MainActivity.this,
//                        BuildConfig.APPLICATION_ID + ".provider",
//                        new File(ac.taskrc().getAbsolutePath()));
                    AccountController ac = controller.accountController(controller.currentAccount());
                    Uri uri = Uri.parse(String.format("file://%s", ac.taskrc().getAbsolutePath()));
                    intent.setDataAndType(uri, "text/plain");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivityForResult(intent, App.SETTINGS_REQUEST);
                    } catch (Exception e) {
                        logger.e(e, "Failed to edit file");
                        controller.toastMessage("No suitable plain text editors " +
                                "found", true);
                    }
                    return true;
                }
            });
        }
    }
}