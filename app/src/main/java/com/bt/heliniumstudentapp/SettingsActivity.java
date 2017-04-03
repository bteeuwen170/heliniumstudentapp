/*
 *
 * Helinium Studentapp
 *
 * Copyright (C) 2016 Bastiaan Teeuwen <bastiaan@mkcl.nl>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 */

package com.bt.heliniumstudentapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import java.text.ParseException;
import java.util.Arrays;

public class SettingsActivity extends PreferenceActivity { //TODO PreferenceFragment
	private static AppCompatActivity mainContext;

	private static Toolbar toolbarTB;

	private static boolean pendingRestart;

	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		final View result = super.onCreateView(name, context, attrs);
		if (result != null) return result;

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			switch (name) {
				case "EditText":
					return new AppCompatEditText(this, attrs);
				case "Spinner":
					return new AppCompatSpinner(this, attrs);
				case "CheckBox":
					return new AppCompatCheckBox(this, attrs);
				case "RadioButton":
					return new AppCompatRadioButton(this, attrs);
				case "CheckedTextView":
					return new AppCompatCheckedTextView(this, attrs);
			}

		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mainContext = MainActivity.mainContext;

		pendingRestart = false;

		setContentView(R.layout.activity_settings);

		toolbarTB = (Toolbar) findViewById(R.id.tb_toolbar_as);
		toolbarTB.setNavigationOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		setToolbar();

		addPreferencesFromResource(R.layout.preferencescreen_settings);

		getListView().setBackgroundColor(Color.TRANSPARENT);
		getListView().setCacheColorHint(Color.TRANSPARENT);
		getListView().setBackgroundColor(ContextCompat.getColor(this, MainActivity.themeColor));

		setTheme(MainActivity.themeSettings);

		MainActivity.setStatusBar(this);

		setTitles();

		findPreference("pref_general_class").setSummary(Arrays.asList(getResources().getStringArray(R.array.general_class_array)).get(
				Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_general_class", "0"))));

		findPreference("pref_general_language").setSummary(Arrays.asList(getResources().getStringArray(R.array.general_language_array)).get(
				Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_general_language", "0"))));

		findPreference("pref_customization_theme").setSummary(Arrays.asList(getResources().getStringArray(R.array.customization_theme_array)).get(
				Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_customization_theme", "0"))));

		findPreference("pref_customization_color_primary").setSummary(Arrays.asList(getResources().getStringArray(R.array.customization_color_primary_array)).get(
				Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_customization_color_primary", "4"))));

		findPreference("pref_customization_color_accent").setSummary(Arrays.asList(getResources().getStringArray(R.array.customization_color_accent_array)).get(
				Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_customization_color_accent", "14"))));

		try {
			final String date = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_schedule_version_0", "");

			if (date.equals(""))
				findPreference("pref_schedule_version_0").setSummary(getString(R.string.database_no));
			else
				findPreference("pref_schedule_version_0").setSummary(DateFormat.getDateFormat(getApplicationContext()).format(
						HeliniumStudentApp.df_save().parse(date)) + date.substring(date.indexOf(' ')));
		} catch (ParseException ignored) {}

		try {
			final String date = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_schedule_version_1", "");

			if (date.equals(""))
				findPreference("pref_schedule_version_1").setSummary(getString(R.string.database_no));
			else
				findPreference("pref_schedule_version_1").setSummary(DateFormat.getDateFormat(getApplicationContext()).format(
						HeliniumStudentApp.df_save().parse(date)) + date.substring(date.indexOf(' ')));
		} catch (ParseException ignored) {}

		try {
			final String date = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_grades_version", "");

			if (date.equals(""))
				findPreference("pref_grades_version").setSummary(getString(R.string.database_no));
			else
				findPreference("pref_grades_version").setSummary(DateFormat.getDateFormat(getApplicationContext()).format(
						HeliniumStudentApp.df_save().parse(date)) + date.substring(date.indexOf(' ')));
		} catch (ParseException ignored) {}

		findPreference("pref_grades_term").setSummary(Arrays.asList(getResources().getStringArray(R.array.grades_term_array)).get(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_grades_term", "1")) - 1));

		try {
			findPreference("pref_updates_check").setSummary(getString(R.string.updates_check_summary) + ' ' + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (PackageManager.NameNotFoundException ignored) {}

		findPreference("pref_general_class").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!((ListPreference) preference).getValue().equals(newValue)) {
					preference.setSummary(Arrays.asList(getResources().getStringArray(R.array.general_class_array)).get(Integer.parseInt(newValue.toString())));

					GradesFragment.gradesHtml = null;
					pendingRestart = true;
					return true;
				} else {
					return false;
				}
			}
		});

		findPreference("pref_general_language").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!((ListPreference) preference).getValue().equals(newValue)) {
					preference.setSummary(Arrays.asList(getResources().getStringArray(R.array.general_language_array)).get(Integer.parseInt(newValue.toString())));

					pendingRestart = true;
					return true;
				} else {
					return false;
				}
			}
		});

		findPreference("pref_customization_theme").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!((ListPreference) preference).getValue().equals(newValue)) {
					preference.setSummary(Arrays.asList(getResources().getStringArray(R.array.customization_theme_array)).get(Integer.parseInt(newValue.toString())));

					//MainActivity.setColors(Integer.parseInt(newValue.toString()), HeliniumStudentApp.ACTION_NULL, HeliniumStudentApp.ACTION_NULL);
					//setTheme(MainActivity.themeSettings);

					GradesFragment.gradesHtml = null;
					pendingRestart = true;
					return true;
				} else {
					return false;
				}
			}
		});

		findPreference("pref_customization_color_primary").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!((ListPreference) preference).getValue().equals(newValue)) {
					preference.setSummary(Arrays.asList(getResources().getStringArray(R.array.customization_color_primary_array)).get(Integer.parseInt(newValue.toString())));

					MainActivity.setColors(HeliniumStudentApp.ACTION_NULL, Integer.parseInt(newValue.toString()), HeliniumStudentApp.ACTION_NULL);
					setToolbar();

					GradesFragment.gradesHtml = null;
					pendingRestart = true;
					return true;
				} else {
					return false;
				}
			}
		});

		findPreference("pref_customization_color_accent").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!((ListPreference) preference).getValue().equals(newValue)) {
					preference.setSummary(Arrays.asList(getResources().getStringArray(R.array.customization_color_accent_array)).get(Integer.parseInt(newValue.toString())));

					MainActivity.setColors(HeliniumStudentApp.ACTION_NULL, HeliniumStudentApp.ACTION_NULL, Integer.parseInt(newValue.toString()));
					setTitles();

					GradesFragment.gradesHtml = null;
					pendingRestart = true;
					return true;
				} else {
					return false;
				}
			}
		});

		findPreference("pref_customization_compact").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				GradesFragment.gradesHtml = null;
				pendingRestart = true;

				return true;
			}
		});

		findPreference("pref_schedule_next_week").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				findPreference("pref_schedule_version_1").setSummary(getString(R.string.database_no));

				PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit().putString("html_schedule_1", null).apply();
				PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit().putString("pref_schedule_version_1", null).apply();

				return true;
			}
		});

		findPreference("pref_grades_term").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (!((ListPreference) preference).getValue().equals(newValue)) {
					findPreference("pref_grades_version").setSummary(getString(R.string.database_no));

					preference.setSummary(Arrays.asList(getResources().getStringArray(R.array.grades_term_array)).get(Integer.parseInt(newValue.toString()) - 1));

					PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit().putString("html_grades", null).apply();
					PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this).edit().putString("pref_grades_version", null).apply();

					GradesFragment.gradesHtml = null;

					pendingRestart = true;
					return true;
				} else {
					return false;
				}
			}
		});

		findPreference("pref_updates_check").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (MainActivity.isOnline()) {
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
						new UpdateClass(SettingsActivity.this, true).execute();
					else
						new UpdateClass(SettingsActivity.this, true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					Toast.makeText(SettingsActivity.this, getString(R.string.error_conn_no), Toast.LENGTH_SHORT).show();
				}
				return true;
			}
		});
	}

	private void setToolbar() {
		toolbarTB.setBackgroundColor(ContextCompat.getColor(this, MainActivity.primaryColor));

		MainActivity.setStatusBar(this);

		toolbarTB.getNavigationIcon().setColorFilter(ContextCompat.getColor(this, MainActivity.primaryTextColor), PorterDuff.Mode.SRC_ATOP);

		Spannable toolbarTitle = new SpannableString(getString(R.string.settings));
		toolbarTitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, MainActivity.primaryTextColor)), 0, toolbarTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		toolbarTB.setTitle(toolbarTitle);
	}

	private void setTitles() {
		final Spannable prefGeneral =  new SpannableString(getString(R.string.general));
		final Spannable prefCustomization =  new SpannableString(getString(R.string.customization));
		final Spannable prefSchedule =  new SpannableString(getString(R.string.schedule));
		final Spannable prefGrades =  new SpannableString(getString(R.string.grades));
		final Spannable prefUpdate =  new SpannableString(getString(R.string.updates));

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
			prefGeneral.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, MainActivity.accentSecondaryColor)), 0, prefGeneral.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefCustomization.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, MainActivity.accentSecondaryColor)), 0, prefCustomization.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefSchedule.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, MainActivity.accentSecondaryColor)), 0, prefSchedule.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefGrades.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, MainActivity.accentSecondaryColor)), 0, prefGrades.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefUpdate.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, MainActivity.accentSecondaryColor)), 0, prefUpdate.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else {
			prefGeneral.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_grey_dark)), 0, prefGeneral.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefCustomization.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_grey_dark)), 0, prefCustomization.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefSchedule.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_grey_dark)), 0, prefSchedule.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefGrades.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_grey_dark)), 0, prefGrades.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			prefUpdate.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.dark_grey_dark)), 0, prefUpdate.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		findPreference("pref_general").setTitle(prefGeneral);
		findPreference("pref_customization").setTitle(prefCustomization);
		findPreference("pref_schedule").setTitle(prefSchedule);
		findPreference("pref_grades").setTitle(prefGrades);
		findPreference("pref_updates").setTitle(prefUpdate);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (MainActivity.isOnline()) {
			findPreference("pref_schedule_next_week").setEnabled(true);
			findPreference("pref_grades_term").setEnabled(true);
			findPreference("pref_updates_check").setEnabled(true);
		} else {
			findPreference("pref_schedule_next_week").setEnabled(false);
			findPreference("pref_grades_term").setEnabled(false);
			findPreference("pref_updates_check").setEnabled(false);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) UpdateClass.downloadAPK();
	}

	@Override
	public void onBackPressed() {
		if (pendingRestart) {
			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("forced_restart", true).apply();

			mainContext.finish();
			startActivity(new Intent(this, MainActivity.class));
		} else {
			finish();
		}
	}
}
