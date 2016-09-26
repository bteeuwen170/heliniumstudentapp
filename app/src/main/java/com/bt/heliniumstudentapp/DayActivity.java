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
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DayActivity extends AppCompatActivity {
	private static final String[] school_hours = new String[]{
		"8:15 - 9:05",
		"9:05 - 9:55",
		"10:10 - 11:00",
		"11:00 - 11:50",
		"12:20 - 13:10",
		"13:10 - 14:00",
		"14:15 - 15:05",
		"15:05 - 15:55",
		"15:55 - 16:45"
	};

	private static final Integer[] school_minutes = new Integer[] {
		/* Start */
		495,	/* 8:15 */
		545,	/* 9:05 */

		595,	/* 9:55 */

		610,	/* 10:10 */
		660,	/* 11:00 */

		710,	/* 11:50 */

		740,	/* 12:20 */
		790,	/* 13:10 */

		840,	/* 14:00 */

		855,	/* 14:15 */
		905,	/* 15:05 */

		955,	/* 15:55 */


		/* End */
		545,	/* 9:05 */
		595,	/* 9:55 */

		610,	/* 10:10 */

		660,	/* 11:00 */
		710,	/* 11:50 */

		740,	/* 12:20 */

		790,	/* 13:10 */
		840,	/* 14:00 */

		855,	/* 14:15 */

		905,	/* 15:05 */
		955,	/* 15:55 */
		1005	/* 16:45 */
	};

	private static ScheduleFragment.week schedule;

	private static int lastPosition, currentTime;

	private static boolean compactView, hw_floating;

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

		MainActivity.setToolbarTitle(this,
				schedule.day_get(lastPosition + 2).day, schedule.day_get(lastPosition + 2).date);

		daysVP.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
		{

			@Override
			public void onPageSelected(int position) {
				MainActivity.setToolbarTitle(DayActivity.this,
						schedule.day_get(position + 2).day, schedule.day_get(position + 2).date);

				hw_floating = (schedule.day_get(position + 2).floatings_get() != 0);
				invalidateOptionsMenu();

				lastPosition = position;
			}

			@Override
			public void onPageScrollStateChanged(int state) {}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
		});
	}

	private static class DaysAdapter extends FragmentPagerAdapter
	{
		private static AppCompatActivity dayContext;

		public DaysAdapter(AppCompatActivity dayContext, FragmentManager fragmentManager)
		{
			super(fragmentManager);
			DaysAdapter.dayContext = dayContext;
		}

		@Override
		public int getCount()
		{
			return 5;
		}

		@Override
		public Fragment getItem(int position)
		{
			return DayFragment.newInstance(dayContext, position);
		}
	}

	public static class DayFragment extends Fragment
	{
		private static AppCompatActivity dayContext;
		private ListView hoursLV;

		private static DayFragment newInstance(AppCompatActivity dayContext, int pos)
		{
			final DayFragment dayFragment = new DayFragment();
			DayFragment.dayContext = dayContext;

			final Bundle bundle = new Bundle();
			bundle.putInt("pos", pos);
			dayFragment.setArguments(bundle); //TODO Smarter way to do this?

			return dayFragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState)
		{
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

			hoursLV.setOnItemClickListener(new AdapterView.OnItemClickListener()
			{

				public void onItemClick(AdapterView<?> parent, View view, final int pos, long id)
				{
					final AlertDialog.Builder dayDialogBuilder =
							new AlertDialog.Builder(new ContextThemeWrapper(dayContext, MainActivity.themeDialog));
					final View dayLayout = View.inflate(dayContext, R.layout.dialog_day, null);

					final ScheduleFragment.week.day.hour hour_data =
							schedule.day_get(getArguments().getInt("pos") + 2).hour_get(pos);
					int hour;
					String course, classroom, teacher, group;
					ScheduleFragment.week.day.hour.extra absence, homework;

					hour = hour_data.hour;
					course = hour_data.course;
					classroom = hour_data.classroom;
					teacher = hour_data.teacher;
					group = hour_data.group;
					absence = hour_data.extra_get(ScheduleFragment.extra_i.ABSENCE.value());
					homework = hour_data.extra_get(ScheduleFragment.extra_i.HOMEWORK.value());

					dayDialogBuilder.setView(dayLayout);

					if (hour > 3) {
						if (hour < 6)
							hour--;
						else if (hour < 9)
							hour -= 2;
						else
							hour -= 3;
					}

					dayDialogBuilder.setTitle(school_hours[hour - 1]);

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
						courseTV.setText(getResources().getIdentifier(course.replaceAll(" ", "_"), "string",
								dayContext.getPackageName()));
					} catch (Resources.NotFoundException e) {
						courseTV.setText(course);
					}

					try {
						teacherTV.setText(getString(getResources().getIdentifier(teacher, "string",
								dayContext.getPackageName())));
					} catch (Resources.NotFoundException e) {
						teacherTV.setText(teacher);
					}

					classroomTV.setText(classroom);

					dayLayout.findViewById(R.id.v_hseperator2_dd).setBackgroundResource(MainActivity.themeDividerColor);

					groupTV.setText(group);

					/* TODO Handle icons */
					if (homework != null) {
						dayLayout.findViewById(R.id.rl_hseperator3_dd).setVisibility(View.VISIBLE);
						dayLayout.findViewById(R.id.v_hseperator3l_dd)
								.setBackgroundResource(MainActivity.themeDividerColor);
						dayLayout.findViewById(R.id.v_hseperator3r_dd)
								.setBackgroundResource(MainActivity.themeDividerColor);

						homeworkTV.setVisibility(View.VISIBLE);
						homeworkTV.setMovementMethod(new ScrollingMovementMethod());
						homeworkTV.setTextColor(textColor);
						homeworkTV.setText(Html.fromHtml(homework.text));
					}

					if (absence != null) {
						dayLayout.findViewById(R.id.rl_hseperator4_dd).setVisibility(View.VISIBLE);
						dayLayout.findViewById(R.id.v_hseperator4l_dd)
								.setBackgroundResource(MainActivity.themeDividerColor);
						dayLayout.findViewById(R.id.v_hseperator4r_dd)
								.setBackgroundResource(MainActivity.themeDividerColor);

						absenceTV.setVisibility(View.VISIBLE);
						absenceTV.setMovementMethod(new ScrollingMovementMethod());
						absenceTV.setTextColor(textColor);
						absenceTV.setText(absence.text);
					}

					final AlertDialog dayDialog = dayDialogBuilder.create();

					dayDialog.setCanceledOnTouchOutside(true);
					dayDialog.show();

					dayDialog.getButton(AlertDialog.BUTTON_POSITIVE)
							.setTextColor(ContextCompat.getColor(dayContext, MainActivity.accentSecondaryColor));
				}
			});
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

			public DayAdapter(final ScheduleFragment.week schedule, final int day)
			{
				this.schedule = schedule;
				this.day = day;
			}

			public int getCount()
			{
				return schedule.day_get(day + 2).hours_get();
			}

			public Object getItem(int pos)
			{
				return null;
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
				boolean current;
				int hour = hour_data.hour;

				/* FIXME Inefficient */
				current = ScheduleFragment.scheduleFocus ==
						new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR) &&
						day + 2 == new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.DAY_OF_WEEK) &&
						currentTime >= school_minutes[hour - 1] &&
						currentTime < school_minutes[hour + 11];

				if (hour == 3 || hour == 6 || hour == 9) {
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

					return convertView;
				}

				if (hour > 3) {
					if (hour < 6)
						hour--;
					else if (hour < 9)
						hour -= 2;
					else
						hour -= 3;
				}

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

				if (hour_data.course == null) {
					convertView.setEnabled(false);
					convertView.setOnClickListener(null);

					if (current)
						hourTV.setTypeface(Typeface.DEFAULT_BOLD);

					return convertView;
				}

				courseTV.setTextColor(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor));
				ctTV.setTextColor(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor));

				if (current) {
					hourTV.setTypeface(Typeface.DEFAULT_BOLD);
					courseTV.setTypeface(Typeface.DEFAULT_BOLD);
					ctTV.setTypeface(Typeface.DEFAULT_BOLD);
				}

				try {
					courseTV.setText(
							getString(getResources().getIdentifier(hour_data.course.replaceAll(" ", "_"), "string",
							dayContext.getPackageName())));
				} catch (Resources.NotFoundException e) {
					courseTV.setText(hour_data.course);
				}

				Spannable ct;
				try {
					ct = new SpannableString(Html.fromHtml(hour_data.classroom + " &ndash; " + getString(
							getResources().getIdentifier(hour_data.teacher, "string", dayContext.getPackageName()))));
				} catch (Resources.NotFoundException e) {
					ct = new SpannableString(Html.fromHtml(hour_data.classroom + " &ndash; " + hour_data.teacher));
				}

				ct.setSpan(new ForegroundColorSpan(ContextCompat.getColor(dayContext,
						MainActivity.themeSecondaryTextColor)), hour_data.classroom.length(), ct.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				ctTV.setText(ct);

				final ImageView homeworkIV = (ImageView) convertView.findViewById(R.id.iv_hw_ld);
				ImageView absenceIV;

				homeworkIV.setColorFilter(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor),
						PorterDuff.Mode.SRC_ATOP);

				if (hour_data.extra_get(ScheduleFragment.extra_i.HOMEWORK.value()) == null) {
					absenceIV = homeworkIV;
				} else {
					/*
					 * TODO Support for multiple icons at the same time,
					 * TODO confirm is possible as well (e.g. Done test or Late test) ?
					 */
					switch (hour_data.extra_get(ScheduleFragment.extra_i.HOMEWORK.value()).type) {
					case TEST:
						homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_homework_test));
						break;
					case HOMEWORKDONE:
						homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_homework_done));
						break;
					case HOMEWORKLATE:
						homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_homework_late));
						break;
					case HOMEWORK:
					default:
						homeworkIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_homework));
						break;
					}

					absenceIV = (ImageView) convertView.findViewById(R.id.iv_ab_ld);
				}

				absenceIV.setColorFilter(ContextCompat.getColor(dayContext, MainActivity.themePrimaryTextColor),
						PorterDuff.Mode.SRC_ATOP);

				if (hour_data.extra_get(ScheduleFragment.extra_i.ABSENCE.value()) != null) {
					switch (hour_data.extra_get(ScheduleFragment.extra_i.ABSENCE.value()).type) {
					case LATE:
						absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_absent));
						break;
					case FULOUGH:
						absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_absent_fulough));
						break;
					case MEDICAL:
						absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_absent_medical));
						break;
					case TRUANCY:
						absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_absent_truancy));
						break;
					case REMOVED:
						absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_absent_removed));
						break;
					case ABSENT:
					default:
						absenceIV.setImageDrawable(ContextCompat.getDrawable(dayContext, R.drawable.ic_absent));
						break;
					}
				}

				return convertView;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (hw_floating) {
			getMenuInflater().inflate(R.menu.menu_day, menu);

			return true;
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() != R.id.i_hwfloating_md)
			return super.onOptionsItemSelected(item);

		final ScheduleFragment.week.day day_data = schedule.day_get(lastPosition + 2);

		if (day_data.floatings_get() == 0) {
			Toast.makeText(this, getString(R.string.homework_floating_no), Toast.LENGTH_SHORT).show();
			return true;
		}

		final AlertDialog.Builder hwf_dialog_builder =
				new AlertDialog.Builder(new ContextThemeWrapper(this, MainActivity.themeDialog));
		AlertDialog hwf_dialog;
		StringBuilder s;
		int i;

		hwf_dialog_builder.setTitle(getString(R.string.homework_floating));

		hwf_dialog_builder.setPositiveButton(android.R.string.ok, null);

		s = new StringBuilder();
		for (i = 0; i < day_data.floatings_get(); i++) {
			s.append(day_data.floating_get(i).course);
			s.append("\n");
			s.append(Html.fromHtml(day_data.floating_get(i).text));
		}

		hwf_dialog_builder.setMessage(s.toString());

		hwf_dialog = hwf_dialog_builder.create();

		hwf_dialog.setCanceledOnTouchOutside(true);
		hwf_dialog.show();

		hwf_dialog.getButton(AlertDialog.BUTTON_POSITIVE)
				.setTextColor(ContextCompat.getColor(this, MainActivity.accentSecondaryColor));

		return true;
	}
}