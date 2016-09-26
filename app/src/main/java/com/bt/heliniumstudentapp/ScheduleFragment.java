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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.TimeZone;

import static android.support.v4.content.ContextCompat.getColor;

public class ScheduleFragment extends Fragment
{
	protected enum extra_i {
		HOMEWORK(0),
		ABSENCE(1);

		private int i;

		extra_i(int i)
		{
			this.i = i;
		}

		protected int value()
		{
			return i;
		}
	}

	/* TODO Seperate */
	protected enum extra_t {
		HOMEWORK,		/* "huiswerk" */
		HOMEWORKDONE,	/* "huiswerk" + "gemaakt" */
		HOMEWORKLATE,	/* "huiswerk" + "gemaakt" */
		TEST,			/* "toets" */
		LATE,			/* "te laat-T" */
		FULOUGH,		/* "Schoolverlof" */
		MEDICAL,		/* "Ziek / medisch" */
		TRUANCY,		/* "Ongeoorloofd verzuim" */
		REMOVED,		/* "verwijderd uit de les" */
		ABSENT			/* Other reason for absence */
	}

	/* This . is . just a mess... */
	protected static class week implements Serializable
	{
		private final day[] days = new day[5];

		private String footer = "";

		//private int year;
		//private int week;

		//private week(final int year, final int week)
		private week()
		{
			//this.year = year;
			//this.week = week;
		}

		private day day_add(final int n)
		{
			return new day(n);
		}

		protected day day_get(final int n)
		{
			return days[n - 2];
		}

		private int day_get_index(final int n)
		{
			final ArrayList<Integer> t = new ArrayList<>();
			int i;

			for (i = 0; i < days.length; i++)
				if (days[i] != null)
					t.add(i);

			return t.get(n);
		}

		private int days_get()
		{
			int i, n = 0;

			for (i = 0; i < days.length; i++)
				if (days[i] != null)
					n++;

			return n;
		}

		private String footer_get()
		{
			return mainContext.getString(R.string.updated_last) + ' ' + footer + ' ' + mainContext.getString(R.string.ago);
		}

		protected class day implements Serializable
		{
			protected final ArrayList<hour> hours = new ArrayList<>();
			private final ArrayList<floating> floatings = new ArrayList<>();

			protected String day, date;
			private boolean today;

			private day(final int n)
			{
				final GregorianCalendar ld = getDate();
				ld.set(Calendar.DAY_OF_WEEK, n);

				final GregorianCalendar td = new GregorianCalendar(HeliniumStudentApp.LOCALE);
				final String wd = HeliniumStudentApp.df_weekday().format(ld.getTime());

				/* FIXME Inefficient */
				if (ld.get(Calendar.DAY_OF_YEAR) == td.get(Calendar.DAY_OF_YEAR) &&
						ld.get(Calendar.YEAR) == td.get(Calendar.YEAR))
					today = true;

				this.day = Character.toUpperCase(wd.charAt(0)) + wd.substring(1);
				this.date = DateFormat.getDateFormat(mainContext).format(ld.getTime());

				days[n - 2] = this;
			}

			private hour hour_add(final int n, final String course, final String classroom, final String teacher,
								  final String group)
			{
				return new hour(n, course, classroom, teacher, group);
			}

			protected hour hour_get(final int n)
			{
				return hours.get(n);
			}

			protected int hours_get()
			{
				return hours.size();
			}

			private floating floating_add(final extra_t type, final String course, final String text)
			{
				return new floating(type, course, text);
			}

			protected floating floating_get(final int n)
			{
				return floatings.get(n);
			}

			protected int floatings_get()
			{
				return floatings.size();
			}

			protected class hour implements Serializable
			{
				/* TODO Confirm 2 is max */
				private extra[] extras = new extra[2];

				protected int hour;
				protected String course;
				protected String classroom;
				protected String teacher;
				protected String group;

				private hour(int n, final String course, final String classroom, final String teacher,
							 final String group)
				{
					if (n < 0) {
						this.hour = n * -1;

						hours.add(this);

						return;
					}

					if (n > 2) {
						if (n < 5)
							n++;
						else if (n < 7)
							n += 2;
						else
							n += 3;
					}

					this.hour = n;
					this.course = course;
					this.classroom = classroom;
					this.teacher = teacher;
					this.group = group;

					hours.add(this);
				}

				private extra extra_add(final int n, final extra_t type, final String text)
				{
					return new extra(n, type, text);
				}

				protected extra extra_get(final int n)
				{
					return extras[n];
				}

				/* This can be any of the types defined in extra_t */
				protected class extra implements Serializable
				{
					protected extra_t type;
					protected String text;

					private extra(final int n, final extra_t type, final String text)
					{
						this.type = type;
						this.text = text;

						extras[n] = this;
					}
				}
			}

			/* This can be either extra_t.HOMEWORK, (extra_t.HOMEWORK_DONE ?) or extra_t.TEST */
			protected class floating implements Serializable
			{
				protected extra_t type;
				protected String course;
				protected String text;

				private floating(final extra_t type, final String course, final String text)
				{
					this.type = type;
					this.course = course;
					this.text = text;

					floatings.add(this);
				}
			}
		}
	}

	private static AppCompatActivity mainContext;
	protected static View scheduleLayout;
	private static boolean init;

	protected static String scheduleJson;
	protected static int scheduleFocus;

	//private static String student_name, student_class;

	private static ListView weekDaysLV;

	@SuppressWarnings("ConstantConditions")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
		mainContext = (AppCompatActivity) getActivity();
		scheduleLayout = inflater.inflate(R.layout.fragment_schedule, viewGroup, false);

		MainActivity.setToolbarTitle(mainContext, getResources().getString(R.string.schedule), null);

		weekDaysLV = (ListView) scheduleLayout.findViewById(R.id.lv_weekDays_fs);

		final boolean restart = PreferenceManager.getDefaultSharedPreferences(mainContext)
				.getBoolean("forced_restart", false);

		if (restart) PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
				.putBoolean("forced_restart", false).apply();

		if (restart) { //TODO Database check?
			MainActivity.setStatusBar(mainContext);

			scheduleFocus = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR);

			scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
					.getString("schedule_0", null);

			if (MainActivity.isOnline())
				parseData(HeliniumStudentApp.ACTION_ONLINE);
			else
				parseData(HeliniumStudentApp.ACTION_OFFLINE);
		} else if (scheduleJson == null) {
			final boolean online = MainActivity.isOnline();

			scheduleFocus = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR);

			if (online && PreferenceManager.getDefaultSharedPreferences(mainContext)
					.getBoolean("pref_updates_auto_update", true)) {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
					new UpdateClass(mainContext, false).execute();
				else
					new UpdateClass(mainContext, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}

			if (online && (PreferenceManager.getDefaultSharedPreferences(mainContext)
					.getBoolean("pref_schedule_init", true) ||
					PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_0", null) == null)) {
				getSchedule(HeliniumStudentApp.DIREC_CURRENT, HeliniumStudentApp.ACTION_INIT_IN);
			} else if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING) {
				MainActivity.setStatusBar(mainContext);

				scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_0", null);

				if (online)
					parseData(HeliniumStudentApp.ACTION_ONLINE);
				else
					parseData(HeliniumStudentApp.ACTION_OFFLINE);
			}
		}

		((SwipeRefreshLayout) scheduleLayout).setColorSchemeResources(MainActivity.accentSecondaryColor,
				MainActivity.accentPrimaryColor, MainActivity.primaryColor);
		((SwipeRefreshLayout) scheduleLayout).setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

			@Override
			public void onRefresh() {
				refresh();
			}
		});

		MainActivity.prevIV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING) {
					if (MainActivity.isOnline()) {
						scheduleFocus --;

						getSchedule(HeliniumStudentApp.DIREC_BACK, HeliniumStudentApp.ACTION_REFRESH_IN);
					} else {
						final int currentWeek = new GregorianCalendar(HeliniumStudentApp.LOCALE)
								.get(Calendar.WEEK_OF_YEAR);

						if (scheduleFocus > currentWeek + 1) {
							scheduleFocus = currentWeek + 1;
							scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
									.getString("schedule_1", null);
						} else {
							scheduleFocus = currentWeek;
							scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
									.getString("schedule_0", null);
						}

						parseData(HeliniumStudentApp.ACTION_OFFLINE);
					}
				}
			}
		});

		MainActivity.historyIV.setOnClickListener(new OnClickListener() { //FIXME Huge mess
			private int year;
			private int monthOfYear;
			private int dayOfMonth;

			@Override
			public void onClick(View v) {
				if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING) {
					if (MainActivity.isOnline()) {
						MainActivity.setUI(HeliniumStudentApp.VIEW_SCHEDULE, HeliniumStudentApp.ACTION_ONLINE);

						final AlertDialog.Builder weekpickerDialogBuilder =
								new AlertDialog.Builder(new ContextThemeWrapper(mainContext, MainActivity.themeDialog));

						final View view = View.inflate(mainContext, R.layout.dialog_schedule, null);
						weekpickerDialogBuilder.setView(view);

						final DatePicker datePicker = (DatePicker) view.findViewById(R.id.np_weekpicker_dw);

						year = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.YEAR);
						monthOfYear = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.MONTH);
						dayOfMonth = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.DAY_OF_MONTH);

						weekpickerDialogBuilder.setTitle(getString(R.string.go_to));

						weekpickerDialogBuilder.setPositiveButton(getString(android.R.string.ok),
								new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								final GregorianCalendar date = new GregorianCalendar(HeliniumStudentApp.LOCALE);
								final GregorianCalendar today = new GregorianCalendar(HeliniumStudentApp.LOCALE);

								date.set(Calendar.YEAR, year);
								date.set(Calendar.MONTH, monthOfYear);
								date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
								date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
								today.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

								scheduleFocus = (today.get(Calendar.WEEK_OF_YEAR)) -
										((int) ((today.getTimeInMillis() / (1000 * 60 * 60 * 24 * 7)) -
												(date.getTimeInMillis() / (1000 * 60 * 60 * 24 * 7))));

								getSchedule(HeliniumStudentApp.DIREC_OTHER, HeliniumStudentApp.ACTION_REFRESH_IN);
							}
						});

						weekpickerDialogBuilder.setNegativeButton(getString(android.R.string.cancel), null);

						final AlertDialog weekPickerDialog = weekpickerDialogBuilder.create();

						final GregorianCalendar minDate = new GregorianCalendar(HeliniumStudentApp.LOCALE);
						minDate.set(Calendar.YEAR, 2000);
						minDate.set(Calendar.WEEK_OF_YEAR, 1);

						final GregorianCalendar maxDate = new GregorianCalendar(HeliniumStudentApp.LOCALE);
						maxDate.set(Calendar.YEAR, 2038);
						maxDate.set(Calendar.WEEK_OF_YEAR, 1);

						datePicker.init(year, monthOfYear, dayOfMonth, new DatePicker.OnDateChangedListener() {
							final GregorianCalendar newDate = new GregorianCalendar(HeliniumStudentApp.LOCALE);

							@Override
							public void onDateChanged(DatePicker view, int dialogYear, int dialogMonthOfYear,
													  int dialogDayOfMonth) {
								newDate.set(year, monthOfYear, dayOfMonth);

								year = dialogYear;
								monthOfYear = dialogMonthOfYear;
								dayOfMonth = dialogDayOfMonth;

								if (minDate != null && minDate.after(newDate))
									view.init(minDate.get(Calendar.YEAR), minDate.get(Calendar.MONTH),
											minDate.get(Calendar.DAY_OF_MONTH), this);
								else if (maxDate != null && maxDate.before(newDate))
									view.init(maxDate.get(Calendar.YEAR), maxDate.get(Calendar.MONTH),
											maxDate.get(Calendar.DAY_OF_MONTH), this);
								else
									view.init(year, monthOfYear, dayOfMonth, this);
							}
						});

						weekPickerDialog.setCanceledOnTouchOutside(true);
						weekPickerDialog.show();

						weekPickerDialog.getButton(AlertDialog.BUTTON_POSITIVE)
								.setTextColor(getColor(mainContext, MainActivity.accentSecondaryColor));
						weekPickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
								.setTextColor(getColor(mainContext, MainActivity.accentSecondaryColor));
					} else {
						scheduleFocus = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR);
						scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
								.getString("schedule_0", null);

						parseData(HeliniumStudentApp.ACTION_OFFLINE);
					}
				}
			}
		});

		MainActivity.nextIV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING) {
					if (MainActivity.isOnline()) {
						scheduleFocus ++;

						getSchedule(HeliniumStudentApp.DIREC_NEXT, HeliniumStudentApp.ACTION_REFRESH_IN);
					} else {
						final int currentWeek = new GregorianCalendar(HeliniumStudentApp.LOCALE)
								.get(Calendar.WEEK_OF_YEAR);

						if (PreferenceManager.getDefaultSharedPreferences(mainContext)
								.getString("schedule_1", null) != null && scheduleFocus >= currentWeek) {
							scheduleFocus = currentWeek + 1;
							scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
									.getString("schedule_1", null);
						} else {
							scheduleFocus = currentWeek;
							scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
									.getString("schedule_0", null);
						}

						parseData(HeliniumStudentApp.ACTION_OFFLINE);
					}
				}
			}
		});

		return scheduleLayout;
	}

	@Override
	public void onResume() { //TODO Null check on database needed?
		super.onResume();

		if (init)
			if (MainActivity.isOnline()) {
				if (scheduleJson != null && PreferenceManager.getDefaultSharedPreferences(mainContext)
						.getString("schedule_0", null) != null && checkDatabase() != HeliniumStudentApp.DB_REFRESHING)
					parseData(HeliniumStudentApp.ACTION_ONLINE);
			} else {
				if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING) {
					final int currentWeek = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR);

					if (scheduleFocus != currentWeek && scheduleFocus != currentWeek + 1) {
						scheduleFocus = currentWeek;
						scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
								.getString("schedule_0", null);
					}

					parseData(HeliniumStudentApp.ACTION_OFFLINE);
				}
			}
		else
			init = true;
	}

	private void refresh() {
		if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING)
			if (MainActivity.isOnline()) {
				getSchedule(HeliniumStudentApp.DIREC_CURRENT, HeliniumStudentApp.ACTION_REFRESH_IN);
			} else {
				MainActivity.setUI(HeliniumStudentApp.VIEW_SCHEDULE, HeliniumStudentApp.ACTION_REFRESH_OUT);

				if (!MainActivity.displayingSnackbar) {
					final Snackbar noConnectionSB = Snackbar.make(mainContext.findViewById(R.id.cl_snackbar_am),
							R.string.error_conn_no, Snackbar.LENGTH_LONG)
							.setAction(R.string.retry, new OnClickListener() {

						@Override
						public void onClick(View v) {
							refresh();
						}
					});

					noConnectionSB.getView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

						@Override
						public void onViewAttachedToWindow(View v) {
							MainActivity.displayingSnackbar = true;
						}

						@Override
						public void onViewDetachedFromWindow(View v) {
							MainActivity.displayingSnackbar = false;
						}
					});

					noConnectionSB.show();
				}

				MainActivity.setUI(HeliniumStudentApp.VIEW_SCHEDULE, HeliniumStudentApp.ACTION_OFFLINE);
			}
	}

	/* FIXME TEMP */
	private static void getSchedule(final int direction, final int transition) {
		MainActivity.setUI(HeliniumStudentApp.VIEW_SCHEDULE, transition);

		try {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				new GetScheduleData().execute(HeliniumStudentApp.URL_SCHEDULE
						+ String.valueOf(HeliniumStudentApp.df_homework().parse(getDateFormatted() + " GMT").getTime()),
						scheduleFocus, direction, transition + 1, true);
			else
				new GetScheduleData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, HeliniumStudentApp.URL_SCHEDULE
						+ String.valueOf(HeliniumStudentApp.df_homework().parse(getDateFormatted() + " GMT").getTime()),
						scheduleFocus, direction, transition + 1, true);
		} catch (ParseException ignored) {}
	}

	protected static class GetScheduleData extends AsyncTask<Object, Void, Integer> {
		private String json;

		private String url;
		private int focus;
		private int direction;
		private int transition;
		private boolean display;

		@Override
		protected Integer doInBackground(Object... params) {
			url = (String) params[0];
			focus = (int) params[1];
			direction = (int) params[2];
			transition = (int) params[3];
			display = (boolean) params[4];

			if (MainActivity.cookies == null) {
				return HeliniumStudentApp.ERR_LOGIN;
			} else {
				try {
					final URLConnection connection = new URL(url).openConnection();

					connection.setConnectTimeout(HeliniumStudentApp.TIMEOUT_CONNECT);
					connection.setReadTimeout(HeliniumStudentApp.TIMEOUT_READ);

					((HttpURLConnection) connection).setInstanceFollowRedirects(false);
					connection.setRequestProperty("Accept-Charset", HeliniumStudentApp.CHARSET);
					connection.setRequestProperty("Content-Type",
							"application/x-www-form-urlencoded;charset=" + HeliniumStudentApp.CHARSET);
					connection.addRequestProperty("Cookie",
							TextUtils.join(",", MainActivity.cookies.getCookieStore().getCookies()));

					connection.connect();

					final Scanner line = new Scanner(connection.getInputStream()).useDelimiter("\\A");
					json = line.hasNext() ? line.next() : "";

					((HttpURLConnection) connection).disconnect();

					final int responseCode = ((HttpURLConnection) connection).getResponseCode();

					/* XXX Room for improvement here */
					if (responseCode == 201 || responseCode == 202)
						return HeliniumStudentApp.ERR_RETRY;
					else if (responseCode == 200)
						//if (json.contains("null")) //TODO Enter
						//	return HeliniumStudentApp.ERR_UNDEFINED;
						//else
						return HeliniumStudentApp.OK;
					else if (responseCode == 500) //FIXME Not always though
						return HeliniumStudentApp.ERR_LOGIN;
					else
						return HeliniumStudentApp.ERR_OK;
				} catch (IOException e) {
					return HeliniumStudentApp.ERR_LOGIN;
				}
			}
		}

		@Override
		protected void onPostExecute(Integer returnCode) {
			switch (returnCode) {
			case HeliniumStudentApp.ERR_LOGIN:
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
					new MainActivity.GetLoginCookie().execute(
							HeliniumStudentApp.VIEW_SCHEDULE, url, focus, direction, transition, display);
				else
					new MainActivity.GetLoginCookie().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
							HeliniumStudentApp.VIEW_SCHEDULE, url, focus, direction, transition, display);
				break;
			case HeliniumStudentApp.ERR_RETRY:
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
					new GetScheduleData().execute(url, focus, direction, transition, display);
				else
					new GetScheduleData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
							url, focus, direction, transition, display);
				break;
			case HeliniumStudentApp.ERR_OK:
			case HeliniumStudentApp.ERR_UNDEFINED:
				MainActivity.recoverError(HeliniumStudentApp.VIEW_SCHEDULE, returnCode, direction, transition);
				break;
			case HeliniumStudentApp.OK:
				if (display) {
					scheduleJson = json;
					parseData(transition);
				}

				if (focus == new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR)) {
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString(
							"schedule_0",json).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString(
							"pref_schedule_version_0", HeliniumStudentApp.df_save().format(new Date())).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString(
							"schedule_start_0", getDateFormatted()).apply();

					if (PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean(
							"pref_schedule_next_week", false) && direction == HeliniumStudentApp.DIREC_CURRENT) {
						try {
							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
								new GetScheduleData().execute(
										HeliniumStudentApp.URL_SCHEDULE + String.valueOf(
												HeliniumStudentApp.df_homework().parse(getDateFormatted() + " GMT")
														.getTime()), focus + 1, 0, transition, false);
							else
								new GetScheduleData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
										HeliniumStudentApp.URL_SCHEDULE + String.valueOf(
												HeliniumStudentApp.df_homework().parse(getDateFormatted() + " GMT")
														.getTime()), focus + 1, 0, transition, false);
						} catch (ParseException ignored) {}
					}
				} else if (focus == new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR) + 1 &&
						PreferenceManager.getDefaultSharedPreferences(mainContext)
								.getBoolean("pref_schedule_next_week", false)) {
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString(
							"schedule_1",json).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString(
							"pref_schedule_version_1", HeliniumStudentApp.df_save().format(new Date())).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString(
							"schedule_start_1", getDateFormatted()).apply();
				}
				break;
			}
		}
	}

	private int checkDatabase() {
		Boolean updated = false;

		final GregorianCalendar currentDate = new GregorianCalendar(HeliniumStudentApp.LOCALE);
		final GregorianCalendar storedDate = new GregorianCalendar(HeliniumStudentApp.LOCALE);

		final String scheduleStart =
				PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_start_0", null);

		if (scheduleStart == null) {
			return HeliniumStudentApp.DB_OK;
		} else {
			try {
				storedDate.setTime(HeliniumStudentApp.df_date().parse(scheduleStart));
			} catch (ParseException ignored) {
				return HeliniumStudentApp.DB_ERROR;
			}
		}

		for (int weekDays = 0; weekDays < 7; weekDays ++) {
			if (currentDate.get(Calendar.YEAR) == storedDate.get(Calendar.YEAR) &&
					currentDate.get(Calendar.MONTH) == storedDate.get(Calendar.MONTH) &&
					currentDate.get(Calendar.DAY_OF_MONTH) == storedDate.get(Calendar.DAY_OF_MONTH)) {
				updated = true;
				break;
			}

			storedDate.add(Calendar.DAY_OF_YEAR, 1);
		}

		if (updated) {
			return HeliniumStudentApp.DB_OK;
		} else {
			scheduleFocus = currentDate.get(Calendar.WEEK_OF_YEAR);

			if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_1", null) == null) {
				if (MainActivity.isOnline()) {
					MainActivity.setStatusBar(mainContext);

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_0",null).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_1", null).apply();

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_start_0", null).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_start_1", null).apply();

					ScheduleFragment.getSchedule(HeliniumStudentApp.DIREC_CURRENT, HeliniumStudentApp.ACTION_SHORT_IN);

					return HeliniumStudentApp.DB_REFRESHING; //TODO Handle by caller to avoid workarounds
				} else {
					Toast.makeText(mainContext, mainContext.getString(R.string.error_database) + ". "
							+ mainContext.getString(R.string.error_conn_no) + ".", Toast.LENGTH_SHORT).show();
					mainContext.finish();

					return HeliniumStudentApp.DB_ERROR; //TODO Throw error / in finally
				}
			} else try {
				currentDate.setTime(HeliniumStudentApp.df_date().parse(PreferenceManager
						.getDefaultSharedPreferences(mainContext).getString("schedule_start_0", "1")));

				if (currentDate.get(Calendar.WEEK_OF_YEAR) - currentDate.get(Calendar.WEEK_OF_YEAR) == 1) {
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_0",
							PreferenceManager.getDefaultSharedPreferences(mainContext)
									.getString("schedule_1", null)).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_1", null).apply();

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("pref_schedule_version_0",
							PreferenceManager.getDefaultSharedPreferences(mainContext)
									.getString("pref_schedule_version_1", null)).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("pref_schedule_version_1", null).apply();

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_start_0",
							PreferenceManager.getDefaultSharedPreferences(mainContext)
									.getString("schedule_start_1", null)).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
							.putString("schedule_start_1", null).apply();

					scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext)
							.getString("schedule_0", null);

					return HeliniumStudentApp.DB_OK;
				} else {
					if (MainActivity.isOnline()) {
						MainActivity.setStatusBar(mainContext);

						PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
								.putString("schedule_0", null).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
								.putString("schedule_1", null).apply();

						PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
								.putString("schedule_start_0", null).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
								.putString("schedule_start_1", null).apply();

						ScheduleFragment.getSchedule(HeliniumStudentApp.DIREC_CURRENT,
								HeliniumStudentApp.ACTION_SHORT_IN);

						return HeliniumStudentApp.DB_REFRESHING; //TODO Handle by caller to avoid workarounds
					} else {
						Toast.makeText(mainContext, mainContext.getString(R.string.error_database) + ". " +
								mainContext.getString(R.string.error_conn_no) + ".", Toast.LENGTH_SHORT).show();
						mainContext.finish();

						return HeliniumStudentApp.DB_ERROR; //TODO Throw error / in finally
					}
				}
			} catch (ParseException ignored) {}

			return HeliniumStudentApp.DB_ERROR; //TODO Throw error / in finally
		}
	}

	private static GregorianCalendar getDate() {
		GregorianCalendar date = new GregorianCalendar(HeliniumStudentApp.LOCALE);
		date.clear();
		date.set(Calendar.YEAR, new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.YEAR)); //FIXME BAD
		date.set(Calendar.WEEK_OF_YEAR, scheduleFocus);
		date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

		return date;
	}

	private static String getDateFormatted() {
		return HeliniumStudentApp.df_date().format(getDate().getTime());
	}

	protected static void parseData(final int transition) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			new ParseData().execute(transition);
		else
			new ParseData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transition);
	}

	private static class ParseData extends AsyncTask<Object, Void, week> {
		private AppCompatActivity mainContext;
		private int transition;

		@Override
		protected week doInBackground(final Object... attrs) {
			JSONObject json;
			JSONArray data;
			final week schedule = new week();
			final GregorianCalendar today = new GregorianCalendar(HeliniumStudentApp.LOCALE);
			int i, j = 0, k = -1, l;

			mainContext = ScheduleFragment.mainContext;
			transition = (Integer) attrs[0];

			try {
				json = (JSONObject) new JSONTokener(scheduleJson).nextValue();
				data = json.getJSONArray("events");

				for (i = 0; i < data.length(); i++) {
					final JSONObject hour_data = data.getJSONObject(i).getJSONObject("afspraakObject");

					final GregorianCalendar hour_start =
							new GregorianCalendar(TimeZone.getTimeZone("Europe/Amsterdam"), HeliniumStudentApp.LOCALE);
					hour_start.setTimeInMillis(data.getJSONObject(i).getLong("start"));
					final int day = hour_start.get(Calendar.DAY_OF_WEEK);

					final int hour = hour_data.getInt("lesuur");
					final String course = data.getJSONObject(i).getString("title");
					final String classroom = hour_data.getString("lokaal");
					/* TODO Option to choose between internal and external database */
					final String teacher = hour_data.getJSONArray("docent").getJSONObject(0).getString("afkorting");
					final String group = hour_data.getString("lesgroep");

					week.day day_p = schedule.day_get(day);

					/* XXX This is a mess..... Clean it up */
					if (day == k) {
						for (l = 1; j + l < hour; l++) {
							switch (j + l - 1) {
							case 2:
								day_p.hour_add(-3, null, null, null, null);
								break;
							case 4:
								day_p.hour_add(-6, null, null, null, null);
								break;
							case 6:
								day_p.hour_add(-9, null, null, null, null);
								break;
							}

							day_p.hour_add(j + l, null, null, null, null);
						}

						j += l - 1;

						if (hour > j) {
							switch (j) {
							case 2:
								day_p.hour_add(-3, null, null, null, null);
								break;
							case 4:
								day_p.hour_add(-6, null, null, null, null);
								break;
							case 6:
								day_p.hour_add(-9, null, null, null, null);
								break;
							}
						}
					}

					if (day_p == null) {
						day_p = schedule.day_add(day);
						k = day;

						for (l = 1; l < hour; l++)
							day_p.hour_add(l, null, null, null, null);
					}

					final week.day.hour hour_p =
							day_p.hour_add(hour, course, classroom, teacher, group);

					if (hour_data.has("huiswerk")) {
						final JSONObject hw_data = hour_data.getJSONObject("huiswerk");

						if (hw_data.has("toets") && hw_data.getBoolean("toets")) {
							hour_p.extra_add(extra_i.HOMEWORK.value(), extra_t.TEST,
									hw_data.getString("omschrijving"));
						} else if (hw_data.has("gemaakt")) {
							if (hw_data.getBoolean("gemaakt"))
								hour_p.extra_add(extra_i.HOMEWORK.value(), extra_t.HOMEWORKDONE,
									hw_data.getString("omschrijving"));
							else
								hour_p.extra_add(extra_i.HOMEWORK.value(), extra_t.HOMEWORKLATE,
										hw_data.getString("omschrijving"));
						} else {
							hour_p.extra_add(extra_i.HOMEWORK.value(), extra_t.HOMEWORK,
									hw_data.getString("omschrijving"));
						}
					}

					if (hour_data.has("absentie")) {
						final JSONObject ab_data = hour_data.getJSONObject("absentie");

						if (ab_data.has("reden")) {
							switch (ab_data.getString("reden")) {
							case "te laat-T":
								hour_p.extra_add(extra_i.ABSENCE.value(), extra_t.LATE,
										mainContext.getString(R.string.late));
								break;
							case "Schoolverlof":
								hour_p.extra_add(extra_i.ABSENCE.value(), extra_t.FULOUGH,
										mainContext.getString(R.string.fulough));
								break;
							case "Ziek / medisch":
								hour_p.extra_add(extra_i.ABSENCE.value(), extra_t.MEDICAL,
										mainContext.getString(R.string.medical));
								break;
							case "Ongeoorloofd verzuim":
								hour_p.extra_add(extra_i.ABSENCE.value(), extra_t.TRUANCY,
										mainContext.getString(R.string.truancy));
								break;
							case "verwijderd uit de les":
								hour_p.extra_add(extra_i.ABSENCE.value(), extra_t.REMOVED,
										mainContext.getString(R.string.removed));
								break;
							default:
								hour_p.extra_add(extra_i.ABSENCE.value(), extra_t.ABSENT,
										mainContext.getString(R.string.absent));
								break;
							}
						}
					}

					j = hour;
				}
			} catch (JSONException | NullPointerException e) {
				//TODO
				return null;
			}

			try {
				data = json.getJSONArray("huiswerkEvents");

				for (i = 0; i < data.length(); i++) {
					final JSONObject hw_data = data.getJSONObject(i);

					final String start = hw_data.getString("start");
					final GregorianCalendar hour_start =
							new GregorianCalendar(TimeZone.getTimeZone("Europe/Amsterdam"), HeliniumStudentApp.LOCALE);
					hour_start.setTime(new SimpleDateFormat("yyyy-MM-dd", HeliniumStudentApp.LOCALE)
							.parse(start.substring(0, start.indexOf('T'))));
					final int day = hour_start.get(Calendar.DAY_OF_WEEK);

					final String course = hw_data.getString("vak");

					week.day day_p = schedule.day_get(day);
					if (day_p == null)
						day_p = schedule.day_add(day);

					if (hw_data.has("toets") && hw_data.getBoolean("toets"))
						day_p.floating_add(extra_t.TEST, course, hw_data.getString("omschrijving"));
					else
						day_p.floating_add(extra_t.HOMEWORK, course, hw_data.getString("omschrijving"));
				}
			} catch (JSONException | ParseException e) {
				//TODO
				return null;
			}

			/* FIXME Returns wrong results */
			/* TODO Run at interval? */
			/* TODO What about leap years or daylight saving? */
			//if (scheduleFocus == today.get(Calendar.WEEK_OF_YEAR) ||
			//		scheduleFocus == today.get(Calendar.WEEK_OF_YEAR) + 1)
			if (scheduleFocus != today.get(Calendar.WEEK_OF_YEAR))
				return schedule;

			final SimpleDateFormat format = HeliniumStudentApp.df_save();

			try {
				long timeDifference;

				if (scheduleFocus == today.get(Calendar.WEEK_OF_YEAR))
					timeDifference = System.currentTimeMillis() - format.parse(PreferenceManager
							.getDefaultSharedPreferences(mainContext)
							.getString("pref_schedule_version_0", "")).getTime();
				else
					timeDifference = System.currentTimeMillis() - format.parse(PreferenceManager
							.getDefaultSharedPreferences(mainContext)
							.getString("pref_schedule_version_1", "")).getTime();

				final long minutes = timeDifference / (60 * 1000) % 60;
				final long hours = timeDifference / (60 * 60 * 1000) % 24;
				final long days = timeDifference / (24 * 60 * 60 * 1000);

				if (days == 0) {
					if (hours == 0) {
						if (minutes == 1)
							schedule.footer = minutes + ' ' + mainContext.getString(R.string.minute);
						else
							schedule.footer = minutes + ' ' + mainContext.getString(R.string.minutes);
					} else if (hours == 1) {
						schedule.footer = hours + ' ' + mainContext.getString(R.string.hour);
					} else {
						schedule.footer = hours + ' ' + mainContext.getString(R.string.hours);
					}
				} else {
					if (days == 1)
						schedule.footer = days + ' ' + mainContext.getString(R.string.day);
					else
						schedule.footer = days + ' ' + mainContext.getString(R.string.days);
				}
			} catch (ParseException ignored) {}

			return schedule;
		}

		@Override
		protected void onPostExecute(final week schedule) {
			/* TODO Handle */
			if (schedule == null)
				Toast.makeText(mainContext, "The new homework/schedule parser has failed :(", Toast.LENGTH_LONG).show();

			final ScheduleAdapter scheduleLVadapter = new ScheduleAdapter(schedule);
			weekDaysLV.setAdapter(scheduleLVadapter);

			scheduleLVadapter.notifyDataSetChanged();

			if (scheduleFocus == new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR))
				MainActivity.weekTV.setTypeface(null, Typeface.BOLD);
			else
				MainActivity.weekTV.setTypeface(null, Typeface.NORMAL);

			final GregorianCalendar date = getDate();
			MainActivity.weekTV.setText(mainContext.getString(R.string.week, date.get(Calendar.WEEK_OF_YEAR)));
			MainActivity.yearTV.setText(String.valueOf(date.get(Calendar.YEAR)));

			/*String nameHtml = scheduleJson; //TODO Implement differently
			nameHtml = nameHtml.substring(nameHtml.indexOf("Rooster van "));
			((TextView) mainContext.findViewById(R.id.tv_name_hd)).setText((nameHtml.substring(12, nameHtml.indexOf(" ("))));
			((TextView) mainContext.findViewById(R.id.tv_class_hd)).setText((nameHtml.substring(nameHtml.indexOf("(") + 1, nameHtml.indexOf(")")) +
					(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_general_class", "0").equals("0") ? "" :
							" (" + mainContext.getString(R.string.general_class) + ' ' + PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_general_class", "") + ')')));*/
			//((TextView) mainContext.findViewById(R.id.tv_name_hd)).setText("Name unavail.");
			//((TextView) mainContext.findViewById(R.id.tv_class_hd)).setText("Class unavail.");

			weekDaysLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(final AdapterView<?> parent, final View view, final int pos, final long id) {
					Bundle bundle = new Bundle();
					bundle.putSerializable("schedule", schedule);
					bundle.putInt("pos", (int) id);

					mainContext.startActivity(new Intent(mainContext, DayActivity.class).putExtras(bundle));
				}
			});

			MainActivity.setUI(HeliniumStudentApp.VIEW_SCHEDULE, transition);
		}
	}

	private static class ScheduleAdapter extends BaseAdapter {
		private week schedule;

		public ScheduleAdapter(final week schedule) {
			this.schedule = schedule;
		}

		public int getCount() {
			return schedule.days_get();
		}

		public Object getItem(int pos) {
			return null;
		}

		public long getItemId(int pos) {
			return schedule.day_get_index(pos);
		}

		public View getView(int pos, View convertView, ViewGroup viewGroup) {
			final LayoutInflater inflater =
					(LayoutInflater) mainContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			week.day day;
			TextView averageTV, dayTV, dateTV;

			if (pos == getCount()) {
				convertView = inflater.inflate(R.layout.listitem_footer, viewGroup, false);
				convertView.setEnabled(false);
				convertView.setOnClickListener(null);

				averageTV = (TextView) convertView.findViewById(R.id.tv_footer_lf);

				averageTV.setTextColor(getColor(mainContext, MainActivity.themeSecondaryTextColor));
				averageTV.setText(schedule.footer_get());

				return convertView;
			}

			convertView = inflater.inflate(R.layout.listitem_schedule, viewGroup, false);

			day = schedule.day_get(schedule.day_get_index(pos) + 2);

			dayTV = (TextView) convertView.findViewById(R.id.tv_day_lm);
			dateTV = (TextView) convertView.findViewById(R.id.tv_date_lm);

			if (MainActivity.themeColor == R.color.theme_dark)
				convertView.setBackgroundResource(R.drawable.listselector_dark);
			else
				convertView.setBackgroundResource(R.drawable.listselector_light);

			if (day.today) {
				dayTV.setTypeface(Typeface.DEFAULT_BOLD);
				dateTV.setTypeface(Typeface.DEFAULT_BOLD);
			}

			dayTV.setTextColor(getColor(mainContext, MainActivity.themePrimaryTextColor));
			dateTV.setTextColor(getColor(mainContext, MainActivity.themeSecondaryTextColor));

			dayTV.setText(day.day);
			dateTV.setText(day.date);

			return convertView;
		}
	}
}