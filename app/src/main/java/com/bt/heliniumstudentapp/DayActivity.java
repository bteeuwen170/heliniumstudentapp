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
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class DayActivity extends AppCompatActivity {
	private static final Integer[] schoolMinutes = new Integer[] {
			495, 545, 595, 610, 660, 710, 740, 790, 840, 855, 905, 955,
			545, 595, 610, 660, 710, 740, 790, 840, 855, 905, 955, 1005
	};

	private static final Integer[] homeworkMinutes = new Integer[] { 495, 545, 610, 660, 740, 790, 855, 905, 955 };

	private int lastPosition;

	private static int currentTime;

	private static boolean compactView;

	private static HashMap<Integer, HomeworkWrapper> homeworkArray; //TODO Not global!

	private static class HomeworkWrapper {
		String homework;
		String options;
		String absence;

		private HomeworkWrapper(final String homework, final String options, final String absence) {
			this.homework = homework;
			this.options = options;
			this.absence = absence;
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_day);

		final Toolbar toolbarTB = (Toolbar) findViewById(R.id.tb_toolbar_ad);
		setSupportActionBar(toolbarTB);
		toolbarTB.setBackgroundResource(MainActivity.primaryColor);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbarTB.getNavigationIcon().setColorFilter(getResources().getColor(MainActivity.primaryTextColor), PorterDuff.Mode.SRC_ATOP);

		getWindow().getDecorView().setBackgroundResource(MainActivity.themeColor);

		MainActivity.setStatusBar(this);

		ViewPager daysVP = (ViewPager) findViewById(R.id.vp_days_ad);

		lastPosition = (Integer) getIntent().getExtras().get("chosen_day");
		daysVP.setAdapter(new DaysAdapter(this, getSupportFragmentManager()));
		daysVP.setOffscreenPageLimit(1);
		daysVP.setCurrentItem(lastPosition);

		GregorianCalendar chosenWeekDay = new GregorianCalendar(Locale.GERMANY);
		chosenWeekDay.set(Calendar.DAY_OF_WEEK, lastPosition + 2);

		compactView = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_customization_compact", false);

		if (ScheduleFragment.homeworkJson != null) { //TODO In function with return statement
			homeworkArray = new HashMap<>();

			JSONObject json = null;
			try {
				json = (JSONObject) new JSONTokener(ScheduleFragment.homeworkJson).nextValue();
			} catch (JSONException e) {
				//TODO Handle, neither floating nor normal homework?
			}

			/*try {
				final JSONArray floatingHomework = json.getJSONArray("huiswerkEvents");
			} catch (JSONException | NullPointerException e) {
				//TODO <strike>No floating homework this week</strike> Probably never called, have a global JSON Exception handler...
			}*/ //TODO Not yet

			try {
				final JSONArray homeworkJson = json.getJSONArray("events");

				for (int event = 0; event < homeworkJson.length(); event++) {
					final JSONObject courseJson = homeworkJson.getJSONObject(event).getJSONObject("afspraakObject");

					final GregorianCalendar startDate = new GregorianCalendar(TimeZone.getTimeZone("Europe/Amsterdam"), Locale.GERMANY);
					startDate.setTimeInMillis(homeworkJson.getJSONObject(event).getLong("start"));

					final int dayOfWeek = startDate.get(Calendar.DAY_OF_WEEK);
					final int hourOfDay = Arrays.asList(homeworkMinutes).indexOf(startDate.get(Calendar.HOUR_OF_DAY) * 60 + startDate.get(Calendar.MINUTE)) + 1;

					//final StringBuilder courseEntry = new StringBuilder();
					String homework = null;
					String absence = null;
					final StringBuilder options = new StringBuilder();

					if (courseJson.has("huiswerk")) {
						final JSONObject courseHomeworkJson = courseJson.getJSONObject("huiswerk");

						homework = courseHomeworkJson.getString("omschrijving");

						if (courseHomeworkJson.has("gemaakt"))
							if (courseHomeworkJson.getBoolean("gemaakt"))
								options.append("<done>");
							else
								options.append("<late>");

						if (courseHomeworkJson.has("toets") && courseHomeworkJson.getBoolean("toets")) options.append("<test>");
					}

					if (courseJson.has("absentie")) {
						final JSONObject courseAbsenceJson = courseJson.getJSONObject("absentie");

						if (courseAbsenceJson.has("reden")) //TODO Does "reden" always have to be provided?
							switch (courseAbsenceJson.getString("reden")) {
								case "Schoolverlof":
									absence = "[fulough]" + getResources().getString(R.string.fulough);
									break;
								case "te laat-T":
									absence = "[late]" + getResources().getString(R.string.late);
									break;
								case "Ziek / medisch":
									absence = "[medical]" + getResources().getString(R.string.medical);
									break;
								case "Ongeoorloofd verzuim":
									absence = "[truancy]" + getResources().getString(R.string.truancy);
									break;
							}
					}

					homeworkArray.put(dayOfWeek * 100 + hourOfDay, new HomeworkWrapper(homework, options.toString(), absence));
				}

				/*for (int event = 0; event < homework.length(); event++) {
					final JSONObject courseJson = homework.getJSONObject(event).getJSONObject("afspraakObject");

					final GregorianCalendar startDate = new GregorianCalendar(TimeZone.getTimeZone("Europe/Amsterdam"), Locale.GERMANY);
					startDate.setTimeInMillis(homework.getJSONObject(event).getLong("start"));

					final int dayOfWeek = startDate.get(Calendar.DAY_OF_WEEK);
					final int hourOfDay = Arrays.asList(homeworkMinutes).indexOf(startDate.get(Calendar.HOUR_OF_DAY) * 60 + startDate.get(Calendar.MINUTE)) + 1;

					final StringBuilder courseEntry = new StringBuilder();

					if (courseJson.has("huiswerk")) {
						final JSONObject courseHomeworkJson = courseJson.getJSONObject("huiswerk");

						courseEntry.append("<hw>");
						courseEntry.append(courseHomeworkJson.getString("omschrijving"));

						if (courseHomeworkJson.has("gemaakt"))
							if (courseHomeworkJson.getBoolean("gemaakt"))
								courseEntry.append("<done>");
							else
								courseEntry.append("<late>");

						if (courseHomeworkJson.has("toets") && courseHomeworkJson.getBoolean("toets")) courseEntry.append("<test>");
					}

					if (courseJson.has("absentie")) {
						final JSONObject courseAbsenceJson = courseJson.getJSONObject("absentie");

						courseEntry.append("[ab]");

						if (courseAbsenceJson.has("reden"))
							switch (courseAbsenceJson.getString("reden")) {
								case "Ongeoorloofd verzuim":
								case "te laat-T":
									courseEntry.append("[late]");
									break;
								case "Ziek / medisch":
									courseEntry.append("[medical]");
									break;
								case "Schoolverlof":
									courseEntry.append("[fulough]");
									break;

							}

						//final String courseAbsenceJson = Html.fromHtml(courseAbsenceJson.getString("reden")).toString(); //TODO Provide reason
					}

					homeworkArray.put(dayOfWeek * 100 + hourOfDay, courseEntry.toString());
				}*/
			} catch (JSONException | NullPointerException e) { //TODO NullPointerException needed?
				//TODO No homework this week or error? Probably error
			}
		}

		final String weekDay = new SimpleDateFormat("EEEE").format(chosenWeekDay.getTime());
		MainActivity.setToolbarTitle(this, Character.toUpperCase(weekDay.charAt(0)) + weekDay.substring(1), (String) getIntent().getExtras().get("chosen_date"));

		daysVP.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				GregorianCalendar chosenWeekDay = new GregorianCalendar(Locale.GERMANY);
				chosenWeekDay.set(Calendar.DAY_OF_WEEK, position + 2);

				GregorianCalendar chosenWeekDate = new GregorianCalendar(Locale.GERMANY);
				try {
					chosenWeekDate.setTime(android.text.format.DateFormat.getDateFormat(getApplicationContext()).parse(getSupportActionBar().getSubtitle().toString()));

					if (position > lastPosition) {
						chosenWeekDate.add(Calendar.DATE, 1);
					} else {
						chosenWeekDate.add(Calendar.DATE, -1);
					}
				} catch (ParseException ignored) {}

				final String weekDay = new SimpleDateFormat("EEEE").format(chosenWeekDay.getTime());
				MainActivity.setToolbarTitle(DayActivity.this, Character.toUpperCase(weekDay.charAt(0)) + weekDay.substring(1),
						android.text.format.DateFormat.getDateFormat(getApplicationContext()).format(chosenWeekDate.getTime()));

				lastPosition = position;
			}

			@Override
			public void onPageScrollStateChanged(int state) {}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
		});
	}

	private static class DaysAdapter extends FragmentPagerAdapter {
		private static AppCompatActivity dayContext;

		public DaysAdapter(AppCompatActivity dayContext, FragmentManager fragmentManager) {
			super(fragmentManager);
			DaysAdapter.dayContext = dayContext;
		}

		@Override
		public int getCount() {
			return 5;
		}

		@Override
		public Fragment getItem(int position) {
			return DayFragment.newInstance(dayContext, position);
		}
	}

	public static class DayFragment extends Fragment {
		private static AppCompatActivity dayContext;
		private ListView hoursLV;

		private static DayFragment newInstance(AppCompatActivity dayContext, int position) {
			final DayFragment dayFragment = new DayFragment();
			DayFragment.dayContext = dayContext;

			final Bundle bundle = new Bundle();
			bundle.putInt("position", position);
			dayFragment.setArguments(bundle); //TODO Smarter way to do this?

			return dayFragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
			final View dayLayout = inflater.inflate(R.layout.fragment_day, viewGroup, false);

			hoursLV = (ListView) dayLayout.findViewById(R.id.lv_hours_fd);

			return dayLayout;
		}

		@Override
		public void onStart() {
			super.onStart();

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				new ParseDay().execute(getArguments().getInt("position"));
			else
				new ParseDay().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getArguments().getInt("position"));
		}

		@Override
		public void onResume() {
			super.onResume();

			final Date time = new Date();
			currentTime = Integer.parseInt(new SimpleDateFormat("HH").format(time)) * 60 + Integer.parseInt(new SimpleDateFormat("mm").format(time));

			hoursLV.invalidateViews();
		}

		private class ParseDay extends AsyncTask<Integer, Void, ArrayList<HashMap<String, Object>>> {

			@Override
			protected ArrayList<HashMap<String, Object>> doInBackground(Integer... position) {
				String localRawHtml = null;

				int day = Calendar.MONDAY;

				boolean available = false;

				switch (position[0]) {
					case 0:
						if (ScheduleFragment.scheduleHtml.contains("</i> maandag")) {
							localRawHtml = ScheduleFragment.scheduleHtml.substring(ScheduleFragment.scheduleHtml.indexOf("</i> maandag"));
							day = Calendar.MONDAY;
							available = true;
						}
						break;
					case 1:
						if (ScheduleFragment.scheduleHtml.contains("</i> dinsdag")) {
							localRawHtml = ScheduleFragment.scheduleHtml.substring(ScheduleFragment.scheduleHtml.indexOf("</i> dinsdag"));
							day = Calendar.TUESDAY;
							available = true;
						}
						break;
					case 2:
						if (ScheduleFragment.scheduleHtml.contains("</i> woensdag")) {
							localRawHtml = ScheduleFragment.scheduleHtml.substring(ScheduleFragment.scheduleHtml.indexOf("</i> woensdag"));
							day = Calendar.WEDNESDAY;
							available = true;
						}
						break;
					case 3:
						if (ScheduleFragment.scheduleHtml.contains("</i> donderdag")) {
							localRawHtml = ScheduleFragment.scheduleHtml.substring(ScheduleFragment.scheduleHtml.indexOf("</i> donderdag"));
							day = Calendar.THURSDAY;
							available = true;
						}
						break;
					case 4:
						if (ScheduleFragment.scheduleHtml.contains("</i> vrijdag")) {
							localRawHtml = ScheduleFragment.scheduleHtml.substring(ScheduleFragment.scheduleHtml.indexOf("</i> vrijdag"));
							day = Calendar.FRIDAY;
							available = true;
						}
						break;
				}

				if (available) {
					final ArrayList<HashMap<String, Object>> hoursLVarray = new ArrayList<>();

					String hoursHtml = localRawHtml.substring(0, localRawHtml.indexOf("</div>")) + "<tr";

					int hour = 0;
					int breakCount = 0;

					while (hour != 9) {
						String hourHtml = hoursHtml.substring(hoursHtml.indexOf("<td class=\"hour\">") + 17);
						hourHtml = hourHtml.substring(0, hourHtml.indexOf("<tr"));
						hoursHtml = hoursHtml.replace("<td class=\"hour\">" + hourHtml, "");

						hour = Integer.parseInt(hourHtml.substring(0, 1));

						HashMap<String, Object> hourItem = new HashMap<>();

						if ((hour == 3 && breakCount == 0) || (hour == 5 && breakCount == 1) || (hour == 7 && breakCount == 2)) {
							hourItem.put("hour", String.valueOf(hour + breakCount));
							hourItem.put("now", String.valueOf(ScheduleFragment.scheduleFocus == new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR) && //TODO Just use chosen_date from putExtra
									position[0] + 2 == new GregorianCalendar(Locale.GERMANY).get(Calendar.DAY_OF_WEEK) &&
									currentTime >= schoolMinutes[hour + breakCount - 1] && currentTime < schoolMinutes[hour + breakCount + 11]));
							hoursLVarray.add(hourItem);

							hourItem = new HashMap<>();

							breakCount ++;
						}

						if (!hourHtml.contains("colspan")) {
							final String course = hourHtml.substring(hourHtml.indexOf("<td class=\"course\">") + 19);
							hourItem.put("course", course.substring(0, course.indexOf("</td>")));

							final String classroom = hourHtml.substring(hourHtml.indexOf("<td class=\"classroom\">") + 22);
							hourItem.put("classroom", classroom.substring(0, classroom.indexOf("</td>")));

							final String teacher = hourHtml.substring(hourHtml.indexOf("<td class=\"teacher\">") + 20);
							hourItem.put("teacher", Html.fromHtml(teacher.substring(0, teacher.indexOf("</td>"))).toString().trim());

							final String group = hourHtml.substring(hourHtml.indexOf("<td class=\"group\">") + 18);
							hourItem.put("group", group.substring(0, group.indexOf("</td>")).trim());

							hourItem.put("homework", homeworkArray.get(day * 100 + hour));
						}

						hourItem.put("hour", String.valueOf(hour + breakCount));
						hourItem.put("now", String.valueOf(ScheduleFragment.scheduleFocus == new GregorianCalendar(Locale.GERMANY).get(Calendar.WEEK_OF_YEAR) && //TODO Just use chosen_date from putExtra
								position[0] + 2 == new GregorianCalendar(Locale.GERMANY).get(Calendar.DAY_OF_WEEK) &&
								currentTime >= schoolMinutes[hour + breakCount - 1] && currentTime < schoolMinutes[hour + breakCount + 11]));
						hoursLVarray.add(hourItem);
					}

					return hoursLVarray;
				} else {
					return null;
				}
			}

			@Override
			protected void onPostExecute(final ArrayList<HashMap<String, Object>> hoursLVarray) {
				if (hoursLVarray != null) {
					final DayAdapter hoursLVadapter = new DayAdapter(hoursLVarray);
					hoursLV.setAdapter(hoursLVadapter);

					hoursLVadapter.notifyDataSetChanged();

					hoursLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
						final String[] hours = new String[]{ "8:15 - 9:05", "9:05 - 9:55", "10:10 - 11:00", "11:00 - 11:50", "12:20 - 13:10", "13:10 - 14:00", "14:15 - 15:05", "15:05 - 15:55", "15:55 - 16:45" };

						public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
							final int hour = Integer.parseInt((String) hoursLVarray.get(position).get("hour"));
							final String course = (String) hoursLVarray.get(position).get("course");
							final String classroom = (String) hoursLVarray.get(position).get("classroom");
							final String group = (String) hoursLVarray.get(position).get("group");
							final String teacher = (String) hoursLVarray.get(position).get("teacher");
							final HomeworkWrapper homework = (HomeworkWrapper) hoursLVarray.get(position).get("homework"); //TODO Make final

							final AlertDialog.Builder dayDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(dayContext, MainActivity.themeDialog));

							final View dayLayout = LayoutInflater.from(dayContext).inflate(R.layout.dialog_day, null);
							dayDialogBuilder.setView(dayLayout);

							if (hour > 3)
								if (hour < 6)
									dayDialogBuilder.setTitle(hours[hour - 2]);
								else if (hour < 9)
									dayDialogBuilder.setTitle(hours[hour - 3]);
								else
									dayDialogBuilder.setTitle(hours[hour - 4]);
							else
								dayDialogBuilder.setTitle(hours[hour - 1]);

							dayDialogBuilder.setPositiveButton(android.R.string.ok, null);

							final int textColor = getResources().getColor(MainActivity.themePrimaryTextColor);

							final TextView courseTV = (TextView) dayLayout.findViewById(R.id.tv_course_dd);
							final TextView teacherTV = (TextView) dayLayout.findViewById(R.id.tv_teacher_dd);
							final TextView classroomTV = (TextView) dayLayout.findViewById(R.id.tv_classroom_dd);
							final TextView groupTV = (TextView) dayLayout.findViewById(R.id.tv_group_dd);
							final TextView homeworkTV = (TextView) dayLayout.findViewById(R.id.tv_homework_dd);
							final TextView absenceTV = (TextView) dayLayout.findViewById(R.id.tv_absence_dd);

							courseTV.setTextColor(textColor);
							teacherTV.setTextColor(textColor);
							classroomTV.setTextColor(textColor);
							groupTV.setTextColor(textColor);

							try {
								courseTV.setText(getResources().getIdentifier(course, "string", dayContext.getPackageName()));
							} catch (Resources.NotFoundException e) {
								courseTV.setText(course);
							}

							try {
								teacherTV.setText(getResources().getString(getResources().getIdentifier(teacher, "string", dayContext.getPackageName())));
							} catch (Resources.NotFoundException e) {
								teacherTV.setText(teacher);
							}

							classroomTV.setText(classroom);

							dayLayout.findViewById(R.id.v_hseperator2_dd).setBackgroundResource(MainActivity.themeDividerColor);

							groupTV.setText(group);

							try { //TODO Handle ASAP
								if (homework != null) { //TODO Handle icons
									if (homework.homework != null) {
										dayLayout.findViewById(R.id.rl_hseperator3_dd).setVisibility(View.VISIBLE);
										dayLayout.findViewById(R.id.v_hseperator3l_dd).setBackgroundResource(MainActivity.themeDividerColor);
										dayLayout.findViewById(R.id.v_hseperator3r_dd).setBackgroundResource(MainActivity.themeDividerColor);

										homeworkTV.setVisibility(View.VISIBLE);
										homeworkTV.setMovementMethod(new ScrollingMovementMethod());
										homeworkTV.setTextColor(textColor);
										homeworkTV.setText(Html.fromHtml(homework.homework));
									}

									if (homework.absence != null) {
										dayLayout.findViewById(R.id.rl_hseperator4_dd).setVisibility(View.VISIBLE);
										dayLayout.findViewById(R.id.v_hseperator4l_dd).setBackgroundResource(MainActivity.themeDividerColor);
										dayLayout.findViewById(R.id.v_hseperator4r_dd).setBackgroundResource(MainActivity.themeDividerColor);

										absenceTV.setVisibility(View.VISIBLE);
										absenceTV.setMovementMethod(new ScrollingMovementMethod());
										absenceTV.setTextColor(textColor);
										absenceTV.setText(Html.fromHtml(homework.absence.replaceFirst("\\[.*?\\]", "")));
									}
								}
							} catch (NullPointerException ignored) {}

							final AlertDialog dayDialog = dayDialogBuilder.create();

							dayDialog.setCanceledOnTouchOutside(true);
							dayDialog.show();

							dayDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(MainActivity.accentSecondaryColor));
						}
					});
				}
			}
		}

		private class DayAdapter extends BaseAdapter {
			private ArrayList<HashMap<String, Object>> hours;

			public DayAdapter(ArrayList<HashMap<String, Object>> objects) {
				this.hours = objects;
			}

			public int getCount() {
				return hours.size();
			}

			public Object getItem(int position) {
				return hours.get(position);
			}

			public long getItemId(int position) {
				return position;
			}

			public View getView(int position, View convertView, ViewGroup viewGroup) {
				int hour = Integer.parseInt((String) hours.get(position).get("hour"));
				final String course = (String) hours.get(position).get("course");
				final String classroom = (String) hours.get(position).get("classroom");
				final String teacher = (String) hours.get(position).get("teacher");
				final HomeworkWrapper homework = (HomeworkWrapper) hours.get(position).get("homework");

				final LayoutInflater inflater = (LayoutInflater) dayContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				boolean current = false;

				if (hours.get(position).get("now").equals("true")) current = true; //TODO Put everything in parser

				if (hour == 3 || hour == 6 || hour == 9) {
					if (compactView)
						convertView = inflater.inflate(R.layout.listitem_day_divider_compact, viewGroup, false);
					else
						convertView = inflater.inflate(R.layout.listitem_day_divider, viewGroup, false);

					if (current) {
						convertView.findViewById(R.id.v_divider_ldb).setBackgroundResource(MainActivity.darkPrimaryColor);
					} else {
						convertView.findViewById(R.id.v_divider_ldb).setBackgroundResource(MainActivity.themeDividerColor);
					}

					convertView.setEnabled(false);
					convertView.setOnClickListener(null);
				} else {
					if (hour > 3)
						if (hour < 6)
							hour --;
						else if (hour < 9)
							hour -= 2;
						else
							hour -= 3;

					if (compactView)
						convertView = inflater.inflate(R.layout.listitem_day_compact, viewGroup, false);
					else
						convertView = inflater.inflate(R.layout.listitem_day, viewGroup, false);

					final TextView hourTV = (TextView) convertView.findViewById(R.id.tv_hour_ld);
					final TextView courseTV = (TextView) convertView.findViewById(R.id.tv_course_ld);
					final TextView ctTV = (TextView) convertView.findViewById(R.id.tv_ct_ld);

					if (MainActivity.themeColor == R.color.theme_dark)
						convertView.setBackgroundResource(R.drawable.listselector_dark);
					else
						convertView.setBackgroundResource(R.drawable.listselector_light);

					((GradientDrawable) hourTV.getBackground()).setColor(getResources().getColor(MainActivity.primaryColor));
					hourTV.setTextColor(getResources().getColor(MainActivity.primaryTextColor));
					courseTV.setTextColor(getResources().getColor(MainActivity.themePrimaryTextColor));
					ctTV.setTextColor(getResources().getColor(MainActivity.themePrimaryTextColor));

					hourTV.setText(String.valueOf(hour));

					if (course == null) {
						convertView.setEnabled(false);
						convertView.setOnClickListener(null);

						if (current) hourTV.setTypeface(Typeface.DEFAULT_BOLD);
					} else {
						if (current) {
							hourTV.setTypeface(Typeface.DEFAULT_BOLD);
							courseTV.setTypeface(Typeface.DEFAULT_BOLD);
							ctTV.setTypeface(Typeface.DEFAULT_BOLD);
						}

						try {
							courseTV.setText(getResources().getIdentifier(course, "string", dayContext.getPackageName()));
						} catch (Resources.NotFoundException e) {
							courseTV.setText(course);
						}

						Spannable ct;
						try {
							ct =  new SpannableString(Html.fromHtml(classroom + " &ndash; " + getResources().getString(getResources().getIdentifier(teacher, "string", dayContext.getPackageName()))));
							ct.setSpan(new ForegroundColorSpan(getResources().getColor(MainActivity.themeSecondaryTextColor)), classroom.length(), ct.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							ctTV.setText(ct);
						} catch (Resources.NotFoundException e) {
							ct =  new SpannableString(Html.fromHtml(classroom + " &ndash; " + teacher));
							ct.setSpan(new ForegroundColorSpan(getResources().getColor(MainActivity.themeSecondaryTextColor)), classroom.length(), ct.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							ctTV.setText(ct);
						}

						try { //TODO Handle ASAP
							if (homework != null) {
								final ImageView homeworkIV = (ImageView) convertView.findViewById(R.id.iv_hw_ld);
								ImageView absenceIV;

								homeworkIV.setColorFilter(getResources().getColor(MainActivity.themePrimaryTextColor), PorterDuff.Mode.SRC_ATOP);

								if (homework.homework != null) {
									if (homework.options.contains("<done>")) //TODO Support for multiple icons at the same time, confirm is possible as well (e.g. Done test or Late test)
										homeworkIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_homework_done));
									else if (homework.options.contains("<late>"))
										homeworkIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_homework_late));
									else if (homework.options.contains("<test>"))
										homeworkIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_homework_test));
									else
										homeworkIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_homework));
									absenceIV = (ImageView) convertView.findViewById(R.id.iv_ab_ld);
								} else {
									absenceIV = homeworkIV;
								}

								absenceIV.setColorFilter(getResources().getColor(MainActivity.themePrimaryTextColor), PorterDuff.Mode.SRC_ATOP);

								if (homework.absence != null)
									if (homework.absence.contains("[late]")) //TODO Any other possibilities?
										absenceIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_absent));
									else if (homework.absence.contains("[medical]"))
										absenceIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_absent_medical));
									else if (homework.absence.contains("[fulough]"))
										absenceIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_absent_fulough));
							}
						} catch (NullPointerException ignored) {}
					}
				}

				return convertView;
			}
		}
	}
}