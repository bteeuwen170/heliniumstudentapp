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

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Objects;

public class DayActivity extends AppCompatActivity {
	private static final Integer[] schoolMinutes = new Integer[] {
			495, 545, 595, 610, 660, 710, 740, 790, 840, 855, 905, 955,
			545, 595, 610, 660, 710, 740, 790, 840, 855, 905, 955, 1005
	};

	//private static final Integer[] homeworkMinutes = new Integer[] { 495, 545, 610, 660, 740, 790, 855, 905, 955 };

	private static ScheduleFragment.week schedule;

	private static int lastPosition;

	private static int currentTime;

	private static boolean compactView;

	@SuppressWarnings("ConstantConditions")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_day);

		final Toolbar toolbarTB = (Toolbar) findViewById(R.id.tb_toolbar_ad);
		setSupportActionBar(toolbarTB);
		toolbarTB.setBackgroundResource(MainActivity.primaryColor);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbarTB.getNavigationIcon().setColorFilter(
				ContextCompat.getColor(this, MainActivity.primaryTextColor), PorterDuff.Mode.SRC_ATOP);

		getWindow().getDecorView().setBackgroundResource(MainActivity.themeColor);

		MainActivity.setStatusBar(this);

		ViewPager daysVP = (ViewPager) findViewById(R.id.vp_days_ad);

		Bundle bundle = getIntent().getExtras();
		schedule = (ScheduleFragment.week) bundle.getSerializable("schedule");
		lastPosition = (Integer) bundle.get("pos");

		daysVP.setAdapter(new DaysAdapter(this, getSupportFragmentManager()));
		daysVP.setOffscreenPageLimit(1);
		daysVP.setCurrentItem(lastPosition);

		compactView =
				PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_customization_compact", false);

		MainActivity.setToolbarTitle(this, schedule.day_get(lastPosition + 2).day, schedule.day_get(lastPosition + 2).date);

		daysVP.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				/*GregorianCalendar chosenWeekDay = new GregorianCalendar(HeliniumStudentApp.LOCALE);
				chosenWeekDay.set(Calendar.DAY_OF_WEEK, position + 2);

				GregorianCalendar chosenWeekDate = new GregorianCalendar(HeliniumStudentApp.LOCALE);
				try {
					chosenWeekDate.setTime(android.text.format.DateFormat.getDateFormat(getApplicationContext()).parse(getSupportActionBar().getSubtitle().toString()));

					if (position > lastPosition) {
						chosenWeekDate.add(Calendar.DATE, 1);
					} else {
						chosenWeekDate.add(Calendar.DATE, -1);
					}
				} catch (ParseException ignored) {}*/

				MainActivity.setToolbarTitle(DayActivity.this, schedule.day_get(position + 2).day, schedule.day_get(position + 2).date);
				/*MainActivity.setToolbarTitle(DayActivity.this, Character.toUpperCase(weekDay.charAt(0)) + weekDay.substring(1),
						android.text.format.DateFormat.getDateFormat(getApplicationContext()).format(chosenWeekDate.getTime()));*/

				lastPosition = position;
			}

			@Override
			public void onPageScrollStateChanged(int state) {}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
		});
	}

	/*private static HashMap<Integer, HomeworkWrapper> parseHomework(Context dayContext) {
		if (ScheduleFragment.homeworkJson != null) {
			final HashMap<Integer, HomeworkWrapper> homeworkArray = new HashMap<>();

			JSONObject json;
			try {
				json = (JSONObject) new JSONTokener(ScheduleFragment.homeworkJson).nextValue();

				final JSONArray homeworkJson = json.getJSONArray("events");

				for (int event = 0; event < homeworkJson.length(); event++) {
					final JSONObject courseJson = homeworkJson.getJSONObject(event).getJSONObject("afspraakObject");

					final GregorianCalendar startDate = new GregorianCalendar(TimeZone.getTimeZone("Europe/Amsterdam"), HeliniumStudentApp.LOCALE);
					startDate.setTimeInMillis(homeworkJson.getJSONObject(event).getLong("start"));

					final int dayOfWeek = startDate.get(Calendar.DAY_OF_WEEK);
					final int hourOfDay = Arrays.asList(homeworkMinutes).indexOf(startDate.get(Calendar.HOUR_OF_DAY) * 60 + startDate.get(Calendar.MINUTE)) + 1;

					String homework, absence;
					homework = absence = null;
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
									absence = "[fulough]" + dayContext.getString(R.string.fulough);
									break;
								case "te laat-T":
									absence = "[late]" + dayContext.getString(R.string.late);
									break;
								case "Ziek / medisch":
									absence = "[medical]" + dayContext.getString(R.string.medical);
									break;
								case "Ongeoorloofd verzuim":
									absence = "[truancy]" + dayContext.getString(R.string.truancy);
									break;
								default:
									absence = "[late]" + dayContext.getString(R.string.late);
							}
					}

					homeworkArray.put(dayOfWeek * 100 + hourOfDay, new HomeworkWrapper(homework, options.toString(), absence));
				}

				return homeworkArray;
			} catch (JSONException  | NullPointerException e) { //TODO NullPointerException needed?
				//FIXME Handle, neither floating nor normal homework?
				//FIXME No homework this week or error? Probably error

				return null;
			}

		try {
			final JSONArray floatingHomework = json.getJSONArray("huiswerkEvents");
		} catch (JSONException | NullPointerException e) {
			//TODO <strike>No floating homework this week</strike> Probably never called, have a global JSON Exception handler...
		}
		} else {
			return null;
		}
	}*/

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

		private static DayFragment newInstance(AppCompatActivity dayContext, int pos) {
			final DayFragment dayFragment = new DayFragment();
			DayFragment.dayContext = dayContext;

			final Bundle bundle = new Bundle();
			bundle.putInt("pos", pos);
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

			final DayAdapter hoursLVadapter = new DayAdapter(schedule, getArguments().getInt("pos"));
			hoursLV.setAdapter(hoursLVadapter);
			hoursLVadapter.notifyDataSetChanged();

			/*hoursLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				final String[] hours = new String[]{ "8:15 - 9:05", "9:05 - 9:55", "10:10 - 11:00", "11:00 - 11:50", "12:20 - 13:10", "13:10 - 14:00", "14:15 - 15:05", "15:05 - 15:55", "15:55 - 16:45" };

				public void onItemClick(AdapterView<?> parent, View view, final int position, long id)
				{
					final ScheduleFragment.week.day.hour hour_data = schedule.day_get(day + 2).hour_get(pos);
					String course, classroom, teacher;
					ScheduleFragment.week.day.hour.extra absence, homework;
					int hour = hour_data.hour;

					course = hour_data.course;
					classroom = hour_data.classroom;
					teacher = hour_data.teacher;
					absence = hour_data.extra_get(ScheduleFragment.extra_i.ABSENCE.value());
					homework = hour_data.extra_get(ScheduleFragment.extra_i.HOMEWORK.value());

					final AlertDialog.Builder dayDialogBuilder =
							new AlertDialog.Builder(new ContextThemeWrapper(dayContext, MainActivity.themeDialog));

					final View dayLayout = View.inflate(dayContext, R.layout.dialog_day, null);
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

					final int textColor = ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor);

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
						teacherTV.setText(getString(getResources().getIdentifier(teacher, "string", dayContext.getPackageName())));
					} catch (Resources.NotFoundException e) {
						teacherTV.setText(teacher);
					}

					classroomTV.setText(classroom);

					dayLayout.findViewById(R.id.v_hseperator2_dd).setBackgroundResource(MainActivity.themeDividerColor);

					groupTV.setText(group);

					if (homework != null) {
						dayLayout.findViewById(R.id.rl_hseperator3_dd).setVisibility(View.VISIBLE);
						dayLayout.findViewById(R.id.v_hseperator3l_dd).setBackgroundResource(MainActivity.themeDividerColor);
						dayLayout.findViewById(R.id.v_hseperator3r_dd).setBackgroundResource(MainActivity.themeDividerColor);

						homeworkTV.setVisibility(View.VISIBLE);
						homeworkTV.setMovementMethod(new ScrollingMovementMethod());
						homeworkTV.setTextColor(textColor);
						homeworkTV.setText(Html.fromHtml(homework.homework));
					}

					if (absence != null) {
						dayLayout.findViewById(R.id.rl_hseperator4_dd).setVisibility(View.VISIBLE);
						dayLayout.findViewById(R.id.v_hseperator4l_dd).setBackgroundResource(MainActivity.themeDividerColor);
						dayLayout.findViewById(R.id.v_hseperator4r_dd).setBackgroundResource(MainActivity.themeDividerColor);

						absenceTV.setVisibility(View.VISIBLE);
						absenceTV.setMovementMethod(new ScrollingMovementMethod());
						absenceTV.setTextColor(textColor);
						absenceTV.setText(Html.fromHtml(homework.absence.replaceFirst("\\[.*?\\]", "")));
					}

					final AlertDialog dayDialog = dayDialogBuilder.create();

					dayDialog.setCanceledOnTouchOutside(true);
					dayDialog.show();

					dayDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(dayContext, MainActivity.accentSecondaryColor));
				}
			});*/
		}

		@Override
		public void onResume() {
			super.onResume();

			final Date time = new Date();
			currentTime = Integer.parseInt(HeliniumStudentApp.df_hours().format(time)) * 60
					+ Integer.parseInt(HeliniumStudentApp.df_minutes().format(time));

			hoursLV.invalidateViews();
		}

		private class DayAdapter extends BaseAdapter
		{
			private ScheduleFragment.week schedule;
			private int day;
			//private boolean pbreak;

			public DayAdapter(final ScheduleFragment.week schedule, final int day)
			{
				this.schedule = schedule;
				this.day = day;
			}

			public int getCount()
			{
				// TODO + 3
				return schedule.day_get(day + 2).hours_get();
			}

			/* XXX ??? */
			public Object getItem(int pos)
			{
				return 0;
				//return hours.get(pos);
			}

			public long getItemId(int pos)
			{
				return pos;
			}

			public View getView(int pos, View convertView, ViewGroup viewGroup)
			{
				final LayoutInflater inflater =
						(LayoutInflater) dayContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final ScheduleFragment.week.day.hour hour_data = schedule.day_get(day + 2).hour_get(pos);
				String course, classroom, teacher;
				ScheduleFragment.week.day.hour.extra absence, homework;
				boolean current = false;
				int hour = hour_data.hour;

				course = hour_data.course;
				classroom = hour_data.classroom;
				teacher = hour_data.teacher;
				absence = hour_data.extra_get(ScheduleFragment.extra_i.ABSENCE.value());
				homework = hour_data.extra_get(ScheduleFragment.extra_i.HOMEWORK.value());

				/*if (hour > 3) {
					if (hour < 6)
						hour--;
					else if (hour < 9)
						hour -= 2;
					else
						hour -= 3;
				}*/

				//if (hours.get(pos).get("now").equals("true"))
				//	current = true; //TODO Put everything in parser

				/*if (pbreak) {
					pbreak = false;
				} else if (hour == 3 || hour == 6 || hour == 9) {
					if (compactView)
						convertView = inflater.inflate(R.layout.listitem_day_divider_compact, viewGroup, false);
					else
						convertView = inflater.inflate(R.layout.listitem_day_divider, viewGroup, false);

					if (current)
						convertView.findViewById(R.id.v_divider_ldb)
								.setBackgroundResource(MainActivity.darkPrimaryColor);
					else
						convertView.findViewById(R.id.v_divider_ldb)
								.setBackgroundResource(MainActivity.themeDividerColor);

					convertView.setEnabled(false);
					convertView.setOnClickListener(null);

					pbreak = true;
					k--;

					return convertView;
				}*/

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

				((GradientDrawable) hourTV.getBackground()).setColor(ContextCompat.getColor(dayContext,
						MainActivity.primaryColor));
				hourTV.setTextColor(ContextCompat.getColor(dayContext, MainActivity.primaryTextColor));

				hourTV.setText(String.valueOf(hour));

				if (course == null) {
					convertView.setEnabled(false);
					convertView.setOnClickListener(null);

					if (current)
						hourTV.setTypeface(Typeface.DEFAULT_BOLD);
				} else {
					courseTV.setTextColor(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor));
					ctTV.setTextColor(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor));

					if (current) {
						hourTV.setTypeface(Typeface.DEFAULT_BOLD);
						courseTV.setTypeface(Typeface.DEFAULT_BOLD);
						ctTV.setTypeface(Typeface.DEFAULT_BOLD);
					}

					/* FIXME Broken */
					try {
						courseTV.setText(getString(getResources().getIdentifier(course.replaceAll(" ", "_"), "string",
								dayContext.getPackageName())));
					} catch (Resources.NotFoundException e) {
						courseTV.setText(course);
					}

					Spannable ct;
					try {
						ct = new SpannableString(Html.fromHtml(classroom + " &ndash; " + getString(
								getResources().getIdentifier(teacher, "string", dayContext.getPackageName()))));
					} catch (Resources.NotFoundException e) {
						ct = new SpannableString(Html.fromHtml(classroom + " &ndash; " + teacher));
					}

					ct.setSpan(new ForegroundColorSpan(ContextCompat.getColor(dayContext,
							MainActivity.themeSecondaryTextColor)), classroom.length(), ct.length(),
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					ctTV.setText(ct);

					final ImageView homeworkIV = (ImageView) convertView.findViewById(R.id.iv_hw_ld);
					ImageView absenceIV;

					homeworkIV.setColorFilter(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor),
							PorterDuff.Mode.SRC_ATOP);

					if (homework != null) {
						/*
						 * TODO Support for multiple icons at the same time,
						 * TODO confirm is possible as well (e.g. Done test or Late test) ?
						 */
						switch (homework.type) {
						case TEST:
							homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_homework_test));
							break;
						case HOMEWORKDONE:
							homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_homework_done));
							break;
						case HOMEWORKLATE:
							homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_homework_late));
							break;
						case HOMEWORK:
						default:
							homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_homework));
							break;
						}

						absenceIV = (ImageView) convertView.findViewById(R.id.iv_ab_ld);
					} else {
						absenceIV = homeworkIV;
					}

					absenceIV.setColorFilter(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor),
							PorterDuff.Mode.SRC_ATOP);

					if (absence != null) {
						switch (absence.type) {
							/* TODO Other icon */
						case LATE:
							absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_absent));
							break;
						case FULOUGH:
							absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_absent_fulough));
							break;
						case MEDICAL:
							absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_absent_medical));
							break;
							/* TODO Other icon */
						case TRUANCY:
							absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_absent));
							break;
							/* TODO New icon */
						case REMOVED:
							absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_absent));
							break;
						case ABSENT:
						default:
							absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext,
									R.drawable.ic_absent));
							break;
						}
					}
				}

				return convertView;
			}
		}
	}
}