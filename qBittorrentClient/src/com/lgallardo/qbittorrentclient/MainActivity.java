/*******************************************************************************
 * Copyright (c) 2014 Luis M. Gallardo D..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Luis M. Gallardo D.
 ******************************************************************************/
package com.lgallardo.qbittorrentclient;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	// Params to get JSON Array
	private static String[] params = new String[2];

	// JSON Node Names
	protected static final String TAG_USER = "user";
	protected static final String TAG_ID = "id";
	protected static final String TAG_ALTDWLIM = "alt_dl_limit";
	protected static final String TAG_DWLIM = "dl_limit";

	// Torrent Info TAGs
	protected static final String TAG_NAME = "name";
	protected static final String TAG_SIZE = "size";
	protected static final String TAG_PROGRESS = "progress";
	protected static final String TAG_STATE = "state";
	protected static final String TAG_HASH = "hash";
	protected static final String TAG_DLSPEED = "dlspeed";
	protected static final String TAG_UPSPEED = "upspeed";
	protected static final String TAG_NUMLEECHS = "num_leechs";
	protected static final String TAG_NUMSEEDS = "num_seeds";
	protected static final String TAG_RATIO = "ratio";
	protected static final String TAG_PRIORITY = "priority";

	protected static final String TAG_INFO = "info";

	protected static final String TAG_ACTION = "action";
	protected static final String TAG_START = "start";
	protected static final String TAG_PAUSE = "pause";
	protected static final String TAG_DELETE = "delete";
	protected static final String TAG_DELETE_DRIVE = "deleteDrive";

	protected static final int ACTION_CODE = 0;
	protected static final int START_CODE = 1;
	protected static final int PAUSE_CODE = 2;
	protected static final int DELETE_CODE = 3;
	protected static final int DELETE_DRIVE_CODE = 4;

	protected static String hostname;
	protected static int port;
	protected static String protocol;
	protected static String username;
	protected static String password;
	protected static boolean oldVersion;

	// Preferences fields
	private SharedPreferences sharedPrefs;
	private StringBuilder builderPrefs;

	static Torrent[] lines;
	static String[] names;

	TextView name1, size1;

	// Drawer properties
	private String[] navigationDrawerItemTitles;
	private DrawerLayout drawerLayout;
	private ListView drawerList;
	private CharSequence drawerTitle;
	private CharSequence title;
	// For app icon control for nav drawer, add new property on MainActivity
	private ActionBarDrawerToggle drawerToggle;

	private ItemstFragment firstFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i("onCreate", "Loading activity_main");

		setContentView(R.layout.activity_main);

		Log.i("onCreate", "Starting drawe menu");

		// Drawer menu
		navigationDrawerItemTitles = getResources().getStringArray(
				R.array.navigation_drawer_items_array);

		for (int i = 0; i < navigationDrawerItemTitles.length; i++) {

			Log.i("onCreate", "navigationDrawerItemTitles: "
					+ navigationDrawerItemTitles[i]);
		}

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		Log.i("onCreate", "DrawerLayout: " + drawerLayout.toString());

		drawerList = (ListView) findViewById(R.id.left_drawer);

		// Drawer item list objects
		ObjectDrawerItem[] drawerItem = new ObjectDrawerItem[7];

		drawerItem[0] = new ObjectDrawerItem(R.drawable.ic_drawer_all, "All");
		drawerItem[1] = new ObjectDrawerItem(R.drawable.ic_drawer_downloading,
				"Downloading");
		drawerItem[2] = new ObjectDrawerItem(R.drawable.ic_drawer_completed,
				"Completed");
		drawerItem[3] = new ObjectDrawerItem(R.drawable.ic_drawer_paused,
				"Paused");
		drawerItem[4] = new ObjectDrawerItem(R.drawable.ic_drawer_active,
				"Active");
		drawerItem[5] = new ObjectDrawerItem(R.drawable.ic_drawer_inactive,
				"Inacctive");
//		drawerItem[6] = new ObjectDrawerItem(R.drawable.ic_action_completed,
//				"Options");
		drawerItem[6] = new ObjectDrawerItem(R.drawable.ic_drawer_settings,
				"Settings");


		// Create object for drawer item OnbjectDrawerItem
		DrawerItemCustomAdapter adapter = new DrawerItemCustomAdapter(this,
				R.layout.listview_item_row, drawerItem);
		drawerList.setAdapter(adapter);

		// Set the item click listener
		drawerList.setOnItemClickListener(new DrawerItemClickListener());

		// Get drawer title
		title = drawerTitle = getTitle();

		// Add the application icon control code inside MainActivity onCreate

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getActionBar().setTitle(title);
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getActionBar().setTitle(drawerTitle);
			}
		};

		drawerLayout.setDrawerListener(drawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// Get preferences
		getPreferences();

		// If it were awaked from an intent-filter,
		// get intent from the intent filter and Add URL torrent
		Intent intent = getIntent();
		String urlTorrent = intent.getDataString();

		if (urlTorrent != null && urlTorrent.length() != 0) {
			addTorrent(intent.getDataString());
		}

		// Refresh UI
		refresh();

		// Fragments

		// Check whether the activity is using the layout version with
		// the fragment_container FrameLayout. If so, we must add the first
		// fragment
		if (findViewById(R.id.fragment_container) != null) {

			// However, if we're being restored from a previous state,
			// then we don't need to do anything and should return or else
			// we could end up with overlapping fragments.
			if (savedInstanceState != null) {
				return;
			}

			// Create an instance of ExampleFragment
			firstFragment = new ItemstFragment();

			firstFragment.setSecondFragmentContainer(R.id.content_frame);

			// Add the fragment to the 'list_frame' FrameLayout
			FragmentManager fragmentManager = getFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();

			fragmentTransaction.add(R.id.list_frame, firstFragment);

			fragmentTransaction.commit();

			// Second fragment will be added in ItemsFRagment's onListItemClick
			// method

		}else{
			
			// Phones handle just one fragment
			
			
			// However, if we're being restored from a previous state,
			// then we don't need to do anything and should return or else
			// we could end up with overlapping fragments.
			if (savedInstanceState != null) {
				return;
			}

			// Create an instance of ExampleFragment
			firstFragment = new ItemstFragment();

			firstFragment.setSecondFragmentContainer(R.id.one_frame);

			// Add the fragment to the 'list_frame' FrameLayout
			FragmentManager fragmentManager = getFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();

			fragmentTransaction.add(R.id.one_frame, firstFragment);
			fragmentTransaction.addToBackStack(null);
			
			fragmentTransaction.commit();
		}
			

		if (savedInstanceState == null) {
			selectItem(0);
		}
	}

	// Drawer's method

	@Override
	public void setTitle(CharSequence title) {
		this.title = title;
		getActionBar().setTitle(title);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	// MainActivity old methods

	// Rotation handling
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		LinearLayout container = (LinearLayout) findViewById(R.id.container);

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			container.setOrientation(LinearLayout.HORIZONTAL);
		} else {
			container.setOrientation(LinearLayout.VERTICAL);
		}
	}

	private void refresh(){
		
		refresh("all");
		
	}
	private void refresh(String state) {

		if (oldVersion == true) {
			params[0] = "json/events";
		} else {
			params[0] = "json/torrents";
		}
		
		params[1] = state;

		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

		if (networkInfo != null && networkInfo.isConnected() && !networkInfo.isFailover()) {

			// Execute the task in background
			qBittorrentTask qtt = new qBittorrentTask();

			// Connecting message
			Toast.makeText(getApplicationContext(), R.string.connecting,
					Toast.LENGTH_LONG).show();

			qtt.execute(params);
		} else {

			// Connection Error message
			Toast.makeText(getApplicationContext(), R.string.connection_error,
					Toast.LENGTH_SHORT).show();

		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		TorrentDetailsFragment tf = (TorrentDetailsFragment) getFragmentManager()
				.findFragmentById(R.id.content_frame);

		int position;
		String hash;

		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		case R.id.action_refresh:
			// Refresh option clicked.
			refresh();
			return true;
		case R.id.action_add:
			// Add URL torrent
			addUrlTorrent();
			return true;

		case R.id.action_pause:
			if (tf != null) {
				position = tf.position;
				hash = MainActivity.lines[position].getHash();
				pauseTorrent(hash);
			}
			return true;
		case R.id.action_resume:
			if (tf != null) {
				position = tf.position;
				hash = MainActivity.lines[position].getHash();
				startTorrent(hash);				
			}
			return true;
		case R.id.action_delete:
			if (tf != null) {
				position = tf.position;
				hash = MainActivity.lines[position].getHash();
				deleteTorrent(hash);
			}
			return true;
		case R.id.action_delete_drive:
			if (tf != null) {
				position = tf.position;
				hash = MainActivity.lines[position].getHash();
				deleteDriveTorrent(hash);
			}
			return true;
		case R.id.action_resume_all:
			resumeAllTorrents();
			return true;
		case R.id.action_pause_all:
			pauseAllTorrents();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		// Make sure the request was successful
		if (resultCode == RESULT_OK && requestCode == ACTION_CODE) {

			// Get Torrent's action
			int action = data.getIntExtra(TAG_ACTION, 0);

			// Get torrent's hash
			String hash = data.getStringExtra(TAG_HASH);

			// Check which request we're responding to
			switch (action) {
			case START_CODE:
				startTorrent(hash);
				break;
			case PAUSE_CODE:
				pauseTorrent(hash);
				break;
			case DELETE_CODE:
				deleteTorrent(hash);
				break;
			case DELETE_DRIVE_CODE:
				deleteDriveTorrent(hash);
				break;
			}

		}

	}

	private void addUrlTorrent() {

		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(MainActivity.this);
		View addTorrentView = li.inflate(R.layout.add_torrent, null);

		// URL input
		final EditText urlInput = (EditText) addTorrentView
				.findViewById(R.id.url);

		// Dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

		// Set add_torrent.xml to AlertDialog builder
		builder.setView(addTorrentView);

		// Cancel
		builder.setNeutralButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User cancelled the dialog
					}
				});

		// Ok
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User accepted the dialog
						addTorrent(urlInput.getText().toString());
					}
				});

		// Create dialog
		AlertDialog dialog = builder.create();

		// Show dialog
		dialog.show();

	}

	private void openPreferences() {
		// TODO Auto-generated method stub
		Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
		startActivity(intent);

	}

	// @Override
	// public void onListItemClick(ListView parent, View v, int position, long
	// id) {
	// // selection.setText(items[position]);
	//
	// Intent intent = new Intent(this, TorrentActionsActivity.class);
	//
	// // Torrent info
	// intent.putExtra(TAG_NAME, MainActivity.lines[position].getFile());
	// intent.putExtra(TAG_SIZE, MainActivity.lines[position].getSize());
	// intent.putExtra(TAG_INFO, MainActivity.lines[position].getInfo());
	// intent.putExtra(TAG_RATIO, MainActivity.lines[position].getRatio());
	// intent.putExtra(TAG_PROGRESS,
	// MainActivity.lines[position].getProgress());
	// intent.putExtra(TAG_STATE, MainActivity.lines[position].getState());
	// intent.putExtra(TAG_NUMLEECHS, MainActivity.lines[position].getLeechs());
	// intent.putExtra(TAG_NUMSEEDS, MainActivity.lines[position].getSeeds());
	// intent.putExtra(TAG_PRIORITY,
	// MainActivity.lines[position].getPriority());
	//
	// intent.putExtra(TAG_HASH, MainActivity.lines[position].getHash());
	//
	// // Http client params
	// intent.putExtra("hostname", hostname);
	// intent.putExtra("protocol", protocol);
	// intent.putExtra("port", port);
	// intent.putExtra("username", username);
	// intent.putExtra("password", password);
	//
	// startActivityForResult(intent, ACTION_CODE);
	// }

	public void startTorrent(String hash) {
		// Execute the task in background
		qBittorrentCommand qtc = new qBittorrentCommand();
		qtc.execute(new String[] { "start", hash });
	}

	public void pauseTorrent(String hash) {
		// Execute the task in background
		qBittorrentCommand qtc = new qBittorrentCommand();
		qtc.execute(new String[] { "pause", hash });
	}

	public void deleteTorrent(String hash) {
		// Execute the task in background
		qBittorrentCommand qtc = new qBittorrentCommand();
		qtc.execute(new String[] { "delete", hash });
	}

	public void deleteDriveTorrent(String hash) {
		// Execute the task in background
		qBittorrentCommand qtc = new qBittorrentCommand();
		qtc.execute(new String[] { "deleteDrive", hash });
	}

	public void addTorrent(String url) {
		// Execute the task in background
		qBittorrentCommand qtc = new qBittorrentCommand();
		qtc.execute(new String[] { "addTorrent", url });
	}

	public void pauseAllTorrents() {
		// Execute the task in background
		qBittorrentCommand qtc = new qBittorrentCommand();
		qtc.execute(new String[] { "pauseAll", null });
	}

	public void resumeAllTorrents() {
		// Execute the task in background
		qBittorrentCommand qtc = new qBittorrentCommand();
		qtc.execute(new String[] { "resumeAll", null });
	}

	// Get Preferences
	protected void getPreferences() {
		// Preferences stuff
		sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(MainActivity.this);

		builderPrefs = new StringBuilder();

		builderPrefs.append("\n" + sharedPrefs.getString("language", "NULL"));

		// Get values from preferences
		hostname = sharedPrefs.getString("hostname", "NULL");
		protocol = sharedPrefs.getString("protocol", "NULL");
		port = Integer.parseInt(sharedPrefs.getString("port", "80"));
		username = sharedPrefs.getString("username", "NULL");
		password = sharedPrefs.getString("password", "NULL");
		oldVersion = sharedPrefs.getBoolean("old_version", false);
	}

	// Here is where the action happens
	private class qBittorrentCommand extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {

			// Get values from preferences
			getPreferences();

			// Creating new JSON Parser
			JSONParser jParser = new JSONParser(hostname, port, username,
					password);

			jParser.postCommand(params[0], params[1]);

			return params[0];

		}

		@Override
		protected void onPostExecute(String result) {

			int messageId = R.string.connection_error;

			if (result == null) {
				messageId = R.string.connection_error;
			}

			if ("start".equals(result)) {
				messageId = R.string.torrentStarted;
			}

			if ("pause".equals(result)) {
				messageId = R.string.torrentPaused;
			}

			if ("delete".equals(result)) {
				messageId = R.string.torrentDeleled;
			}

			if ("deleteDrive".equals(result)) {
				messageId = R.string.torrentDeletedDrive;
			}

			if ("addTorrent".equals(result)) {
				messageId = R.string.torrentAdded;
			}

			if ("pauseAll".equals(result)) {
				messageId = R.string.AllTorrentsPaused;
			}

			if ("resumeAll".equals(result)) {
				messageId = R.string.AllTorrentsResumed;
			}

			Toast.makeText(getApplicationContext(), messageId,
					Toast.LENGTH_LONG).show();

		}
	}

	// Here is where the action happens
	private class qBittorrentTask extends AsyncTask<String, Integer, Torrent[]> {

		@Override
		protected Torrent[] doInBackground(String... params) {

			String name, size, info, progress, state, hash, ratio, leechs, seeds, priority;

			Torrent[] torrents = null;
			
			// Preferences stuff
			getPreferences();

			// Creating new JSON Parser
			JSONParser jParser = new JSONParser(hostname, port, username,
					password);

			JSONArray jArray = jParser.getJSONArrayFromUrl(params[0]);

			if (jArray != null) {

				// Log.i("jArray length", "" + jArray.length());

				try {

					torrents = new Torrent[jArray.length()];

					MainActivity.names = new String[jArray.length()];

					for (int i = 0; i < jArray.length(); i++) {

						
						JSONObject json = jArray.getJSONObject(i);

						name = json.getString(TAG_NAME);
						size = json.getString(TAG_SIZE);
						progress = String.format("%.2f",
								json.getDouble(TAG_PROGRESS) * 100)
								+ "%";
						info = size + " | D:" + json.getString(TAG_DLSPEED)
								+ " | U:" + json.getString(TAG_UPSPEED) + " | "
								+ progress;
						state = json.getString(TAG_STATE);
						hash = json.getString(TAG_HASH);
						ratio = json.getString(TAG_RATIO);
						leechs = json.getString(TAG_NUMLEECHS);
						seeds = json.getString(TAG_NUMSEEDS);
						priority = json.getString(TAG_PRIORITY);
						
						torrents[i] = new Torrent(name, size, state, hash,
								info, ratio, progress, leechs, seeds, priority);

						MainActivity.names[i] = name;
						
					}
				} catch (JSONException e) {
					Log.e("MAIN:", e.toString());
				}

			}
			return torrents;

		}

		@Override
		protected void onPostExecute(Torrent[] result) {

			if (result == null) {

				Toast.makeText(getApplicationContext(),
						R.string.connection_error, Toast.LENGTH_LONG).show();

			} else {

				
				ArrayList<Torrent> torrentsFiltered = new ArrayList<Torrent>();
				
				for(int i=0; i<result.length;i++){
					
					if(params[1].equals("all")){						 
						torrentsFiltered.add(result[i]);
					}
		
					if(params[1].equals("downloading")){
						if("downloading".equals(result[i].getState()) || "stalledDL".equals(result[i].getState()) || "pausedDL".equals(result[i].getState())|| "queueDL".equals(result[i].getState()) || "checkingDL".equals(result[i].getState())){
							torrentsFiltered.add(result[i]);
						}
					}
					
					if(params[1].equals("completed")){
						if("uploading".equals(result[i].getState()) || "stalledUP".equals(result[i].getState()) || "pausedUP".equals(result[i].getState())|| "queueUP".equals(result[i].getState()) || "checkingUP".equals(result[i].getState())){
							torrentsFiltered.add(result[i]);
						}
					}
					
					if(params[1].equals("paused")){
						if("pausedDL".equals(result[i].getState()) || "pausedUP".equals(result[i].getState())){
							torrentsFiltered.add(result[i]);
						}
					}
					
					if(params[1].equals("active")){
						if("uploading".equals(result[i].getState()) || "downloading".equals(result[i].getState())){
							torrentsFiltered.add(result[i]);
						}
					}
					
					if(params[1].equals("inactive")){
						if("pausedUP".equals(result[i].getState()) || "pausedDL".equals(result[i].getState()) || "queueUP".equals(result[i].getState())|| "queueDL".equals(result[i].getState()) || "stalledUP".equals(result[i].getState()) || "stalledDL".equals(result[i].getState())){
							torrentsFiltered.add(result[i]);
						}
					}
					
				}
				
				
				
				//MainActivity.lines = (torrent[]) torrentsFiltered.toArray();
				
				
				// Get names (delete in background method)
				MainActivity.names = new String[torrentsFiltered.size()];
				MainActivity.lines = new Torrent[torrentsFiltered.size()];
				
				for(int i=0; i<torrentsFiltered.size() ;i++){
					
					Torrent torrent = torrentsFiltered.get(i);
					
					MainActivity.names[i] = torrent.getFile();
					MainActivity.lines[i] = torrent;
				}
				

				try {

					// ListFragment fList = (ListFragment) listFragment;
					// fList.setListAdapter(new myAdapter());

					firstFragment.setListAdapter(new myAdapter());

					// firstFragment.getListView().setItemChecked(0, true);
					firstFragment.getListView().setChoiceMode(
							ListView.CHOICE_MODE_SINGLE);

				} catch (Exception e) {
					// TODO: handle exception
					Log.e("ADAPTER", e.toString());
				}

			}
		}

	}

	class myAdapter extends ArrayAdapter<String> {
		public myAdapter() {
			// TODO Auto-generated constructor stub
			super(MainActivity.this, R.layout.row, R.id.file, names);
			Log.i("myAdapter", "lines: " + lines.length);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View row = super.getView(position, convertView, parent);

			String state = lines[position].getState();

			TextView info = (TextView) row.findViewById(R.id.info);

			info.setText("" + lines[position].getInfo());

			ImageView icon = (ImageView) row.findViewById(R.id.icon);

			if ("pausedUP".equals(state)) {
				icon.setImageResource(R.drawable.paused);
			}

			if ("pausedDL".equals(state)) {
				icon.setImageResource(R.drawable.paused);
			}

			if ("stalledUP".equals(state)) {
				icon.setImageResource(R.drawable.stalledup);
			}

			if ("stalledDL".equals(state)) {
				icon.setImageResource(R.drawable.stalleddl);
			}

			if ("downloading".equals(state)) {
				icon.setImageResource(R.drawable.downloading);
			}

			if ("uploading".equals(state)) {
				icon.setImageResource(R.drawable.uploading);
			}

			return (row);
		}
	}

	// Drawer classes

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
		}

	}

	private void selectItem(int position) {

		// Fragment fragment = null;

		switch (position) {
		case 0:
			refresh("all");
			break;
		case 1:
			refresh("downloading");
			break;
		case 2:
			refresh("completed");
			break;			
		case 3:
			refresh("paused");
			break;
		case 4:
			refresh("active");
			break;
		case 5:
			refresh("inactive");
			break;
		case 6:
			// Settings
			openPreferences();
			break;
		default:
			break;
		}

		// if (fragment != null || listFragment != null || contentFragment !=
		// null) {
		// // FragmentManager fragmentManager = getFragmentManager();
		// // fragmentManager.beginTransaction()
		// // .replace(R.id.content_frame, fragment).commit();

		drawerList.setItemChecked(position, true);
		drawerList.setSelection(position);
		setTitle(navigationDrawerItemTitles[position]);
		drawerLayout.closeDrawer(drawerList);

		// } else {
		// Log.e("MainActivity", "Error in creating fragment");
		// }
	}
}
