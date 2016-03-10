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
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradesFragment extends Fragment {
	private static AppCompatActivity mainContext;
	protected static View gradesLayout;
	private static boolean init;

	protected static String gradesHtml;
	protected static int termFocus, maxYear, yearFocus;

	private static ExpandableListView gradesELV;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
		mainContext = (AppCompatActivity) getActivity();
		gradesLayout = inflater.inflate(R.layout.fragment_grades, viewGroup, false);

		boolean pass = true;

		if (gradesHtml == null) {
			termFocus = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_grades_term", "1"));
			yearFocus = 0;

			if (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_general_class", "0")) == 0) {
				try { //TODO Improve
					maxYear = Integer.parseInt(((TextView) mainContext.findViewById(R.id.tv_class_hd)).getText().toString().replaceAll("\\D+", ""));
				} catch (NumberFormatException e) {
					pass = false;

					MainActivity.drawerNV.getMenu().findItem(R.id.i_schedule_md).setChecked(true);
					MainActivity.FM.beginTransaction().replace(R.id.fl_container_am, new ScheduleFragment(), "SCHEDULE").commit();

					final AlertDialog.Builder classDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(mainContext, MainActivity.themeDialog));

					classDialogBuilder.setTitle(R.string.error);
					classDialogBuilder.setMessage(R.string.error_class);

					classDialogBuilder.setCancelable(false);

					classDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							mainContext.startActivity(new Intent(mainContext, SettingsActivity.class));
						}
					});

					classDialogBuilder.setNegativeButton(android.R.string.cancel, null);

					final AlertDialog classDialog = classDialogBuilder.create();

					classDialog.setCanceledOnTouchOutside(false);
					classDialog.show();

					classDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(MainActivity.accentSecondaryColor));
					classDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(MainActivity.accentSecondaryColor));
				}
			} else {
				maxYear = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_general_class", "1"));
			}
		}

		if (pass) {
			MainActivity.setToolbarTitle(mainContext, getResources().getString(R.string.grades), null);

			gradesELV = (ExpandableListView) gradesLayout.findViewById(R.id.lv_course_fg);

			final boolean online = MainActivity.isOnline();

			if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null) == null) { //TODO Simpler
				if (online) {
					getGrades(termFocus, new SimpleDateFormat("dd-MM-yyyy").format(new Date()), MyApplication.DIREC_CURRENT, MyApplication.ACTION_INIT_IN);
				} else { //TODO Display empty GradesFragment with retry option
					Toast.makeText(mainContext, getResources().getString(R.string.database_no), Toast.LENGTH_SHORT).show();

					MainActivity.drawerNV.getMenu().findItem(R.id.i_schedule_md).setChecked(true);
					MainActivity.FM.beginTransaction().replace(R.id.fl_container_am, new ScheduleFragment(), "SCHEDULE").commit();
				}
			} else if (online && gradesHtml == null && PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean("pref_grades_init", true)) {
				getGrades(termFocus, new SimpleDateFormat("dd-MM-yyyy").format(new Date()), MyApplication.DIREC_CURRENT, MyApplication.ACTION_INIT_IN);
			} else {
				if (gradesHtml == null) gradesHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null);

				if (online)
					parseData(MyApplication.ACTION_ONLINE);
				else
					parseData(MyApplication.ACTION_OFFLINE);
			}

			((SwipeRefreshLayout) gradesLayout).setColorSchemeResources(MainActivity.accentSecondaryColor, MainActivity.accentPrimaryColor, MainActivity.primaryColor);
			((SwipeRefreshLayout) gradesLayout).setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

				@Override
				public void onRefresh() {
					refresh();
				}
			});

			gradesELV.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
				int previousPosition = -1;

				@Override
				public void onGroupExpand(int position) {
					if (position != previousPosition) gradesELV.collapseGroup(previousPosition);
					previousPosition = position;
				}
			});

			gradesELV.setOnChildClickListener(new ExpandableListView.OnChildClickListener() { //Just a little easter egg
				int clickCount = 1;

				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int position, long id) {
					if (clickCount >= 80) {
						Toast.makeText(mainContext, "Is this what you wanted?", Toast.LENGTH_SHORT).show();
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/dQw4w9WgXcQ")));
					} else {
						switch (clickCount) {
							case 2:
								Toast.makeText(mainContext, "Good for you!", Toast.LENGTH_SHORT).show();
								break;
							case 10:
								Toast.makeText(mainContext, "You're really proud of that, aren't you?", Toast.LENGTH_SHORT).show();
								break;
							case 20:
								Toast.makeText(mainContext, "It's really not that big of a deal...", Toast.LENGTH_SHORT).show();
								break;
							case 40:
								Toast.makeText(mainContext, "You can stop now.", Toast.LENGTH_SHORT).show();
								break;
							case 50:
								Toast.makeText(mainContext, "Please...", Toast.LENGTH_SHORT).show();
							case 60:
								Toast.makeText(mainContext, "FUCK OFF!", Toast.LENGTH_SHORT).show();
								break;
						}
					}

					clickCount ++;
					return false;
				}
			});

			MainActivity.prevIV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (MainActivity.isOnline()) {
						if (termFocus != 1) {
							termFocus--;

							Date date = new Date();
							getGrades(termFocus, new SimpleDateFormat("dd-MM-").format(date) + (Integer.valueOf(new SimpleDateFormat("yyyy").format(date)) + yearFocus), MyApplication.DIREC_BACK,
									MyApplication.ACTION_REFRESH_IN);
						} else {
							MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_ONLINE);
						}
					} else {
						final int databaseFocus = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_grades_term", "1"));

						if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null) != null && yearFocus == 0 && termFocus > databaseFocus) {
							yearFocus = 0;
							termFocus = databaseFocus;

							gradesHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null);
							parseData(MyApplication.ACTION_OFFLINE);
						} else {
							MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_OFFLINE);
						}
					}
				}
			});

			MainActivity.historyIV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (MainActivity.isOnline()) {
						if (maxYear != 1) {
							MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_ONLINE);

							final AlertDialog.Builder gradesDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(mainContext, MainActivity.themeDialog));
							final View gradesLayout = LayoutInflater.from(mainContext).inflate(R.layout.dialog_grades, null);

							gradesDialogBuilder.setTitle(R.string.year);

							final NumberPicker yearNP = (NumberPicker) gradesLayout.findViewById(R.id.np_year_dg);

							gradesDialogBuilder.setView(gradesLayout);

							gradesDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (MainActivity.isOnline()) {
										final Date date = new Date();
										final int oldValue = yearFocus;

										yearFocus = yearNP.getValue() - maxYear;
										getGrades(termFocus,
												new SimpleDateFormat("dd-MM-").format(date) + (Integer.valueOf(new SimpleDateFormat("yyyy").format(date)) + yearFocus), oldValue + MyApplication.FOCUS_YEAR,
												MyApplication.ACTION_REFRESH_IN);
									} else {
										Toast.makeText(mainContext, getResources().getString(R.string.error_conn_no), Toast.LENGTH_SHORT).show();
									}
								}
							});

							yearNP.setMinValue(1);
							yearNP.setMaxValue(maxYear);

							yearNP.setValue(maxYear);

							java.lang.reflect.Field[] pickerFields = NumberPicker.class.getDeclaredFields();
							for (java.lang.reflect.Field pf : pickerFields) {
								if (pf.getName().equals("mSelectionDivider")) {
									pf.setAccessible(true);

									try {
										pf.set(yearNP, new ColorDrawable(getResources().getColor(MainActivity.accentPrimaryColor)));
									} catch (IllegalArgumentException | IllegalAccessException ignored) {}
									break;
									/*} else if(pf.getName().equals("mSelectorWheelPaint")) {
										pf.setAccessible(true);

										try {
											((Paint) pf.get(yearNP)).setColor(getResources().getColor(MainActivity.themePrimaryTextColor));
										} catch (IllegalArgumentException | IllegalAccessException ignored) {}*///FIXME Doesn't work... yet
								} else if (pf.getName().equals("mInputText")) {
									pf.setAccessible(true);

									try {
										((EditText) pf.get(yearNP)).setTextColor(getResources().getColor(MainActivity.themePrimaryTextColor));
									} catch (IllegalArgumentException | IllegalAccessException ignored) {}
								}
							}

							yearNP.invalidate();

							gradesDialogBuilder.setNegativeButton(android.R.string.cancel, null);

							AlertDialog gradesDialog = gradesDialogBuilder.create();

							gradesDialog.setCanceledOnTouchOutside(true);
							gradesDialog.show();

							gradesDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(MainActivity.accentSecondaryColor));
							gradesDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(MainActivity.accentSecondaryColor));
						}
					} else {
						final int databaseFocus = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_grades_term", "1"));

						if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null) != null && yearFocus != 0 || termFocus != databaseFocus) {
							yearFocus = 0;
							termFocus = databaseFocus;

							gradesHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null);
							parseData(MyApplication.ACTION_OFFLINE);
						} else {
							MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_OFFLINE);
						}
					}
				}
			});

			MainActivity.nextIV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (MainActivity.isOnline()) {
						if (termFocus != 4) {
							final Date date = new Date();

							termFocus ++;

							getGrades(termFocus,
									new SimpleDateFormat("dd-MM-").format(date) + (Integer.valueOf(new SimpleDateFormat("yyyy").format(date)) + yearFocus), MyApplication.DIREC_NEXT, MyApplication.ACTION_REFRESH_IN);
						} else {
							MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_ONLINE);
						}
					} else {
						final int databaseFocus = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_grades_term", "1"));

						if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null) != null && yearFocus == 0 && termFocus < databaseFocus) {
							yearFocus = 0;
							termFocus = databaseFocus;

							gradesHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null);
							parseData(MyApplication.ACTION_OFFLINE);
						} else {
							MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_OFFLINE);
						}
					}
				}
			});
		}

		return gradesLayout;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (init)
			if (gradesHtml != null)
				if (MainActivity.isOnline())
					MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_ONLINE);
				else
					MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_OFFLINE);
		else
			init = true;
	}

	private void refresh() {
		if (MainActivity.isOnline()) {
			final Date date = new Date();
			getGrades(termFocus, new SimpleDateFormat("dd-MM-").format(date) + (Integer.valueOf(new SimpleDateFormat("yyyy").format(date)) + yearFocus), yearFocus + MyApplication.FOCUS_YEAR,
					MyApplication.ACTION_REFRESH_IN);
		} else {
			MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_REFRESH_OUT);

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

			MainActivity.setUI(MyApplication.VIEW_GRADES, MyApplication.ACTION_OFFLINE);
		}
	}

	protected static void getGrades(final int term, String date, final int direction, final int transition) {
		MainActivity.setUI(MyApplication.VIEW_GRADES, transition);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			new GetGradesData().execute(MyApplication.URL_GRADES + date + "&periode291=" + term, direction, transition + 1);
		else
			new GetGradesData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MyApplication.URL_GRADES + date + "&periode291=" + term, direction, transition + 1);
	}

	protected static class GetGradesData extends AsyncTask<Object, Void, Integer> {
		private String html;

		private String url;
		private int direction;
		private int transition;

		@Override
		protected Integer doInBackground(Object... params) {
			url = (String) params[0];
			direction = (int) params[1];
			transition = (int) params[2];

			if (MainActivity.cookies == null) {
				return MyApplication.ERR_LOGIN;
			} else {
				try {
					final URLConnection connection = new URL(url).openConnection();

					connection.setConnectTimeout(MyApplication.TIMEOUT_CONNECT);
					connection.setReadTimeout(MyApplication.TIMEOUT_READ);

					((HttpURLConnection) connection).setInstanceFollowRedirects(false);
					connection.setRequestProperty("Accept-Charset", MyApplication.CHARSET);
					connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + MyApplication.CHARSET);
					connection.addRequestProperty("Cookie", TextUtils.join(",", MainActivity.cookies.getCookieStore().getCookies()));

					connection.connect();

					final Scanner line = new Scanner(connection.getInputStream()).useDelimiter("\\A");
					html = line.hasNext() ? line.next() : "";

					((HttpURLConnection) connection).disconnect();

					if (((HttpURLConnection) connection).getResponseCode() == 200)
						//if (!html.contains("<th class=\"wp3-rotate\" width=\"1%\" title=\"Rapportcijfer\" alt=\"Rapportcijfer\">") || html.contains("ajax-loader.gif"))
						if (html.contains("ajax-loader.gif"))
							return MyApplication.ERR_RETRY;
						else if (html.contains("<h2>Er is een fout opgetreden</h2>") || html.contains("Leerlingnummer onbekend") || !html.contains("Periode") || html.contains("cross.png"))
							return MyApplication.ERR_UNDEFINED;
						else
							return MyApplication.OK;
					else
						return MyApplication.ERR_OK;
				} catch (IOException e) {
					return MyApplication.ERR_LOGIN;
				}
			}
		}

		@Override
		protected void onPostExecute(Integer returnCode) {
			switch (returnCode) {
				case MyApplication.ERR_LOGIN:
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
						new MainActivity.GetLoginCookie().execute(MyApplication.VIEW_GRADES, url, direction, transition);
					else
						new MainActivity.GetLoginCookie().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MyApplication.VIEW_GRADES, url, direction, transition);
					break;
				case MyApplication.ERR_RETRY:
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
						new GetGradesData().execute(url, direction, transition);
					else
						new GetGradesData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, direction, transition);
					break;
				case MyApplication.ERR_OK:
				case MyApplication.ERR_UNDEFINED:
					MainActivity.recoverError(MyApplication.VIEW_GRADES, returnCode, direction, transition);
					break;
				case MyApplication.OK:
					gradesHtml = html;

					if (termFocus == Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_grades_term", "1")) && yearFocus == 0) {
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_grades", html).apply();
						PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_grades_version", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date())).apply();
					}

					parseData(transition);
					break;
			}
		}
	}

	protected static void parseData(final int transition) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			new ParseData().execute(transition);
		else
			new ParseData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, transition);
	}

	private static class CourseWrapper {
		String course;
		String[][] grades;
		String average;
		String averageRound;

		private CourseWrapper(final String course, final String[][] grades, final String average, final String averageRound) {
			this.course = course;
			this.grades = grades;
			this.average = average;
			this.averageRound = averageRound;
		}

		protected int getGradesCount() {
			return grades.length - 1; //To subtract average which is included in the gradeCount. Temporary naturally speaking...
		}

		protected String getGrades() {
			final StringBuilder result = new StringBuilder();

			for (int i = 0; i < getGradesCount(); i++) result.append(grades[i][0]).append(grades[i][1]).append(" ");

			return result.toString();
		}
	}

	private static class GradesWrapper {
		ArrayList<CourseWrapper> courses;
		String average;

		private GradesWrapper(final ArrayList<CourseWrapper> courses, final String average) {
			this.courses = courses;
			this.average = average;
		}

		private String getAverage() {
			return average;
		}

		private ArrayList<CourseWrapper> getCourseWrapper() {
			return courses;
		}
	}

	protected static class ParseData extends AsyncTask<Object, Void, GradesWrapper> {
		private AppCompatActivity mainContext;
		private int transition;

		@Override
		protected GradesWrapper doInBackground(final Object... attrs) {
			mainContext = GradesFragment.mainContext;
			transition = (Integer) attrs[0];

			final ArrayList<CourseWrapper> gradesMap = new ArrayList<>();

			int courseCount = 0;
			int averageCount = 0;
			double averageTotal = 0;

			String localHTML = gradesHtml.substring(gradesHtml.indexOf("<th class=\"wp3-rotate\" width=\"1%\" title=\"Rapportcijfer\" alt=\"Rapportcijfer\">"), gradesHtml.indexOf("</wissearch:exclude>"));

			final Matcher courseMatcher = Pattern.compile("<span title").matcher(localHTML);
			while (courseMatcher.find()) courseCount ++;

			for (int currentCourseCount = 0; currentCourseCount < courseCount; currentCourseCount ++) {
				double courseAverage = 0.0;
				String courseAverageRound = "0";
				int gradeCount = 0;

				String courseHTML = localHTML.substring(localHTML.indexOf("<td class=\"vak\">") + 16);

				if (courseHTML.contains("<td class=\"vak\">"))
					courseHTML = courseHTML.substring(0, courseHTML.indexOf("<td class=\"vak\">"));
				else
					courseHTML = courseHTML.substring(0, courseHTML.indexOf("<div id=\"jTooltip\" class=\"jTooltip default\" style=\"display: none;\">"));

				localHTML = localHTML.replace("<td class=\"vak\">" + courseHTML, "");

				final Matcher courseCountBubbleGradeMatcher = Pattern.compile("<span class=\"triggerBubble wp3-cijfer \"").matcher(courseHTML);
				while (courseCountBubbleGradeMatcher.find()) gradeCount ++;

				if (gradeCount != 0) { //TODO Review once more
					final int bubbleCount = gradeCount;

					String courseBubbleHTML = courseHTML;

					for (int i = 0; i < bubbleCount; i++) {
						String courseBubbleGradeHTML = courseBubbleHTML.substring(courseBubbleHTML.indexOf("<span class=\"triggerBubble wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\""));
						courseBubbleGradeHTML = courseBubbleGradeHTML.substring(0, courseBubbleGradeHTML.indexOf("<span class='closeBubble' title='Sluiten'></span></span>") + 56);
						courseBubbleHTML = courseBubbleHTML.replace(courseBubbleGradeHTML, "");

						final Matcher courseBubbleGradeCountMatcher = Pattern.compile("<span class=\" wp3-cijfer \">").matcher(courseBubbleGradeHTML);
						while (courseBubbleGradeCountMatcher.find()) gradeCount --;
					}

					gradeCount -= bubbleCount;
				}

				final Matcher courseCountGradeMatcher = Pattern.compile("&lt;tr&gt;&lt;td&gt;Herkansing&lt;/u&gt;&lt;/td&gt;&lt;td&gt;:&lt;/td&gt;").matcher(courseHTML);
				while (courseCountGradeMatcher.find()) gradeCount ++;

				final Matcher courseCountAverageMatcher = Pattern.compile("&lt;tr&gt;&lt;td&gt;Toetscode&lt;/u&gt;&lt;/td&gt;&lt;td&gt;:&lt;/td&gt;&lt;td&gt;Berekend rapportcijfer&lt;/td&gt;&lt;/tr&gt;")
						.matcher(courseHTML);
				while (courseCountAverageMatcher.find()) gradeCount ++;

				final String[][] courseArray = new String[gradeCount][3];

				final Matcher courseCountAdviceMatcher = Pattern.compile("&lt;tr&gt;&lt;td&gt;Toetssoort&lt;/u&gt;&lt;/td&gt;&lt;td&gt;:&lt;/td&gt;&lt;td&gt;Advies&lt;/td&gt;&lt;/tr&gt;").matcher(courseHTML);
				while (courseCountAdviceMatcher.find()) gradeCount += 2;

				for (int i = 0; i < gradeCount; i++) {
					if (courseHTML.contains("<span class=\"triggerBubble wp3-cijfer \"")) { //FIXME Adjust to ensure order is right
						String gradesContainerHTML = courseHTML.substring(courseHTML.indexOf("<span class=\"triggerBubble wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"") + 75);
						gradesContainerHTML = gradesContainerHTML.substring(0, gradesContainerHTML.indexOf("<span class=\'closeBubble\' title=\'Sluiten\'>"));

						courseHTML = courseHTML.replace("<span class=\"triggerBubble wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"" + gradesContainerHTML, "");

						String wp3GradeContainerHTML = gradesContainerHTML.substring(0, gradesContainerHTML.indexOf("\" class=\"result__figure\"><span>"));
						wp3GradeContainerHTML = Html.fromHtml(wp3GradeContainerHTML).toString();

						String wp3GradeHTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<td><strong>Cijfer</strong></td><td>:</td><td><strong>") + 54);
						wp3GradeHTML = wp3GradeHTML.substring(0, wp3GradeHTML.indexOf("</strong></td></tr>"));

						final String wp3WeightHTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<tr><td>Weging</u></td><td>:</td><td>") + 37);
						final String wp3CodeHTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<tr><td>Toetscode</u></td><td>:</td><td>") + 40);
						final String wp3DescTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<tr><td>Beschrijving</u></td><td>:</td><td>") + 43);

						double wp3Grade = 0.0;
						try {
							wp3Grade = Double.parseDouble(wp3GradeHTML);
						} catch (NumberFormatException ignored) {}

						if (wp3GradeHTML.contains("S") || wp3GradeHTML.contains("O") || (wp3Grade < 5.5 && wp3Grade != 0.0)) {
							courseArray[i][0] = "<font color='#F44336'>" + wp3GradeHTML + "</font>";
						} else {
							courseArray[i][0] = wp3GradeHTML;
						}

						courseArray[i][1] = "<sup><small><font color='#0000FF'>" + wp3WeightHTML.substring(0, wp3WeightHTML.indexOf("</td></tr>")) + "</font></small></sup>";

						StringBuilder bubbleGrades = new StringBuilder();

						while (gradesContainerHTML.contains("<span class=\" wp3-cijfer \">")) {
							String bubbleGradeContainerHTML = gradesContainerHTML.substring(gradesContainerHTML.indexOf("<span class=\" wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"") + 62);
							bubbleGradeContainerHTML = bubbleGradeContainerHTML.substring(0, gradesContainerHTML.indexOf("\" class=\"result__figure\"><span>"));

							gradesContainerHTML = gradesContainerHTML.replace("<span class=\" wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"" + bubbleGradeContainerHTML, "");

							bubbleGradeContainerHTML = Html.fromHtml(bubbleGradeContainerHTML).toString();

							String bubbleGradeHTML = bubbleGradeContainerHTML.substring(bubbleGradeContainerHTML.indexOf("<td><strong>Deelcijfer</strong></td><td>:</td><td><strong>") + 58);
							bubbleGradeHTML = bubbleGradeHTML.substring(0, bubbleGradeHTML.indexOf("</strong></td></tr>"));

							final String bubbleWeightHTML = bubbleGradeContainerHTML.substring(bubbleGradeContainerHTML.indexOf("<tr><td>Weging</u></td><td>:</td><td>") + 37);
							final String bubbleCodeHTML = bubbleGradeContainerHTML.substring(bubbleGradeContainerHTML.indexOf("<tr><td>Toetscode</u></td><td>:</td><td>") + 40);
							final String bubbleDescHTML = bubbleGradeContainerHTML.substring(bubbleGradeContainerHTML.indexOf("<tr><td>Beschrijving</u></td><td>:</td><td>") + 43);

							double bubbleGrade = 0.0;
							try {
								bubbleGrade = Double.parseDouble(bubbleGradeHTML);
							} catch (NumberFormatException ignored) {}

							if (bubbleGradeHTML.contains("S") || bubbleGradeHTML.contains("O") || (bubbleGrade < 5.5 && bubbleGrade != 0.0)) {
								bubbleGrades.append("<br />&emsp;").append("<font color='#F44336'>").append(bubbleGradeHTML).append("</font>");
							} else {
								bubbleGrades.append("<br />&emsp;").append(bubbleGradeHTML);
							}

							bubbleGrades.append("<sup><small>" ).append(bubbleWeightHTML.substring(0, bubbleWeightHTML.indexOf("</td></tr>"))).append("</font></small></sup>").append(" ")
									.append("<b>").append(bubbleCodeHTML.substring(0, bubbleCodeHTML.indexOf("</td></tr>"))).append("</b> - ")
									.append(bubbleDescHTML.substring(0, bubbleDescHTML.indexOf("</td></tr>")));
						}

						courseArray[i][2] = "<b>" + wp3CodeHTML.substring(0, wp3CodeHTML.indexOf("</td></tr>")) + "</b> - " + wp3DescTML.substring(0, wp3DescTML.indexOf("</td></tr>")) + bubbleGrades.toString();
					} else if (courseHTML.contains("<span class=\" wp3-cijfer \">")) {
						String wp3GradeContainerHTML = courseHTML.substring(courseHTML.indexOf("<span class=\" wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"") + 62);
						wp3GradeContainerHTML = wp3GradeContainerHTML.substring(0, wp3GradeContainerHTML.indexOf("\" class=\"result__figure\"><span>"));

						courseHTML = courseHTML.replace("<span class=\" wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"" + wp3GradeContainerHTML, "");

						wp3GradeContainerHTML = Html.fromHtml(wp3GradeContainerHTML).toString();

						String wp3GradeHTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<td><strong>Cijfer</strong></td><td>:</td><td><strong>") + 54);
						wp3GradeHTML = wp3GradeHTML.substring(0, wp3GradeHTML.indexOf("</strong></td></tr>"));

						if (wp3GradeContainerHTML.contains("<tr><td>Toetssoort</u></td><td>:</td><td>Berekend rapportcijfer</td></tr>")) {
							if (wp3GradeHTML.matches("[a-zA-Z ]*\\d+.*")) { //TODO ^^^ Not in function so 1 doesn't have to be subtracted from array-length in post parsing
								courseAverage = Double.parseDouble(wp3GradeHTML);
							} else { //TODO Any other value possible?
								if (wp3GradeHTML.contains("S")) {
									courseAverage = 2.0;
								} else if (wp3GradeHTML.contains("O")) {
									courseAverage = 4.0;
								} else if (wp3GradeHTML.contains("V")) {
									courseAverage = 6.0;
								} else if (wp3GradeHTML.contains("G")) {
									courseAverage = 8.0;
								}
							}

							String averageRoundHTML = courseHTML.substring(courseHTML.indexOf("<span class=\" wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"") + 62);
							averageRoundHTML = averageRoundHTML.substring(0, averageRoundHTML.indexOf("\" class=\"result__figure\"><span>"));
							courseHTML = courseHTML.replace("<span class=\" wp3-cijfer \"><a href=\"javascript: void(0)\" rel=\"" + averageRoundHTML, "");

							averageRoundHTML = Html.fromHtml(averageRoundHTML).toString();
							averageRoundHTML = averageRoundHTML.substring(averageRoundHTML.indexOf("<td><strong>Cijfer</strong></td><td>:</td><td><strong>") + 54);
							averageRoundHTML = averageRoundHTML.substring(0, averageRoundHTML.indexOf("</strong></td></tr>"));

							courseAverageRound = averageRoundHTML;

							averageCount++;
							averageTotal += courseAverage;
						} else if (wp3GradeContainerHTML.contains("<tr><td>Toetssoort</u></td><td>:</td><td>Advies</td></tr>")) {
							//TODO Handle
						} else if (wp3GradeContainerHTML.contains("<tr><td>Herkansing</u></td><td>:</td><td>")) {
							final String wp3WeightHTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<tr><td>Weging</u></td><td>:</td><td>") + 37);
							final String wp3CodeHTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<tr><td>Toetscode</u></td><td>:</td><td>") + 40);
							final String wp3DescHTML = wp3GradeContainerHTML.substring(wp3GradeContainerHTML.indexOf("<tr><td>Beschrijving</u></td><td>:</td><td>") + 43);

							double wp3Grade = 0.0;
							try {
								wp3Grade = Double.parseDouble(wp3GradeHTML);
							} catch (NumberFormatException ignored) {}

							if ((wp3Grade < 5.5 && wp3Grade != 0.0) || wp3GradeHTML.contains("S") || wp3GradeHTML.contains("O")) {
								courseArray[i][0] = "<font color='#F44336'>" + wp3GradeHTML + "</font>";
							} else {
								courseArray[i][0] = wp3GradeHTML;
							}

							courseArray[i][1] = "<sup><small>" + wp3WeightHTML.substring(0, wp3WeightHTML.indexOf("</td></tr>")) + "</font></small></sup>";
							courseArray[i][2] = "<b>" + wp3CodeHTML.substring(0, wp3CodeHTML.indexOf("</td></tr>")) + "</b> - " + wp3DescHTML.substring(0, wp3DescHTML.indexOf("</td></tr>"));
						//} else {
							//TODO Handle any other possibilities
						}
					}
				}

				final String course = courseHTML.substring(courseHTML.indexOf("<span title"));

				try { //TODO Messy
					if (courseAverageRound.equals("S") || courseAverageRound.equals("O") || Integer.parseInt(courseAverageRound) < 6) {
						gradesMap.add(new CourseWrapper(Html.fromHtml(course.substring(0, course.indexOf("</td>"))).toString().trim(),
								courseArray, "<font color='#F44336'>" + String.valueOf(courseAverage), "<font color='#F44336'>" + courseAverageRound));
					} else {
						gradesMap.add(new CourseWrapper(Html.fromHtml(course.substring(0, course.indexOf("</td>"))).toString().trim(),
								courseArray, String.valueOf(courseAverage), courseAverageRound));
					}
				} catch (NumberFormatException ignored) {
					gradesMap.add(new CourseWrapper(Html.fromHtml(course.substring(0, course.indexOf("</td>"))).toString().trim(),
							courseArray, String.valueOf(courseAverage), courseAverageRound));
				}
			}

			final double average = averageTotal / averageCount;
			if (average < 5.5)
				return new GradesWrapper(gradesMap, "<font color='#F44336'>" + new DecimalFormat("#.#").format(average));
			else
				return new GradesWrapper(gradesMap, new DecimalFormat("#.#").format(average));
		}

		@Override
		protected void onPostExecute(final GradesWrapper wrapper) {
			final GradesAdapter gradesELVadapter = new GradesAdapter(wrapper);
			gradesELV.setAdapter(gradesELVadapter);

			gradesELVadapter.notifyDataSetChanged();

			if (termFocus == Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_grades_term", "1")) && yearFocus == 0)
				MainActivity.weekTV.setTypeface(null, Typeface.BOLD);
			else
				MainActivity.weekTV.setTypeface(null, Typeface.NORMAL);

			MainActivity.weekTV.setText(mainContext.getResources().getString(R.string.term) + ' ' + String.valueOf(termFocus));
			MainActivity.yearTV.setText(mainContext.getResources().getString(R.string.year) + ' ' + String.valueOf(maxYear + yearFocus));

			MainActivity.setUI(MyApplication.VIEW_GRADES, transition);
		}
	}

	private static class GradesAdapter extends BaseExpandableListAdapter {
		private ArrayList<CourseWrapper> gradesArray;
		private String average;

		private boolean compactView;

		public GradesAdapter(GradesWrapper objects) {
			gradesArray = objects.getCourseWrapper();
			average = objects.getAverage();

			compactView = PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean("pref_customization_compact", false);
		}

		@Override
		public int getGroupCount() {
			return gradesArray.size() + 1;
		}

		@Override
		public Object getGroup(int position) {
			return gradesArray.get(position);
		}

		@Override
		public long getGroupId(int position) {
			return position;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return gradesArray.get(groupPosition).getGradesCount();
		}

		@Override
		public Object getChild(int groupPosition, int position) {
			return gradesArray.get(position);
		}

		@Override
		public long getChildId(int groupPosition, int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int position) {
			return gradesArray.get(groupPosition).grades[position][0].equals("10.0");
		}

		@Override
		public View getGroupView(int position, boolean isExpanded, View convertView, ViewGroup viewGroup) {
			if (position == getGroupCount() - 1) {
				convertView = ((LayoutInflater) mainContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_footer, viewGroup, false);
				convertView.setEnabled(false);
				convertView.setOnClickListener(null);

				final TextView averageTV = (TextView) convertView.findViewById(R.id.tv_footer_lf);

				if (!average.equals("NaN")) {
					averageTV.setTextColor(mainContext.getResources().getColor(MainActivity.themeSecondaryTextColor));

					averageTV.setText(mainContext.getResources().getString(R.string.average) + ' ' + average);
				}

				return convertView;
			} else {
				final String course = gradesArray.get(position).course;
				final Spanned grades = Html.fromHtml(gradesArray.get(position).getGrades());

				if (compactView)
					convertView = ((LayoutInflater) mainContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_grades_compact, viewGroup, false);
				else
					convertView = ((LayoutInflater) mainContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_grades, viewGroup, false);

				final TextView courseTV = (TextView) convertView.findViewById(R.id.tv_course_lg);
				final TextView gradesTV = (TextView) convertView.findViewById(R.id.tv_grades_lg);
				final TextView averageTV = (TextView) convertView.findViewById(R.id.tv_average_lg);
				final TextView averageRoundTV = (TextView) convertView.findViewById(R.id.tv_average_round_lg);

				if (MainActivity.themeColor == R.color.theme_dark)
					convertView.setBackgroundResource(R.drawable.listselector_dark);
				else
					convertView.setBackgroundResource(R.drawable.listselector_light);

				courseTV.setTextColor(mainContext.getResources().getColor(MainActivity.themePrimaryTextColor));
				gradesTV.setTextColor(mainContext.getResources().getColor(MainActivity.themePrimaryTextColor));
				averageTV.setTextColor(mainContext.getResources().getColor(MainActivity.themePrimaryTextColor));
				averageRoundTV.setTextColor(mainContext.getResources().getColor(MainActivity.themePrimaryTextColor));

				try {
					courseTV.setText(mainContext.getResources().getIdentifier(course, "string", mainContext.getPackageName()));
				} catch (Resources.NotFoundException e) {
					courseTV.setText(course);
				}

				if (grades.toString().equals("")) {
					convertView.setEnabled(false);
					convertView.setOnClickListener(null);
				} else {
					gradesTV.setSelected(true);
					gradesTV.setText(grades);
				}

				if (!Html.fromHtml(gradesArray.get(position).average).toString().equals("0.0")) { //TODO Optimize
					averageTV.setText(Html.fromHtml(gradesArray.get(position).average));
					averageRoundTV.setText(Html.fromHtml(gradesArray.get(position).averageRound));
				}

				return convertView;
			}
		}

		@Override
		public View getChildView(int groupPosition, int position, boolean isLastChild, View convertView, ViewGroup viewGroup) {
			if (compactView)
				convertView = ((LayoutInflater) mainContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_grades_expanded_compact, viewGroup, false);
			else
				convertView = ((LayoutInflater) mainContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.listitem_grades_expanded, viewGroup, false);

			final TextView descTV = (TextView) convertView.findViewById(R.id.tv_desc_lge);
			final TextView gradeTV = (TextView) convertView.findViewById(R.id.tv_grade_lge);

			convertView.setBackgroundColor(mainContext.getResources().getColor(MainActivity.themeDividerColor));

			descTV.setTextColor(mainContext.getResources().getColor(MainActivity.themePrimaryTextColor));
			gradeTV.setTextColor(mainContext.getResources().getColor(MainActivity.themePrimaryTextColor));

			descTV.setSelected(true);
			descTV.setText(Html.fromHtml(gradesArray.get(groupPosition).grades[position][2]));

			gradeTV.setText(Html.fromHtml(gradesArray.get(groupPosition).grades[position][0] + "<sup><small>" + gradesArray.get(groupPosition).grades[position][1].replaceAll("<[^>]*>","")));

			return convertView;
		}
	}
}