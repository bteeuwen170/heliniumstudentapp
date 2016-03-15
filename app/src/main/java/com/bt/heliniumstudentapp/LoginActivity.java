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

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
	protected static AppCompatActivity loginContext;

	protected static ProgressDialog authenticationProgressDialog;

	private EditText usernameET, passwordET;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		loginContext = this;

		getWindow().getDecorView().setBackgroundResource(R.color.indigo);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setStatusBarColor(Color.TRANSPARENT);
			getWindow().setStatusBarColor(getResources().getColor(R.color.indigo));
			setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher),
					getResources().getColor(R.color.indigo)));
		}

		usernameET = (EditText) findViewById(R.id.et_username_la);
		passwordET = (EditText) findViewById(R.id.et_password_la);
		final Button loginBtn = (Button) findViewById(R.id.btn_login_la);

		usernameET.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("username", ""));

		usernameET.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				loginBtn.setEnabled((usernameET.length() != 0 && passwordET.length() != 0));
			}

			public void afterTextChanged(Editable s) {}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		});

		passwordET.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				loginBtn.setEnabled((usernameET.length() != 0 && passwordET.length() != 0));
			}

			public void afterTextChanged(Editable s) {}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		});

		View.OnClickListener login = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				refresh();
			}
		};

		loginBtn.setOnClickListener(login);
	}

	private void refresh() {
		if (MainActivity.isOnline()) {
			if (usernameET.length() != 0 || passwordET.length() != 0) {
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), 0);

				PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString("username", usernameET.getEditableText().toString()).apply();
				PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString("password", passwordET.getEditableText().toString()).apply();

				authenticationProgressDialog = new ProgressDialog(new ContextThemeWrapper(LoginActivity.this, MainActivity.themeDialog));
				authenticationProgressDialog.setCancelable(false);
				authenticationProgressDialog.setMessage(getResources().getString(R.string.authenticating));
				authenticationProgressDialog.show();

				new MainActivity.GetLoginCookie().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, HeliniumStudentApp.VIEW_LOGIN);
			}
		} else {
			if (!MainActivity.displayingSnackbar) {
				final Snackbar noConnectionSB = Snackbar.make(findViewById(R.id.cl_snackbar_al), R.string.error_conn_no, Snackbar.LENGTH_LONG).setAction(R.string.retry, new View.OnClickListener() {

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
		}
	}
}