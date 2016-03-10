/*
 *
 * Helinium Studentapp
 *
 * Copyright (C) 2016 Bastiaan Teeuwen <bastiaan.teeuwen170@gmail.com>
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

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import java.util.Locale;

public class MyApplication extends Application {
	protected static final int ACTION_ONLINE = 0;
	protected static final int ACTION_ONLINE_1 = 1;
	protected static final int ACTION_OFFLINE = 2;
	protected static final int ACTION_OFFLINE_1 = 3;
	protected static final int ACTION_INIT_IN = 4;
	protected static final int ACTION_INIT_OUT = 5;
	protected static final int ACTION_SHORT_IN = 6;
	protected static final int ACTION_SHORT_OUT = 7;
	protected static final int ACTION_REFRESH_IN = 8;
	protected static final int ACTION_REFRESH_OUT = 9;
	protected static final int ACTION_NULL = 100;

	protected static final int DB_OK = 0;
	protected static final int DB_REFRESHING = 1;
	protected static final int DB_ERROR = 100;

	protected static final int DELAY_DRAWER = 200;
	protected static final int DELAY_RESTART = 1000;

	protected static final int DIREC_BACK = -1;
	protected static final int DIREC_CURRENT = 0;
	protected static final int DIREC_NEXT = 1;
	protected static final int DIREC_OTHER = 100;

	protected static final int OK = 0;
	protected static final int ERR_LOGIN = 1;
	protected static final int ERR_USERPASS = 2;
	protected static final int ERR_IO = 3;
	protected static final int ERR_OK = 4;
	protected static final int ERR_UNDEFINED = 100;
	protected static final int ERR_RETRY = 1000;

	protected static final int FOCUS_YEAR = 1000;

	protected static final int TIMEOUT_CONNECT = 7000;
	protected static final int TIMEOUT_READ = 7000;

	protected static final int VIEW_LOGIN = 0;
	protected static final int VIEW_SCHEDULE = 1;
	protected static final int VIEW_SCHEDULE_HOMEWORK = 2;
	protected static final int VIEW_GRADES = 3;

	protected static final String CHARSET = "UTF-8";

	protected static final String URL_LOGIN= "https://leerlingen.helinium.nl/login?passAction=login";
	protected static final String URL_SCHEDULE = "https://leerlingen.helinium.nl/Portaal/Rooster?wis_ajax&ajax_object=293&startdate293=";
	protected static final String URL_HOMEWORK = "https://leerlingen.helinium.nl/fs/SOMTools/Comps/Agenda.cfc?&method=getLeerlingRooster&so_id=319&start=";
	protected static final String URL_GRADES = "https://leerlingen.helinium.nl/Portaal/Cijfers?wis_ajax&ajax_object=291&startdate=";
	protected static final String URL_UPDATE_CHANGELOG = "https://dl.dropboxusercontent.com/u/9920547/BT/heliniumleerlingenweb/changelog";
	protected static final String URL_UPDATE_BETA = "https://dl.dropboxusercontent.com/u/9920547/BT/heliniumleerlingenweb/beta/app-release.apk";
	protected static final String URL_UPDATE_RELEASE = "https://dl.dropboxusercontent.com/u/9920547/BT/heliniumleerlingenweb/release/app-release.apk";
	protected static final String URL_GITHUB = "https://github.com/bteeuwen170/heliniumstudentapp";

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		setLocale(this);
	}

	protected static void setLocale(Context context) {
		Locale locale = null;
		final Configuration config = new Configuration();

		switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_general_language", "0"))) {
			case 0:
				locale = Resources.getSystem().getConfiguration().locale; //TODO Or just Locale.getDefault()?
				break;
			case 1:
				locale = new Locale("nl");
				break;
			case 2:
				locale = Locale.US;
				break;
		}

		assert locale != null;
		Locale.setDefault(locale);
		config.locale = locale;
		context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
	}
}