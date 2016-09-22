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

import android.Manifest;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Editable;
import android.text.Html;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class UpdateClass extends AsyncTask<Void, Integer, String> {
	protected static Activity context;
	private boolean settings;

	private static AlertDialog updateDialog;

	protected static String versionName;

	protected UpdateClass(final Activity context, boolean settings) {
		UpdateClass.context = context;
		this.settings = settings;

		final AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, MainActivity.themeDialog));

		updateDialogBuilder.setTitle(R.string.update_checking);

		updateDialogBuilder.setNegativeButton(R.string.cancel, null);
		updateDialogBuilder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) { //FIXME Come on, don't do this up here
				ActivityCompat.requestPermissions(context, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1); //TODO See warning
				//FIXME Great, now everything is protected...
			}
		});

		updateDialogBuilder.setView(R.layout.dialog_update);

		updateDialog = updateDialogBuilder.create();

		updateDialog.setCanceledOnTouchOutside(true);

		if (settings) {
			updateDialog.show();

			updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

			updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, MainActivity.accentPrimaryColor));
			updateDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, MainActivity.accentSecondaryColor));
		}
	}

	@Override
	protected String doInBackground(Void... Void) {
		try {
			URLConnection connection = new URL(HeliniumStudentApp.URL_UPDATE_CHANGELOG).openConnection();

			connection.setConnectTimeout(HeliniumStudentApp.TIMEOUT_CONNECT);
			connection.setReadTimeout(HeliniumStudentApp.TIMEOUT_READ);

			connection.setRequestProperty("Accept-Charset", HeliniumStudentApp.CHARSET);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + HeliniumStudentApp.CHARSET);

			connection.connect();

			final Scanner line = new Scanner(connection.getInputStream()).useDelimiter("\\A");
			final String html = line.hasNext() ? line.next() : "";

			((HttpURLConnection) connection).disconnect();

			if (((HttpURLConnection) connection).getResponseCode() == 200)
				return html;
			else
				return null;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	protected void onPostExecute(String html) {
		if (html != null) {
			try {
				final JSONObject json = (JSONObject) new JSONTokener(html).nextValue();

				versionName = json.optString("version_name");
				final int versionCode = json.optInt("version_code");

				try {
					final int currentVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;

					if (versionCode > currentVersionCode) {
						if (!settings) updateDialog.show();

						updateDialog.setTitle(context.getString(R.string.update) + ' ' + versionName);

						final TextView contentTV = (TextView) updateDialog.findViewById(R.id.tv_content_du);
						contentTV.setTextColor(ContextCompat.getColor(context, MainActivity.themePrimaryTextColor));
						contentTV.setText(Html.fromHtml(json.optString("content"), null, new Html.TagHandler() {

							@Override
							public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
								if ("li".equals(tag))
									if (opening)
										output.append(" \u2022 ");
									else
										output.append("\n");
							}
						}));

						updateDialog.setCanceledOnTouchOutside(true);

						updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

						updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, MainActivity.accentSecondaryColor));
					} else if (settings) {
						updateDialog.setTitle(context.getString(R.string.update_no));

						final TextView contentTV = (TextView) updateDialog.findViewById(R.id.tv_content_du);
						contentTV.setTextColor(ContextCompat.getColor(context, MainActivity.themePrimaryTextColor));
						contentTV.setText(Html.fromHtml(json.optString("content"), null, new Html.TagHandler() {

							@Override
							public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
								if ("li".equals(tag))
									if (opening)
										output.append(" \u2022 ");
									else
										output.append("\n");
							}
						}));

						updateDialog.setCanceledOnTouchOutside(true);
					}
				} catch (NameNotFoundException e) {
					Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
				}
			} catch (JSONException e) {
				Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show();
			}
		} else if (settings) {
			updateDialog.cancel();

			final AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, MainActivity.themeDialog));

			updateDialogBuilder.setTitle(context.getString(R.string.update));

			updateDialogBuilder.setMessage(R.string.error_update);

			updateDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadAPK();
				}
			});

			updateDialogBuilder.setNegativeButton(android.R.string.no, null);

			final AlertDialog updateDialog = updateDialogBuilder.create();

			updateDialog.setCanceledOnTouchOutside(true);
			updateDialog.show();

			updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, MainActivity.accentSecondaryColor));
			updateDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, MainActivity.accentSecondaryColor));
		}
	}

	protected static void downloadAPK() {
		File oldUpdate = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/heliniumstudentapp.apk");
		if (oldUpdate.exists()) //noinspection ResultOfMethodCallIgnored
			oldUpdate.delete();

		if (MainActivity.isOnline()) {
			DownloadManager.Request request;

			request = new DownloadManager.Request(Uri.parse(HeliniumStudentApp.URL_UPDATE_RELEASE));

			request.setTitle(context.getString(R.string.app_name) + " " + versionName);
			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/heliniumstudentapp.apk");

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

			((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);

			context.registerReceiver(new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
					final Intent install = new Intent(Intent.ACTION_VIEW);
					install.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/heliniumstudentapp.apk")),
							"application/vnd.android.package-archive");
					context.startActivity(install);
				}
			}, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		} else {
			Toast.makeText(context, R.string.error_conn_no, Toast.LENGTH_SHORT).show();
		}
	}
}