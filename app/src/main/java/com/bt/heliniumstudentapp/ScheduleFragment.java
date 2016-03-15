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
import android.support.v7.internal.view.ContextThemeWrapper;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

public class ScheduleFragment extends Fragment {
	private static AppCompatActivity mainContext;
	protected static View scheduleLayout;
	private static boolean init;

	protected static String scheduleHtml, homeworkJson;
	protected static int scheduleFocus;

	private static ListView weekDaysLV;

	@SuppressWarnings("ConstantConditions")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
		mainContext = (AppCompatActivity) getActivity();
		scheduleLayout = inflater.inflate(R.layout.fragment_schedule, viewGroup, false);

		MainActivity.setToolbarTitle(mainContext, getResources().getString(R.string.schedule), null);

		weekDaysLV = (ListView) scheduleLayout.findViewById(R.id.lv_weekDays_fs);

		final boolean restart = PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean("forced_restart", false);

		if (restart) PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putBoolean("forced_restart", false).apply();

		if (restart) { //TODO Database check?
			MainActivity.setStatusBar(mainContext);

			scheduleFocus = new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR);

			scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null);
			homeworkJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("json_homework_0", null);

			if (MainActivity.isOnline())
				parseData(HeliniumStudentApp.ACTION_ONLINE);
			else
				parseData(HeliniumStudentApp.ACTION_OFFLINE);
		} else if (scheduleHtml == null) {
			final boolean online = MainActivity.isOnline();

			scheduleFocus = new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR);

			if (online && PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean("pref_updates_auto_update", true))
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
					new UpdateClass(mainContext, false).execute();
				else
					new UpdateClass(mainContext, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

			if (online && (PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean("pref_schedule_init", true) ||
					PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null) == null)) {
				getSchedule(HeliniumStudentApp.DIREC_CURRENT, HeliniumStudentApp.ACTION_INIT_IN);
			} else if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING) {
				MainActivity.setStatusBar(mainContext);

				scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null);
				homeworkJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("json_homework_0", null);

				if (online)
					parseData(HeliniumStudentApp.ACTION_ONLINE);
				else
					parseData(HeliniumStudentApp.ACTION_OFFLINE);
			}
		}

		((SwipeRefreshLayout) scheduleLayout).setColorSchemeResources(MainActivity.accentSecondaryColor, MainActivity.accentPrimaryColor, MainActivity.primaryColor);
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
						final int currentWeek = new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR);

						if (scheduleFocus > currentWeek + 1) {
							scheduleFocus = currentWeek + 1;
							scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_1", null);
							homeworkJson = null;
						} else {
							scheduleFocus = currentWeek;
							scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null);
							homeworkJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("json_homework_0", null);
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

						final AlertDialog.Builder weekpickerDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(mainContext, MainActivity.themeDialog));

						final View view = LayoutInflater.from(mainContext).inflate(R.layout.dialog_schedule, null);
						weekpickerDialogBuilder.setView(view);

						final DatePicker datePicker = (DatePicker) view.findViewById(R.id.np_weekpicker_dw);

						year = new GregorianCalendar(Locale.GERMANY).get(Calendar.YEAR);
						monthOfYear = new GregorianCalendar(Locale.GERMANY).get(Calendar.MONTH);
						dayOfMonth = new GregorianCalendar(Locale.GERMANY).get(Calendar.DAY_OF_MONTH);

						weekpickerDialogBuilder.setTitle(getResources().getString(R.string.go_to));

						weekpickerDialogBuilder.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								final GregorianCalendar date = new GregorianCalendar(Locale.GERMANY);
								final GregorianCalendar today = new GregorianCalendar(Locale.GERMANY);

								date.set(Calendar.YEAR, year);
								date.set(Calendar.MONTH, monthOfYear);
								date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
								date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
								today.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

								scheduleFocus = (today.get(Calendar.WEEK_OF_YEAR)) - ((int) ((today.getTimeInMillis() / (1000 * 60 * 60 * 24 * 7)) - (date.getTimeInMillis() / (1000 * 60 * 60 * 24 * 7))));

								getSchedule(HeliniumStudentApp.DIREC_OTHER, HeliniumStudentApp.ACTION_REFRESH_IN);
							}
						});

						weekpickerDialogBuilder.setNegativeButton(getString(android.R.string.cancel), null);

						final AlertDialog weekPickerDialog = weekpickerDialogBuilder.create();

						final GregorianCalendar minDate = new GregorianCalendar(Locale.GERMANY);
						minDate.set(Calendar.YEAR, 2000);
						minDate.set(Calendar.WEEK_OF_YEAR, 1);

						final GregorianCalendar maxDate = new GregorianCalendar(Locale.GERMANY);
						maxDate.set(Calendar.YEAR, 2038);
						maxDate.set(Calendar.WEEK_OF_YEAR, 1);

						datePicker.init(year, monthOfYear, dayOfMonth, new DatePicker.OnDateChangedListener() {
							final GregorianCalendar newDate = new GregorianCalendar(Locale.GERMANY);

							@Override
							public void onDateChanged(DatePicker view, int dialogYear, int dialogMonthOfYear, int dialogDayOfMonth) {
								newDate.set(year, monthOfYear, dayOfMonth);

								year = dialogYear;
								monthOfYear = dialogMonthOfYear;
								dayOfMonth = dialogDayOfMonth;

								if (minDate != null && minDate.after(newDate))
									view.init(minDate.get(Calendar.YEAR), minDate.get(Calendar.MONTH), minDate.get(Calendar.DAY_OF_MONTH), this);
								else if (maxDate != null && maxDate.before(newDate))
									view.init(maxDate.get(Calendar.YEAR), maxDate.get(Calendar.MONTH), maxDate.get(Calendar.DAY_OF_MONTH), this);
								else
									view.init(year, monthOfYear, dayOfMonth, this);
							}
						});

						weekPickerDialog.setCanceledOnTouchOutside(true);
						weekPickerDialog.show();

						weekPickerDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(MainActivity.accentSecondaryColor));
						weekPickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(MainActivity.accentSecondaryColor));
					} else {
						scheduleFocus = new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR);
						scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null);
						homeworkJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("json_homework_0", null);

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
						final int currentWeek = new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR);

						if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_1", null) != null && scheduleFocus >= currentWeek) {
							scheduleFocus = currentWeek + 1;
							scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_1", null);
							homeworkJson = null;
						} else {
							scheduleFocus = currentWeek;
							scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null);
							homeworkJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("json_homework_0", null);
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
				if (scheduleHtml != null && PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null) != null && checkDatabase() != HeliniumStudentApp.DB_REFRESHING)
					parseData(HeliniumStudentApp.ACTION_ONLINE);
			} else {
				if (checkDatabase() != HeliniumStudentApp.DB_REFRESHING) {
					final int currentWeek = new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR);

					if (scheduleFocus != currentWeek && scheduleFocus != currentWeek + 1) {
						scheduleFocus = currentWeek;
						scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null);
						homeworkJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("json_homework_0", null);
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
					final Snackbar noConnectionSB = Snackbar.make(mainContext.findViewById(R.id.cl_snackbar_am), R.string.error_conn_no, Snackbar.LENGTH_LONG).setAction(R.string.retry, new OnClickListener() {

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

	private static void getSchedule(final int direction, final int transition) {
		MainActivity.setUI(HeliniumStudentApp.VIEW_SCHEDULE, transition);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			new GetScheduleData().execute(HeliniumStudentApp.URL_SCHEDULE + getDateFormatted(), scheduleFocus, direction, transition + 1, true);
		else
			new GetScheduleData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					HeliniumStudentApp.URL_SCHEDULE + getDateFormatted(), scheduleFocus, direction, transition + 1, true);
	}

	protected static class GetScheduleData extends AsyncTask<Object, Void, Integer> {
		private String html;

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
					connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + HeliniumStudentApp.CHARSET);
					connection.addRequestProperty("Cookie", TextUtils.join(",", MainActivity.cookies.getCookieStore().getCookies()));

					connection.connect();

					final Scanner line = new Scanner(connection.getInputStream()).useDelimiter("\\A");
					html = line.hasNext() ? line.next() : "";

					((HttpURLConnection) connection).disconnect();

					if (((HttpURLConnection) connection).getResponseCode() == 200)
						if (html.contains("ajax-loader.gif"))
							return HeliniumStudentApp.ERR_RETRY;
						else if (html.contains("<h2>Er is een fout opgetreden</h2>") || html.contains("Leerlingnummer onbekend") || !html.contains("Week")) //TODO Also html.contains("cross.png")?
							return HeliniumStudentApp.ERR_UNDEFINED;
						else
							return HeliniumStudentApp.OK;
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
						new MainActivity.GetLoginCookie().execute(HeliniumStudentApp.VIEW_SCHEDULE, url, focus, direction, transition, display);
					else
						new MainActivity.GetLoginCookie().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, HeliniumStudentApp.VIEW_SCHEDULE, url, focus, direction, transition, display);
					break;
				case HeliniumStudentApp.ERR_RETRY:
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
						new GetScheduleData().execute(url, focus, direction, transition, display);
					else
						new GetScheduleData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, focus, direction, transition, display);
					break;
				case HeliniumStudentApp.ERR_OK:
				case HeliniumStudentApp.ERR_UNDEFINED:
					MainActivity.recoverError(HeliniumStudentApp.VIEW_SCHEDULE, returnCode, direction, transition);
					break;
				case HeliniumStudentApp.OK:
					if (display) {
						scheduleHtml = html;
						try {
							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
								new GetHomeworkData().execute(
										HeliniumStudentApp.URL_HOMEWORK + String.valueOf(new SimpleDateFormat("dd-MM-yyyy z").parse(url.substring(85) + " GMT").getTime()), focus, direction, transition, scheduleFocus);
							else
								new GetHomeworkData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
										HeliniumStudentApp.URL_HOMEWORK + String.valueOf(new SimpleDateFormat("dd-MM-yyyy z").parse(url.substring(85) + " GMT").getTime()), focus, direction, transition, scheduleFocus);
						} catch (ParseException e) {
							//FIXME Handle immediately (and fix)
						}
					}

					if (focus == new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR)) {
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_0", html).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_schedule_version_0", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date())).apply();

						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_0", getDateFormatted()).apply();

						if (PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean("pref_schedule_next_week", false) && direction == HeliniumStudentApp.DIREC_CURRENT) {
							if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
								new GetScheduleData().execute(HeliniumStudentApp.URL_SCHEDULE + getDateFormatted(), focus + 1, 0, transition, false);
							else
								new GetScheduleData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, HeliniumStudentApp.URL_SCHEDULE + getDateFormatted(), focus + 1, 0, transition, false);
						}
					} else if (focus == new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR) + 1 &&
							PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean("pref_schedule_next_week", false)) {
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_1", html).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_schedule_version_1", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date())).apply();

						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_1", getDateFormatted()).apply();
					}
					break;
			}
		}
	}

	protected static class GetHomeworkData extends AsyncTask<Object, Void, Integer> {
		private String json;

		private String url;
		private int focus;
		private int direction;
		private int transition;

		@Override
		protected Integer doInBackground(Object... params) {
			url = (String) params[0];
			focus = (int) params[1];
			direction = (int) params[2];
			transition = (int) params[3];

			if (MainActivity.cookies == null) {
				return HeliniumStudentApp.ERR_LOGIN;
			} else {
				try {
					final URLConnection connection = new URL(url).openConnection();

					connection.setConnectTimeout(HeliniumStudentApp.TIMEOUT_CONNECT);
					connection.setReadTimeout(HeliniumStudentApp.TIMEOUT_READ);

					((HttpURLConnection) connection).setInstanceFollowRedirects(false);
					connection.setRequestProperty("Accept-Charset", HeliniumStudentApp.CHARSET);
					connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + HeliniumStudentApp.CHARSET);
					connection.addRequestProperty("Cookie", TextUtils.join(",", MainActivity.cookies.getCookieStore().getCookies()));

					connection.connect();

					final Scanner line = new Scanner(connection.getInputStream()).useDelimiter("\\A");
					json = line.hasNext() ? line.next() : "";

					((HttpURLConnection) connection).disconnect();

					final int responseCode = ((HttpURLConnection) connection).getResponseCode();

					if (responseCode == 201 || responseCode == 202) //TODO Do more research on this, performance can possibly be improved
						return HeliniumStudentApp.ERR_RETRY;
					else if (responseCode == 200)
						//if (json.contains("null")) //TODO Enter
						//	return HeliniumStudentApp.ERR_UNDEFINED;
						//else
						return HeliniumStudentApp.OK;
					else
						return HeliniumStudentApp.ERR_OK;
				} catch (IOException e) {
					return HeliniumStudentApp.ERR_LOGIN; //TODO WTF, extremely unlikely
				}
			}
		}

		@Override
		protected void onPostExecute(Integer returnCode) {
			switch (returnCode) {
				case HeliniumStudentApp.ERR_LOGIN:
				case HeliniumStudentApp.ERR_OK:
				case HeliniumStudentApp.ERR_UNDEFINED:
					MainActivity.recoverError(HeliniumStudentApp.VIEW_SCHEDULE_HOMEWORK, returnCode, direction, transition);
					break;
				case HeliniumStudentApp.ERR_RETRY:
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
						new GetHomeworkData().execute(url, focus, direction, transition);
					else
						new GetHomeworkData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, focus, direction, transition, focus);
					break;
				case HeliniumStudentApp.OK:
					homeworkJson = json;

					if (focus == new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR)) PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("json_homework_0", json).apply();

					parseData(transition);
					break;
			}
		}
	}

	private int checkDatabase() {
		Boolean updated = false;

		final GregorianCalendar currentDate = new GregorianCalendar(Locale.GERMANY);
		final GregorianCalendar storedDate = new GregorianCalendar(Locale.GERMANY);

		final String scheduleStart = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_start_0", null);

		if (scheduleStart == null) {
			return HeliniumStudentApp.DB_OK;
		} else {
			try {
				storedDate.setTime(new SimpleDateFormat("dd-MM-yyyy").parse(scheduleStart));
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

			if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_1", null) == null) {
				if (MainActivity.isOnline()) {
					MainActivity.setStatusBar(mainContext);

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_0", null).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("json_homework_0", null).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_1", null).apply();

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_0", null).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_1", null).apply();

					ScheduleFragment.getSchedule(HeliniumStudentApp.DIREC_CURRENT, HeliniumStudentApp.ACTION_SHORT_IN);

					return HeliniumStudentApp.DB_REFRESHING; //TODO Handle by caller to avoid workarounds
				} else {
					Toast.makeText(mainContext, mainContext.getString(R.string.error_database) + ". " + mainContext.getString(R.string.error_conn_no) + ".", Toast.LENGTH_SHORT).show();
					mainContext.finish();

					return HeliniumStudentApp.DB_ERROR; //TODO Throw error / in finally
				}
			} else try {
				currentDate.setTime(new SimpleDateFormat("dd-MM-yyyy").parse(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_start_0", "1")));

				if (currentDate.get(Calendar.WEEK_OF_YEAR) - currentDate.get(Calendar.WEEK_OF_YEAR) == 1) {
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_0",
							PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_1", null)).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_1", null).apply();

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("json_homework_0", null).apply();

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_schedule_version_0",
							PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_schedule_version_1", null)).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_schedule_version_1", null).apply();

					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_0",
							PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_start_1", null)).apply();
					PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_1", null).apply();

					scheduleHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_schedule_0", null);
					homeworkJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("json_homework_0", null);

					return HeliniumStudentApp.DB_OK;
				} else {
					if (MainActivity.isOnline()) {
						MainActivity.setStatusBar(mainContext);

						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_0", null).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("json_homework_0", null).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_1", null).apply();

						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_0", null).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_schedule_start_1", null).apply();

						ScheduleFragment.getSchedule(HeliniumStudentApp.DIREC_CURRENT, HeliniumStudentApp.ACTION_SHORT_IN);

						return HeliniumStudentApp.DB_REFRESHING; //TODO Handle by caller to avoid workarounds
					} else {
						Toast.makeText(mainContext, mainContext.getString(R.string.error_database) + ". " + mainContext.getString(R.string.error_conn_no) + ".", Toast.LENGTH_SHORT).show();
						mainContext.finish();

						return HeliniumStudentApp.DB_ERROR; //TODO Throw error / in finally
					}
				}
			} catch (ParseException ignored) {}

			return HeliniumStudentApp.DB_ERROR; //TODO Throw error / in finally
		}
	}

	private static GregorianCalendar getDate() {
		GregorianCalendar date = new GregorianCalendar(Locale.GERMANY);
		date.clear();
		date.set(Calendar.YEAR, new GregorianCalendar(Locale.GERMANY).get(Calendar.YEAR)); //FIXME BAD
		date.set(Calendar.WEEK_OF_YEAR, scheduleFocus);
		date.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

		return date;
	}

	private static String getDateFormatted() {
		return new SimpleDateFormat("dd-MM-yyyy").format(getDate().getTime());
	}

	protected static void parseData(final int transition) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			new ParseData().execute(transition);
		else
			new ParseData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transition);
	}

	private static class ParseData extends AsyncTask<Object, Void, ArrayList<HashMap<String, String>>> {
		private AppCompatActivity mainContext;
		private int transition;

		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(final Object... attrs) {
			mainContext = ScheduleFragment.mainContext;
			transition = (Integer) attrs[0];

			final ArrayList<HashMap<String, String>> scheduleWrapper = new ArrayList<>();
			final GregorianCalendar today = new GregorianCalendar(Locale.GERMANY);

			for (int day = 2; day < 7; day ++) {
				final GregorianCalendar weekDays = getDate();
				weekDays.set(Calendar.DAY_OF_WEEK, day);

				HashMap<String,String> item = new HashMap<>();

				final String weekDay = new SimpleDateFormat("EEEE").format(weekDays.getTime());

				item.put("weekDay", Character.toUpperCase(weekDay.charAt(0)) + weekDay.substring(1));
				item.put("date", DateFormat.getDateFormat(mainContext).format(weekDays.getTime()));

				if (scheduleHtml.contains("</i> " + new String[] { "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag" }[day - 2]))
					item.put("unavailable", "false");
				else
					item.put("unavailable", "true");

				if (scheduleFocus == today.get(Calendar.WEEK_OF_YEAR) && day == today.get(Calendar.DAY_OF_WEEK))
					item.put("today", "true");
				else
					item.put("today", "false");

				scheduleWrapper.add(item);
			}

			if (scheduleFocus == today.get(Calendar.WEEK_OF_YEAR) || scheduleFocus == today.get(Calendar.WEEK_OF_YEAR) + 1) { //TODO Timer? What about leap years or daylight saving?
				final HashMap<String,String> item = new HashMap<>();
				final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				final char ws = ' ';

				try {
					long timeDifference;

					if (scheduleFocus == today.get(Calendar.WEEK_OF_YEAR))
						timeDifference = System.currentTimeMillis() - format.parse(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_schedule_version_0", "")).getTime();
					else
						timeDifference = System.currentTimeMillis() - format.parse(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_schedule_version_1", "")).getTime();

					final long minutes = timeDifference / (60 * 1000) % 60;
					final long hours = timeDifference / (60 * 60 * 1000) % 24;
					final long days = timeDifference / (24 * 60 * 60 * 1000);

					if (days == 0)
						if (hours == 0)
							if (minutes == 1)
								item.put("updated", mainContext.getString(R.string.updated_last) + ws + minutes + ws + mainContext.getString(R.string.minute) + ws + mainContext.getString(R.string.ago));
							else
								item.put("updated", mainContext.getString(R.string.updated_last) + ws + minutes + ws + mainContext.getString(R.string.minutes) + ws + mainContext.getString(R.string.ago));
						else
						if (hours == 1)
							item.put("updated", mainContext.getString(R.string.updated_last) + ws + hours + ws + mainContext.getString(R.string.hour) + ws + mainContext.getString(R.string.ago));
						else
							item.put("updated", mainContext.getString(R.string.updated_last) + ws + hours + ws + mainContext.getString(R.string.hours) + ws + mainContext.getString(R.string.ago));
					else
					if (days == 1)
						item.put("updated", mainContext.getString(R.string.updated_last) + ws + days + ws + mainContext.getString(R.string.day) + ws + mainContext.getString(R.string.ago));
					else
						item.put("updated", mainContext.getString(R.string.updated_last) + ws + days + ws + mainContext.getString(R.string.days) + ws + mainContext.getString(R.string.ago));

					scheduleWrapper.add(item);
				} catch (ParseException ignored) {}
			}

			return scheduleWrapper;
		}

		@Override
		protected void onPostExecute(final ArrayList<HashMap<String, String>> wrapper) {
			final ScheduleAdapter scheduleLVadapter = new ScheduleAdapter(wrapper);
			weekDaysLV.setAdapter(scheduleLVadapter);

			scheduleLVadapter.notifyDataSetChanged();

			if (scheduleFocus == new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR))
				MainActivity.weekTV.setTypeface(null, Typeface.BOLD);
			else
				MainActivity.weekTV.setTypeface(null, Typeface.NORMAL);

			final GregorianCalendar date = getDate();
			MainActivity.weekTV.setText("Week " + String.valueOf(date.get(Calendar.WEEK_OF_YEAR)));
			MainActivity.yearTV.setText(String.valueOf(date.get(Calendar.YEAR)));

			String classExtra = "";
			if (!PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_general_class", "0").equals("0"))
				classExtra = " (" + mainContext.getString(R.string.general_class) + ' ' + PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_general_class", "") + ')';

			String nameHtml = scheduleHtml; //TODO Implement differently
			nameHtml = nameHtml.substring(nameHtml.indexOf("Rooster van "));
			((TextView) mainContext.findViewById(R.id.tv_name_hd)).setText((nameHtml.substring(12, nameHtml.indexOf(" ("))));
			((TextView) mainContext.findViewById(R.id.tv_class_hd)).setText((nameHtml.substring(nameHtml.indexOf("(") + 1, nameHtml.indexOf(")")) + classExtra));

			weekDaysLV.setOnItemClickListener(new AdapterView.OnItemClickListener() { //TODO Move

				@Override
				public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
					mainContext.startActivity(new Intent(mainContext, DayActivity.class).putExtra("chosen_day", position).putExtra("chosen_date", wrapper.get(position).get("date")));
				}
			});

			MainActivity.setUI(HeliniumStudentApp.VIEW_SCHEDULE, transition);
		}
	}

	private static class ScheduleAdapter extends BaseAdapter {
		private ArrayList<HashMap<String, String>> weekDays;

		public ScheduleAdapter(ArrayList<HashMap<String, String>> objects) {
			weekDays = objects;
		}

		public int getCount() {
			return weekDays.size();
		}

		public Object getItem(int position) {
			return weekDays.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup viewGroup) {
			final LayoutInflater inflater = (LayoutInflater) mainContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			final String updated = weekDays.get(position).get("updated");

			if (updated != null) {
				convertView = inflater.inflate(R.layout.listitem_footer, viewGroup, false);
				convertView.setEnabled(false);
				convertView.setOnClickListener(null);

				final TextView averageTV = (TextView) convertView.findViewById(R.id.tv_footer_lf);

				averageTV.setTextColor(mainContext.getResources().getColor(MainActivity.themeSecondaryTextColor));

				averageTV.setText(updated);
			} else {
				convertView = inflater.inflate(R.layout.listitem_schedule, viewGroup, false);

				final TextView dayTV = (TextView) convertView.findViewById(R.id.tv_day_lm);
				final TextView dateTV = (TextView) convertView.findViewById(R.id.tv_date_lm);

				if (MainActivity.themeColor == R.color.theme_dark) {
					convertView.setBackgroundResource(R.drawable.listselector_dark);
				} else {
					convertView.setBackgroundResource(R.drawable.listselector_light);
				}

				if (weekDays.get(position).get("today").equals("true")) {
					dayTV.setTypeface(Typeface.DEFAULT_BOLD);
					dateTV.setTypeface(Typeface.DEFAULT_BOLD);
				}

				if (weekDays.get(position).get("unavailable").equals("true")) {
					convertView.setEnabled(false);
					convertView.setOnClickListener(null);

					dayTV.setTextColor(mainContext.getResources().getColor(MainActivity.themeDisabledTextColor));
					dateTV.setTextColor(mainContext.getResources().getColor(MainActivity.themeDisabledTextColor));
				} else {
					dayTV.setTextColor(mainContext.getResources().getColor(MainActivity.themePrimaryTextColor));
					dateTV.setTextColor(mainContext.getResources().getColor(MainActivity.themeSecondaryTextColor));
				}

				dayTV.setText(weekDays.get(position).get("weekDay"));
				dateTV.setText(weekDays.get(position).get("date"));
			}

			return convertView;
		}
	}
}