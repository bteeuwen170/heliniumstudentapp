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
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.widget.RelativeLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HeliniumStudentApp extends Application
{
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
	protected static final int VIEW_GRADES = 2;

	protected static final String CHARSET = "UTF-8";
	protected static final Locale LOCALE = Locale.GERMANY;

	protected static final String URL_LOGIN = "https://leerlingen.helinium.nl/login?passAction=login";
	protected static final String URL_SCHEDULE = "https://leerlingen.helinium.nl/fs/SOMTools/Comps/Agenda.cfc?&method=getLeerlingRooster&so_id=319&start=";
	protected static final String URL_GRADES = "https://leerlingen.helinium.nl/Portaal/Cijfers?wis_ajax&ajax_object=291&startdate=";
	protected static final String URL_UPDATE_CHANGELOG = "https://dl.dropboxusercontent.com/u/9920547/BT/heliniumstudentapp/changelog";
	protected static final String URL_UPDATE_RELEASE = "https://dl.dropboxusercontent.com/u/9920547/BT/heliniumstudentapp/bin/app-release.apk";
	protected static final String URL_EMAIL = "bastiaan.teeuwen170@gmail.com";
	protected static final String URL_GITHUB = "https://github.com/bteeuwen170/heliniumstudentapp";

	protected static SimpleDateFormat df_homework()
	{ //TODO Move single used or only in class used back
		return new SimpleDateFormat("dd-MM-yyyy z", LOCALE);
	}

	protected static SimpleDateFormat df_save()
	{
		return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", LOCALE);
	}

	protected static SimpleDateFormat df_date()
	{
		return new SimpleDateFormat("dd-MM-yyyy", LOCALE);
	}

	protected static SimpleDateFormat df_weekday()
	{
		return new SimpleDateFormat("EEEE", MainActivity.mainContext.getResources().getConfiguration().locale);
	}

	protected static SimpleDateFormat df_hours()
	{
		return new SimpleDateFormat("HH", LOCALE);
	}

	protected static SimpleDateFormat df_minutes()
	{
		return new SimpleDateFormat("mm", LOCALE);
	}

	protected static String df_grades(final int yearFocus)
	{
		final Date date = new Date();
		return new SimpleDateFormat("dd-MM-", LOCALE).format(date) + (Integer.valueOf(new SimpleDateFormat("yyyy", LOCALE).format(date)) + yearFocus);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		final TypedArray styledAttributes = getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize}); //FIXME This kind of works, but really doesn't
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) MainActivity.toolbarTB.getLayoutParams();
		layoutParams.height = (int) styledAttributes.getDimension(0, 0);
		MainActivity.toolbarTB.setLayoutParams(layoutParams);

		setLocale(this);
	}

	protected static void setLocale(Context context)
	{
		Locale locale;

		switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("pref_general_language", "0"))) {
		case 1:
			locale = new Locale("nl");
			break;
		case 2:
			locale = Locale.US;
			break;
		default:
			locale = Locale.getDefault();
		}

		Locale.setDefault(locale);

		final Configuration config = new Configuration();
		config.locale = locale;
		context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
	}
}