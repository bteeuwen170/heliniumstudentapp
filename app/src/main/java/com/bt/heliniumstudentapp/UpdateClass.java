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

import android.app.Activity;
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
import android.support.v7.internal.view.ContextThemeWrapper;
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

	private static boolean beta;
	private static int versionCode;

	protected UpdateClass(Activity context, boolean settings) {
		UpdateClass.context = context;
		this.settings = settings;

		final AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, MainActivity.themeDialog));

		updateDialogBuilder.setTitle(R.string.update_checking);

		updateDialogBuilder.setNegativeButton(R.string.cancel, null);
		updateDialogBuilder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) { //FIXME Come on, don't do this up here
				downloadAPK(String.valueOf(versionCode), beta);
			}
		});

		updateDialogBuilder.setView(R.layout.dialog_update);

		updateDialog = updateDialogBuilder.create();

		updateDialog.setCanceledOnTouchOutside(true);

		if (settings) {
			updateDialog.show();

			updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

			updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(MainActivity.accentPrimaryColor));
			updateDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(MainActivity.accentSecondaryColor));
		}
	}

	@Override
	protected String doInBackground(Void... Void) {
		try {
			URLConnection connection = new URL(MyApplication.URL_UPDATE_CHANGELOG).openConnection();

			connection.setConnectTimeout(MyApplication.TIMEOUT_CONNECT);
			connection.setReadTimeout(MyApplication.TIMEOUT_READ);

			connection.setRequestProperty("Accept-Charset", MyApplication.CHARSET);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + MyApplication.CHARSET);

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

				final String versionName = json.optString("version_name");
				versionCode = json.optInt("version_code");

				try {
					//final String currentVersionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
					final int currentVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;

					//final boolean betas = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_general_advanced", false);

					if (versionCode > currentVersionCode/* && !versionName.contains("-beta")) || (betas && currentVersionName.contains("-beta") &&
							!versionName.endsWith(String.valueOf(currentVersionName.substring(currentVersionName.indexOf("-beta")))))*/) {
						//if (betas && !versionName.endsWith(String.valueOf(currentVersionName.substring(currentVersionName.indexOf("-beta"))))) beta = true;
						if (!settings) updateDialog.show();

						updateDialog.setTitle(context.getString(R.string.update) + ' ' + versionName);

						final TextView contentTV = (TextView) updateDialog.findViewById(R.id.tv_content_du);
						contentTV.setTextColor(context.getResources().getColor(MainActivity.themePrimaryTextColor));
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

						updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(MainActivity.accentSecondaryColor));
					} else if (settings) {
						updateDialog.setTitle(context.getString(R.string.update_no));

						final TextView contentTV = (TextView) updateDialog.findViewById(R.id.tv_content_du);
						contentTV.setTextColor(context.getResources().getColor(MainActivity.themePrimaryTextColor));
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
					downloadAPK("unknown", false);
				}
			});

			updateDialogBuilder.setNegativeButton(android.R.string.no, null);

			final AlertDialog updateDialog = updateDialogBuilder.create();

			updateDialog.setCanceledOnTouchOutside(true);
			updateDialog.show();

			updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(MainActivity.accentSecondaryColor));
			updateDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(MainActivity.accentSecondaryColor));
		}
	}

	private static void downloadAPK(final String version, final boolean beta) {
		if (new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/heliniumleerlingenweb_" + version + ".apk").exists()) {
			final Intent install = new Intent(Intent.ACTION_VIEW);
			install.setDataAndType(Uri.fromFile(
					new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/heliniumleerlingenweb_" + version + ".apk")), "application/vnd.android.package-archive");
			context.startActivity(install);
		} else {
			if (MainActivity.isOnline()) {
				DownloadManager.Request request;

				if (beta)
					request = new DownloadManager.Request(Uri.parse(MyApplication.URL_UPDATE_BETA));
				else
					request = new DownloadManager.Request(Uri.parse(MyApplication.URL_UPDATE_RELEASE));

				request.setTitle("Helinium Leerlingenweb " + version);
				request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/heliniumleerlingenweb_" + version + ".apk");

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					request.allowScanningByMediaScanner(); //TODO Necessary?
					request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
				}

				((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);

				context.registerReceiver(new BroadcastReceiver() {

					@Override
					public void onReceive(Context context, Intent intent) {
						final Intent install = new Intent(Intent.ACTION_VIEW);
						install.setDataAndType(Uri.fromFile(
								new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/heliniumleerlingenweb_" + version + ".apk")
						), "application/vnd.android.package-archive");
						context.startActivity(install);
					}
				}, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
			} else {
				Toast.makeText(context, R.string.error_conn_no, Toast.LENGTH_SHORT).show();
			}
		}
	}
}