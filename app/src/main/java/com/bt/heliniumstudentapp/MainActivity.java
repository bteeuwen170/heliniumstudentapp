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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	protected static AppCompatActivity mainContext; //TODO Make private again

	protected static boolean displayingSnackbar;

	protected static Toolbar toolbarTB;
	private static ActionBarDrawerToggle drawerDLtoggle;
	private static DrawerLayout drawerDL;
	protected static NavigationView drawerNV;
	protected static View containerFL, containerLL, statusLL;
	protected static TextView weekTV, yearTV;
	protected static ImageView prevIV, historyIV, nextIV;

	protected static FragmentManager FM;

	protected static CookieManager cookies;

	protected static int
			themeColor, themeDialog, themeSettings, themeDisabledTextColor, themeDividerColor, themePrimaryTextColor, themeSecondaryTextColor,
			primaryColor, darkPrimaryColor,
			secondaryColor,
			primaryTextColor, secondaryTextColor,
			accentPrimaryColor, accentSecondaryColor,
			accentTextColor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mainContext = this;

		HeliniumStudentApp.setLocale(this);

		if (!isOnline() && PreferenceManager.getDefaultSharedPreferences(this).getString("schedule_0", null) == null) { //TODO Keep app running and display empty ScheduleFragment with retry option
			Toast.makeText(this, getResources().getString(R.string.error_conn_no) + ". " + getResources().getString(R.string.database_no) + ".", Toast.LENGTH_LONG).show();
			finish();
		} else if ((PreferenceManager.getDefaultSharedPreferences(this).getString("username", null) == null || PreferenceManager.getDefaultSharedPreferences(this).getString("password", null) == null) &&
				PreferenceManager.getDefaultSharedPreferences(this).getString("schedule_0", null) == null) {
			startActivity(new Intent(this, LoginActivity.class));
			finish();
		} else {
			setContentView(R.layout.activity_main);

			toolbarTB = (Toolbar) findViewById(R.id.tb_toolbar_am);
			drawerDL = (DrawerLayout) findViewById(R.id.dl_drawer_am);
			drawerNV = (NavigationView) findViewById(R.id.nv_drawer_am);
			containerFL = findViewById(R.id.fl_container_am);
			containerLL = findViewById(R.id.ll_container_am);
			statusLL = findViewById(R.id.ll_status_am);
			weekTV = (TextView) findViewById(R.id.tv_week_am);
			yearTV = (TextView) findViewById(R.id.tv_year_am);
			prevIV = (ImageView) findViewById(R.id.iv_prev_am);
			historyIV = (ImageView) findViewById(R.id.iv_select_am);
			nextIV = (ImageView) findViewById(R.id.iv_next_am);

			setColors(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_customization_theme", "0")),
					Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_customization_color_primary", "4")),
					Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_customization_color_accent", "14")));

			setSupportActionBar(toolbarTB);
			toolbarTB.setBackgroundColor(ContextCompat.getColor(this, primaryColor));

			drawerDLtoggle = new ActionBarDrawerToggle(this, drawerDL, toolbarTB, 0, 0);
			drawerDLtoggle.setDrawerIndicatorEnabled(false);
			Drawable navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu);
			navigationIcon.setColorFilter(ContextCompat.getColor(this, primaryTextColor), PorterDuff.Mode.SRC_ATOP);
			drawerDLtoggle.setHomeAsUpIndicator(navigationIcon);
			drawerDLtoggle.setToolbarNavigationClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					drawerDL.openDrawer(drawerNV);
				}
			});
			drawerDLtoggle.syncState();

			((ProgressBar) findViewById(R.id.pb_progressbar_am)).getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, accentPrimaryColor), android.graphics.PorterDuff.Mode.MULTIPLY);

			weekTV.setTextColor(ContextCompat.getColor(this, primaryTextColor));
			yearTV.setTextColor(ContextCompat.getColor(this, secondaryTextColor));
			prevIV.setColorFilter(ContextCompat.getColor(this, primaryTextColor));
			historyIV.setColorFilter(ContextCompat.getColor(this, accentTextColor));
			nextIV.setColorFilter(ContextCompat.getColor(this, primaryTextColor));

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (themeColor == R.color.theme_dark)
					getWindow().setStatusBarColor(ContextCompat.getColor(this, themeColor));
				else
					getWindow().setStatusBarColor(ContextCompat.getColor(this, themeDisabledTextColor));

				setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher), ContextCompat.getColor(this, themeColor)));

				((GradientDrawable) ((RippleDrawable) prevIV.getBackground()).getDrawable(0)).setColor(ContextCompat.getColor(this, primaryColor)); //FIXME Improve this ridiculous workaround
				((GradientDrawable) ((RippleDrawable) historyIV.getBackground()).getDrawable(0)).setColor(ContextCompat.getColor(this, accentPrimaryColor));
				((GradientDrawable) ((RippleDrawable) nextIV.getBackground()).getDrawable(0)).setColor(ContextCompat.getColor(this, primaryColor));
			} else {
				((GradientDrawable) prevIV.getBackground()).setColor(ContextCompat.getColor(this, primaryColor));
				((GradientDrawable) historyIV.getBackground()).setColor(ContextCompat.getColor(this, accentPrimaryColor));
				((GradientDrawable) nextIV.getBackground()).setColor(ContextCompat.getColor(this, primaryColor));
			}

			final ColorStateList drawerItemColorStateList = new ColorStateList(
					new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
					new int[] { ContextCompat.getColor(this, accentSecondaryColor), ContextCompat.getColor(this, themeSecondaryTextColor) }
			);

			FM = getSupportFragmentManager();
			FM.beginTransaction().replace(R.id.fl_container_am, new ScheduleFragment(), "SCHEDULE").commit();

			drawerDL.setBackgroundResource(themeColor);
			drawerNV.setBackgroundResource(themeColor);
			containerLL.setBackgroundResource(primaryColor);

			drawerNV.setItemIconTintList(drawerItemColorStateList);
			drawerNV.setItemTextColor(drawerItemColorStateList);
			drawerNV.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

				@Override
				public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
					drawerDL.closeDrawers();

					new Handler().postDelayed(new Runnable() {

						@Override
						public void run() {
							switch (menuItem.getItemId()) {
								case R.id.i_schedule_md:
									menuItem.setChecked(true);

									if (!FM.findFragmentById(R.id.fl_container_am).getTag().equals("SCHEDULE")) {
										weekTV.setText("");
										yearTV.setText("");

										prevIV.setOnClickListener(null);
										historyIV.setOnClickListener(null);
										nextIV.setOnClickListener(null);

										FM.beginTransaction().replace(R.id.fl_container_am, new ScheduleFragment(), "SCHEDULE").commit();
									}
									break;
								case R.id.i_grades_md:
									menuItem.setChecked(true);

									if (!FM.findFragmentById(R.id.fl_container_am).getTag().equals("GRADES")) {
										weekTV.setText("");
										yearTV.setText("");

										prevIV.setOnClickListener(null);
										historyIV.setOnClickListener(null);
										nextIV.setOnClickListener(null);

										FM.beginTransaction().replace(R.id.fl_container_am, new GradesFragment(), "GRADES").commit();
									}
									break;
								case R.id.i_settings_md:
									startActivity(new Intent(mainContext, SettingsActivity.class));
									break;
								case R.id.i_logout_md:
									if (isOnline()) {
										final AlertDialog.Builder logoutDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(mainContext, themeDialog));

										logoutDialogBuilder.setTitle(R.string.logout);
										logoutDialogBuilder.setMessage(R.string.logout_confirm);

										logoutDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

											@Override
											public void onClick(DialogInterface dialog, int which) {
												ScheduleFragment.scheduleJson = null;
												GradesFragment.gradesHtml = null;

												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("username", null).apply();
												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("password", null).apply();
												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_general_class", "0").apply();

												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("schedule_0", null).apply();
												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("schedule_start_0", null).apply();
												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_schedule_version_0", null).apply();

												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("schedule_1", null).apply();
												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("schedule_start_1", null).apply();
												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_schedule_version_1", null).apply();

												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("html_grades", null).apply();
												PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("pref_grades_version", null).apply();

												new Handler().postDelayed(new Runnable() {

													@Override
													public void run() {
														finish();
														startActivity(new Intent(mainContext, MainActivity.class));
													}
												}, HeliniumStudentApp.DELAY_RESTART);
											}
										});

										logoutDialogBuilder.setNegativeButton(android.R.string.no, null);

										final AlertDialog logoutDialog = logoutDialogBuilder.create();

										logoutDialog.setCanceledOnTouchOutside(true);
										logoutDialog.show();

										logoutDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(mainContext, accentSecondaryColor));
										logoutDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(mainContext, accentSecondaryColor));
									} else {
										Toast.makeText(mainContext, R.string.error_conn_no, Toast.LENGTH_SHORT).show();
									}
									break;
								case R.id.i_about_md:
									final AlertDialog.Builder aboutDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(mainContext, themeDialog));

									aboutDialogBuilder.setTitle(R.string.about);
									aboutDialogBuilder.setMessage(getResources().getString(R.string.app_name) +
											"\n\nCopyright (C) 2016 Bastiaan Teeuwen <bastiaan@mkcl.nl>\n\n" +
											"This program is free software; you can redistribute it and/or " +
											"modify it under the terms of the GNU General Public License " +
											"as published by the Free Software Foundation; version 2 " +
											"of the License, or (at your option) any later version.\n\n" +
											"This program is distributed in the hope that it will be useful, " +
											"but WITHOUT ANY WARRANTY; without even the implied warranty of " +
											"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the " +
											"GNU General Public License for more details.\n\n" +
											"You should have received a copy of the GNU General Public License " +
											"along with this program; if not, write to the Free Software " +
											"Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.");

									aboutDialogBuilder.setNeutralButton(R.string.website,
											new DialogInterface.OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog, int which) {
											try {
												startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HeliniumStudentApp.URL_WEBSITE)));
											} catch (ActivityNotFoundException e) {
												Toast.makeText(mainContext, "Couldn't find a browser", Toast.LENGTH_SHORT).show();
											}
										}
									});

									aboutDialogBuilder.setPositiveButton(R.string.github,
											new DialogInterface.OnClickListener() {

										@Override
										public void onClick(DialogInterface dialog, int which) {
											try {
												startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(HeliniumStudentApp.URL_GITHUB)));
											} catch (ActivityNotFoundException e) {
												Toast.makeText(mainContext, "Couldn't find a browser", Toast.LENGTH_SHORT).show();
											}
										}
									});

									aboutDialogBuilder.setNegativeButton(android.R.string.cancel, null);

									final AlertDialog aboutDialog = aboutDialogBuilder.create();

									aboutDialog.setCanceledOnTouchOutside(true);
									aboutDialog.show();

									((TextView) aboutDialog.findViewById(android.R.id.message)).setTextSize(12);

									aboutDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(mainContext, accentSecondaryColor));
									aboutDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(mainContext, accentSecondaryColor));
									aboutDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(mainContext, accentSecondaryColor));

									break;
							}
						}
					}, HeliniumStudentApp.DELAY_DRAWER);
					return true;
				}
			});
		}
	}

	protected static boolean isOnline() {
		return ((ConnectivityManager) mainContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null
				&& ((ConnectivityManager) mainContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnectedOrConnecting();
	}

	protected static void recoverError(final int view, final int error, final int direction, final int transition) {
		String postfixError = "";
		switch (view) {
			case HeliniumStudentApp.VIEW_LOGIN:
				postfixError = mainContext.getResources().getString(R.string.while_login);
				break;
			case HeliniumStudentApp.VIEW_SCHEDULE:
				postfixError = mainContext.getResources().getString(R.string.while_schedule);
				break;
			case HeliniumStudentApp.VIEW_GRADES:
				postfixError = mainContext.getResources().getString(R.string.while_grades);
				break;
		}

		switch (error) {
			case HeliniumStudentApp.ERR_IO:
				Toast.makeText(mainContext, mainContext.getResources().getString(R.string.error_conn) + " " + postfixError, Toast.LENGTH_SHORT).show();
				break;
			case HeliniumStudentApp.ERR_OK:
				Toast.makeText(mainContext, mainContext.getResources().getString(R.string.error_ok) + " " + postfixError, Toast.LENGTH_SHORT).show();
				break;
			case HeliniumStudentApp.ERR_UNDEFINED:
				Toast.makeText(mainContext, mainContext.getResources().getString(R.string.error) + " " + postfixError, Toast.LENGTH_SHORT).show();
				break;
			case HeliniumStudentApp.ERR_USERPASS:
				Toast.makeText(mainContext, mainContext.getResources().getString(R.string.error_userpass), Toast.LENGTH_SHORT).show();
				break;
		}

		switch (view) {
			case HeliniumStudentApp.VIEW_LOGIN:
				LoginActivity.authenticationProgressDialog.cancel();
				if (error == HeliniumStudentApp.ERR_USERPASS) PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putString("password", null).apply();
				break;
			case HeliniumStudentApp.VIEW_SCHEDULE:
				final int currentWeek = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR);

				switch (transition) {
					case HeliniumStudentApp.ACTION_INIT_OUT:
					case HeliniumStudentApp.ACTION_SHORT_OUT:
						if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_0", null) == null) { //TODO Does this ever happen or is this handled by checkDatabase?
							Toast.makeText(mainContext, mainContext.getResources().getString(R.string.database_no), Toast.LENGTH_SHORT).show();

							mainContext.finish(); //FIXME Properly close, otherwise app will become really glitchy...
						} else {
							setStatusBar(mainContext);

							ScheduleFragment.scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_0", null);

							ScheduleFragment.parseData(transition);
						}
						break;
				}

				switch (direction) {
					case HeliniumStudentApp.DIREC_BACK:
						if (ScheduleFragment.scheduleFocus > currentWeek && PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_1", null) != null) {
							ScheduleFragment.scheduleFocus = currentWeek + 1;
							ScheduleFragment.scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_1", null);
						} else {
							ScheduleFragment.scheduleFocus = currentWeek;
							ScheduleFragment.scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_0", null);
						}

						ScheduleFragment.parseData(transition);
						break;
					case HeliniumStudentApp.DIREC_CURRENT:
						setUI(view, transition);
						break;
					case HeliniumStudentApp.DIREC_NEXT:
						if (ScheduleFragment.scheduleFocus > currentWeek && PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_1", null) != null) {
							ScheduleFragment.scheduleFocus = currentWeek + 1;
							ScheduleFragment.scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_1", null);
						} else {
							ScheduleFragment.scheduleFocus = currentWeek;
							ScheduleFragment.scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_0", null);
						}

						ScheduleFragment.parseData(transition);
						break;
					case HeliniumStudentApp.DIREC_OTHER:
						ScheduleFragment.scheduleFocus = new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR);
						ScheduleFragment.scheduleJson = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_0", null);

						ScheduleFragment.parseData(transition);
						break;
				}
				break;
			case HeliniumStudentApp.VIEW_GRADES:
				if (direction >= HeliniumStudentApp.FOCUS_YEAR) {
					GradesFragment.yearFocus = direction - HeliniumStudentApp.FOCUS_YEAR;
				} else {
					switch (direction) {
						case HeliniumStudentApp.DIREC_BACK:
							GradesFragment.termFocus ++;
							break;
						case HeliniumStudentApp.DIREC_NEXT:
							GradesFragment.termFocus --;
							break;
					}
				}

				switch (transition) {
					case HeliniumStudentApp.ACTION_INIT_OUT:
						if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null) == null) { //TODO Keep and display empty GradesFragment with retry option
							Toast.makeText(mainContext, mainContext.getResources().getString(R.string.database_no), Toast.LENGTH_SHORT).show();

							drawerNV.getMenu().findItem(R.id.i_schedule_md).setChecked(true);
							FM.beginTransaction().replace(R.id.fl_container_am, new ScheduleFragment(), "SCHEDULE").commit();

							setUI(view, transition);
						} else {
							GradesFragment.gradesHtml = PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null); //TODO Move elsewhere like in ScheduleFragment

							GradesFragment.parseData(transition);
						}
						break;
					case HeliniumStudentApp.ACTION_REFRESH_OUT:
						setUI(view, transition);
						break;
				}
				break;
		}
	}

	@SuppressWarnings("deprecation")
	protected static void setUI(final int view, final int action) {
		if (action == HeliniumStudentApp.ACTION_SHORT_IN || (action == HeliniumStudentApp.ACTION_INIT_IN && view == HeliniumStudentApp.VIEW_GRADES)) {
			containerFL.setVisibility(View.GONE);
			drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
			drawerDLtoggle.setToolbarNavigationClickListener(null);
			drawerDLtoggle.syncState();

			weekTV.setText("");
			yearTV.setText("");

			prevIV.setAlpha(130);
			historyIV.setAlpha(130);
			nextIV.setAlpha(130);
			containerFL.setAlpha(0);
			statusLL.setAlpha(1);
		} else if (action == HeliniumStudentApp.ACTION_SHORT_OUT || (action == HeliniumStudentApp.ACTION_INIT_OUT && view == HeliniumStudentApp.VIEW_GRADES)) {
			containerFL.setVisibility(View.VISIBLE);
			drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			drawerDLtoggle.setToolbarNavigationClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					drawerDL.openDrawer(drawerNV);
				}
			});
			drawerDLtoggle.syncState();

			historyIV.setAlpha(255);

			switch (GradesFragment.termFocus) {
				case 1:
					prevIV.setAlpha(130);
					nextIV.setAlpha(255);
					break;
				case 4:
					prevIV.setAlpha(255);
					nextIV.setAlpha(130);
					break;
				default:
					prevIV.setAlpha(255);
					nextIV.setAlpha(255);
					break;
			}

			final int shortAnimationDuration = mainContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

			statusLL.animate().alpha(0).setDuration(shortAnimationDuration).setListener(null);
			containerFL.animate().alpha(1).setDuration(shortAnimationDuration).setListener(null);
		} else if (action == HeliniumStudentApp.ACTION_INIT_IN) {
			toolbarTB.setVisibility(View.GONE);
			containerFL.setVisibility(View.GONE);
			containerLL.setVisibility(View.GONE);
			drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

			toolbarTB.setAlpha(0);
			containerFL.setAlpha(0);
			containerLL.setAlpha(0);
			statusLL.setAlpha(1);
		} else if (action == HeliniumStudentApp.ACTION_INIT_OUT) {
			toolbarTB.setVisibility(View.VISIBLE);
			containerFL.setVisibility(View.VISIBLE);
			containerLL.setVisibility(View.VISIBLE);
			drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

			setStatusBar(mainContext);

			final int shortAnimationDuration = mainContext.getResources().getInteger(android.R.integer.config_shortAnimTime);
			final int longAnimationDuration = mainContext.getResources().getInteger(android.R.integer.config_longAnimTime);

			toolbarTB.animate().alpha(1).setDuration(longAnimationDuration).setListener(null);
			containerLL.animate().alpha(1).setDuration(longAnimationDuration).setListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);

					statusLL.animate().alpha(0).setDuration(shortAnimationDuration).setListener(null);
					containerFL.animate().alpha(1).setDuration(shortAnimationDuration).setListener(null);
				}
			});
		} else {
			switch (view) {
				case HeliniumStudentApp.VIEW_SCHEDULE:
					switch (action) {
						case HeliniumStudentApp.ACTION_ONLINE:
						case HeliniumStudentApp.ACTION_ONLINE_1:
							prevIV.setAlpha(255);
							historyIV.setAlpha(255);
							nextIV.setAlpha(255);
							break;
						case HeliniumStudentApp.ACTION_OFFLINE:
						case HeliniumStudentApp.ACTION_OFFLINE_1:
							if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("schedule_1", null) == null) {
								prevIV.setAlpha(130);
								historyIV.setAlpha(130);
								nextIV.setAlpha(130);
							} else {
								if (ScheduleFragment.scheduleFocus == new GregorianCalendar(HeliniumStudentApp.LOCALE).get(Calendar.WEEK_OF_YEAR) + 1) {
									prevIV.setAlpha(255);
									historyIV.setAlpha(255);
									nextIV.setAlpha(130);
								} else {
									prevIV.setAlpha(130);
									historyIV.setAlpha(130);
									nextIV.setAlpha(255);
								}
							}
							break;
						case HeliniumStudentApp.ACTION_REFRESH_IN:
							drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
							drawerDLtoggle.setToolbarNavigationClickListener(null);
							drawerDLtoggle.syncState();

							prevIV.setAlpha(130);
							historyIV.setAlpha(130);
							nextIV.setAlpha(130);

							((SwipeRefreshLayout) ScheduleFragment.scheduleLayout).setRefreshing(true);
							break;
						case HeliniumStudentApp.ACTION_REFRESH_OUT:
							drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
							drawerDLtoggle.setToolbarNavigationClickListener(new View.OnClickListener() {

								@Override
								public void onClick(View v) {
									drawerDL.openDrawer(drawerNV);
								}
							});
							drawerDLtoggle.syncState();

							prevIV.setAlpha(255);
							historyIV.setAlpha(255);
							nextIV.setAlpha(255);

							((SwipeRefreshLayout) ScheduleFragment.scheduleLayout).setRefreshing(false);
							break;
						/*case HeliniumStudentApp.ERR_UNDEFINED:
						case HeliniumStudentApp.ERR_OK:
							//FIXME HANDLE!!!
							break;*/
					}
					break;
				case HeliniumStudentApp.VIEW_GRADES:
					switch (action) {
						case HeliniumStudentApp.ACTION_ONLINE:
						case HeliniumStudentApp.ACTION_ONLINE_1:
							historyIV.setAlpha(255);

							switch (GradesFragment.termFocus) {
								case 1:
									prevIV.setAlpha(130);
									nextIV.setAlpha(255);
									break;
								case 4:
									prevIV.setAlpha(255);
									nextIV.setAlpha(130);
									break;
								default:
									prevIV.setAlpha(255);
									nextIV.setAlpha(255);
									break;
							}
							break;
						case HeliniumStudentApp.ACTION_OFFLINE:
						case HeliniumStudentApp.ACTION_OFFLINE_1:
							final int databaseFocus = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("pref_grades_term", "1"));

							prevIV.setAlpha(130);
							historyIV.setAlpha(130);
							nextIV.setAlpha(130);

							if (PreferenceManager.getDefaultSharedPreferences(mainContext).getString("html_grades", null) != null) {
								if (GradesFragment.yearFocus == 0 && GradesFragment.termFocus > databaseFocus) prevIV.setAlpha(255);
								if (GradesFragment.yearFocus != 0 || GradesFragment.termFocus != databaseFocus) historyIV.setAlpha(255);
								if (GradesFragment.yearFocus == 0 && GradesFragment.termFocus < databaseFocus) nextIV.setAlpha(255);
							}
							break;
						case HeliniumStudentApp.ACTION_REFRESH_IN:
							drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
							drawerDLtoggle.setToolbarNavigationClickListener(null);
							drawerDLtoggle.syncState();

							prevIV.setAlpha(130);
							historyIV.setAlpha(130);
							nextIV.setAlpha(130);

							((SwipeRefreshLayout) GradesFragment.gradesLayout).setRefreshing(true);
							break;
						case HeliniumStudentApp.ACTION_REFRESH_OUT:
							drawerDL.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
							drawerDLtoggle.setToolbarNavigationClickListener(new View.OnClickListener() {

								@Override
								public void onClick(View v) {
									drawerDL.openDrawer(drawerNV);
								}
							});
							drawerDLtoggle.syncState();

							historyIV.setAlpha(255);

							switch (GradesFragment.termFocus) {
								case 1:
									prevIV.setAlpha(130);
									nextIV.setAlpha(255);
									break;
								case 4:
									prevIV.setAlpha(255);
									nextIV.setAlpha(130);
									break;
								default:
									prevIV.setAlpha(255);
									nextIV.setAlpha(255);
									break;
							}

							((SwipeRefreshLayout) GradesFragment.gradesLayout).setRefreshing(false);
							break;
						/*case HeliniumStudentApp.ERR_UNDEFINED:
						case HeliniumStudentApp.ERR_OK:
							//FIXME HANDLE!!!
							break;*/
					}
					break;
			}
		}
	}

	protected static void setColors(int theme, int colorPrimary, int colorAccent) {
		final String[] colors = new String[] { "red", "pink", "purple", "deep_purple", "indigo", "blue", "light_blue", "cyan", "teal", "green", "light_green", "lime", "yellow", "amber", "orange",
				"deep_orange", "brown", "dark_grey", "grey", "blue_grey", "white" };

		if (theme != HeliniumStudentApp.ACTION_NULL)
			if (theme == 0) {
				themeColor = R.color.theme_light;
				themeDialog = R.style.lightDialog;
				themeSettings = R.style.lightSettings;
				themeDisabledTextColor = R.color.text_disabled_dark;
				themeDividerColor = R.color.divider_dark;
				themePrimaryTextColor = R.color.text_dark;
				themeSecondaryTextColor = R.color.text_secondary_dark;
			} else {
				themeColor = R.color.theme_dark;
				themeDialog = R.style.darkDialog;
				themeSettings = R.style.darkSettings;
				themeDisabledTextColor = R.color.text_disabled_light;
				themeDividerColor = R.color.divider_light;
				themePrimaryTextColor = R.color.text_light;
				themeSecondaryTextColor = R.color.text_secondary_light;
			}

		if (colorPrimary != HeliniumStudentApp.ACTION_NULL) {
			darkPrimaryColor = mainContext.getResources().getIdentifier(colors[colorPrimary] + "_dark", "color", mainContext.getPackageName());
			primaryColor = mainContext.getResources().getIdentifier(colors[colorPrimary], "color", mainContext.getPackageName());
			secondaryColor = mainContext.getResources().getIdentifier(colors[colorPrimary] + "_secondary", "color", mainContext.getPackageName());

			if (colorPrimary == 6 || colorPrimary == 7 || (colorPrimary > 8 && colorPrimary < 15) || colorPrimary == 18 || colorPrimary == 20) {
				primaryTextColor = R.color.text_dark;
				secondaryTextColor = R.color.text_secondary_dark;
			} else {
				primaryTextColor = R.color.text_light;
				secondaryTextColor = R.color.text_secondary_light;
			}
		}

		if (colorAccent != HeliniumStudentApp.ACTION_NULL) {
			accentPrimaryColor = mainContext.getResources().getIdentifier(colors[colorAccent] + "_dark", "color", mainContext.getPackageName());
			accentSecondaryColor = mainContext.getResources().getIdentifier(colors[colorAccent], "color", mainContext.getPackageName());

			if (colorAccent > 5)
				accentTextColor = R.color.text_dark;
			else
				accentTextColor = R.color.text_light;
		}
	}

	protected static void setStatusBar(Activity context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			context.getWindow().setStatusBarColor(Color.TRANSPARENT);

			if (context == mainContext)
				drawerDL.setStatusBarBackgroundColor(ContextCompat.getColor(context, darkPrimaryColor));
			else
				context.getWindow().setStatusBarColor(ContextCompat.getColor(context, darkPrimaryColor));

			context.setTaskDescription(new ActivityManager.TaskDescription(context.getString(R.string.app_name),
					BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher), ContextCompat.getColor(context, primaryColor)));

		}
	}

	protected static void setToolbarTitle(AppCompatActivity context, String title, String subtitle) {
		final Spannable toolbarTitle = new SpannableString(title);
		toolbarTitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, primaryTextColor)), 0, toolbarTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		try {
			context.getSupportActionBar().setTitle(toolbarTitle);
		} catch (NullPointerException e) {
			return;
		}

		if (subtitle != null) {
			final Spannable toolbarSubtitle = new SpannableString(subtitle);
			toolbarSubtitle.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, secondaryTextColor)), 0,
					toolbarSubtitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			context.getSupportActionBar().setSubtitle(toolbarSubtitle);
		}
	}

	protected static class GetLoginCookie extends AsyncTask<Object, Void, Integer> {
		private int view;
		private String url;
		private int focus;
		private int direction;
		private int transition;
		private boolean display;

		@Override
		protected Integer doInBackground(Object... params) {
			view = (int) params[0];
			if (view == HeliniumStudentApp.VIEW_GRADES) {
				url = (String) params[1];
				direction = (int) params[2];
				transition = (int) params[3];
			} else if (view != HeliniumStudentApp.VIEW_LOGIN) {
				url = (String) params[1];
				focus = (int) params[2];
				direction = (int) params[3];
				transition = (int) params[4];
				if (view == HeliniumStudentApp.VIEW_SCHEDULE) display = (boolean) params[5]; //TODO That's always the case right?
			}

			OutputStream output = null;

			cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(cookies);

			try {
				final URLConnection connection = new URL(HeliniumStudentApp.URL_LOGIN).openConnection();

				connection.setConnectTimeout(HeliniumStudentApp.TIMEOUT_CONNECT);
				connection.setReadTimeout(HeliniumStudentApp.TIMEOUT_READ);

				connection.setDoOutput(true);
				((HttpURLConnection) connection).setInstanceFollowRedirects(false);
				connection.setRequestProperty("Accept-Charset", HeliniumStudentApp.CHARSET);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + HeliniumStudentApp.CHARSET);

				output = connection.getOutputStream();
				output.write(String.format(
						"wu_loginname=%s&wu_password=%s",
						URLEncoder.encode(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("username", ""), HeliniumStudentApp.CHARSET),
						URLEncoder.encode(PreferenceManager.getDefaultSharedPreferences(mainContext).getString("password", ""), HeliniumStudentApp.CHARSET) +
								"&Login=Inloggen&path=%2F%3F").getBytes(HeliniumStudentApp.CHARSET));

				final List<String> setCookie = connection.getHeaderFields().get("Set-Cookie");

				if (setCookie != null) for (String cookie : setCookie) cookies.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));

				((HttpURLConnection) connection).disconnect();

				switch (((HttpURLConnection) connection).getResponseCode()) {
					case 302:
						return HeliniumStudentApp.OK;
					case 200: //TODO Other error messages that the website can give besides wrong user/pass (that don't cause a redirect)?
						return HeliniumStudentApp.ERR_USERPASS;
					default:
						return HeliniumStudentApp.ERR_OK;
				}
			} catch (IOException e) {
				return HeliniumStudentApp.ERR_IO;
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException ignored) {}
				}
			}
		}

		@Override
		protected void onPostExecute(Integer returnCode) {
			if (returnCode == HeliniumStudentApp.OK) {
				switch (view) {
					case HeliniumStudentApp.VIEW_LOGIN:
						LoginActivity.authenticationProgressDialog.cancel();

						mainContext.startActivity(new Intent(LoginActivity.loginContext, MainActivity.class));
						LoginActivity.loginContext.finish();
						break;
					case HeliniumStudentApp.VIEW_SCHEDULE:
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
							new ScheduleFragment.GetScheduleData().execute(url, focus, direction, transition, display);
						else
							new ScheduleFragment.GetScheduleData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, focus, direction, transition, display);
						break;
					case HeliniumStudentApp.VIEW_GRADES:
						if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
							new GradesFragment.GetGradesData().execute(url, direction, transition);
						else
							new GradesFragment.GetGradesData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, direction, transition);
						break;
				}
			} else {
				recoverError(view, returnCode, direction, transition);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) UpdateClass.downloadAPK();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (drawerDL.isDrawerOpen(drawerNV))
				drawerDL.closeDrawers();
			else
				drawerDL.openDrawer(drawerNV);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		if (drawerDL.isDrawerOpen(drawerNV))
			drawerDL.closeDrawers();
		else
			moveTaskToBack(true);
	}
}