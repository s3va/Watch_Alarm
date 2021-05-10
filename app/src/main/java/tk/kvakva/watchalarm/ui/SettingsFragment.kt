package tk.kvakva.watchalarm.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tk.kvakva.watchalarm.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}