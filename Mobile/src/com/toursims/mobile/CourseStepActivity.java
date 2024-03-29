package com.toursims.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.toursims.mobile.controller.CourseLoader;
import com.toursims.mobile.model.Course;
import com.toursims.mobile.model.kml.Placemark;
import com.toursims.mobile.model.kml.Point;
import com.toursims.mobile.model.places.Road;
import com.toursims.mobile.ui.ToolBox;
import com.toursims.mobile.ui.utils.CustomItemizedOverlay;
import com.toursims.mobile.ui.utils.RoadProvider;

public class CourseStepActivity extends SherlockMapActivity {

	private static final String TAG = LocalizationService.class.getName();
	private static final String PROXIMITY_INTENT = LocalizationService.class
			.getName() + ".PROXIMITY_INTENT";
	private static final String PROXIMITY_RECEIVER = LocalizationService.class
			.getName() + ".PROXIMITY_RECEIVER";
	private static final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 1; // in
																		// Meters
	private static final long MINIMUM_TIME_BETWEEN_UPDATE = 10 * 1000; // in
																		// Milliseconds
	private static final long POINT_RADIUS = 250; // in Meters
	private static final long PROX_ALERT_EXPIRATION = -1;

	private static List<Placemark> placemarks;
	private static List<GeoPoint> bounds;
	private static Course course;
	private static String type;
	private MapController mapController;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private Drawable fdrawable;
	private CustomItemizedOverlay itemizedOverlay;
	private CustomItemizedOverlay itemizedOverlay_currentPoint;
	private CustomItemizedOverlay itemizedOverlay_prev;
	private List<Road> mRoads;
	private MyLocationOverlay myLocationOverlay;
	private MapView mapView;
	private LocationManager locationManager;
	private static int currentPlacemark;
	private static BroadcastReceiver receiverLocalization;
	private int courseId;
	private PendingIntent proximityIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = getSharedPreferences(
				CustomPreferences.PREF_FILE, 0);
		currentPlacemark = settings.getInt(
				CustomPreferences.COURSE_CURRENT_PLACEMARK, -1);
		setContentView(R.layout.coursestep);

		placemarks = getPlaceMarks();

		mapView = (MapView) findViewById(R.id.map);
		mapView.setBuiltInZoomControls(true);
		// mapView.setStreetView(true);
		mapController = mapView.getController();
		// mapController.setZoom(14); // Zoom 1 is world view

		// ActionBarSherlock setup
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setIcon(R.drawable.ic_dialog_map_colored);
		actionBar.setTitle(course.getName());
	}

	@Override
	protected void onPause() {
		Log.d("TAG", "Stop service");
		stopService(new Intent(this, LocalizationService.class));

		try {
			unregisterReceiver(receiverLocalization);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Log.d("TAG", "Start Service");
		Intent i = new Intent(this, LocalizationService.class);
		if (currentPlacemark < placemarks.size()) {
			i.putExtra(Point.LATITUDE, placemarks.get(currentPlacemark)
					.getPoint().getLatitude());
			i.putExtra(Point.LONGITUDE, placemarks.get(currentPlacemark)
					.getPoint().getLongitude());
			i.putExtra(Placemark.NAME, placemarks.get(currentPlacemark)
					.getName());
		}
		startService(i);

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d("TAG", "Stop service");
		stopService(new Intent(this, LocalizationService.class));

		Log.d("TAG", "Start Service");
		Intent i = new Intent(this, LocalizationService.class);
		startService(i);

		Bundle b = getIntent().getExtras();
		if ((b.getBoolean(Course.NEXT_PLACEMARK))
				&& (currentPlacemark < (placemarks.size() - 1))) {
			incrementCurrentPlacemark();
		}

		if (!b.containsKey("NOTIFICATION")) {
			updatePlacemark();
		} else {
			displayNotification();
		}

		updateMap();
		zoomInBounds();

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MINIMUM_TIME_BETWEEN_UPDATE, MINIMUM_DISTANCECHANGE_FOR_UPDATE,
				new MyLocationListener());
	}

	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			MapOverlay mapOverlay = new MapOverlay(mRoads.get(msg.what),
					mapView);
			mapOverlays.add(mapOverlay);
			// listOfOverlays.clear();
			mapView.invalidate();
			// mapController.setZoom(14);
			Log.d("mHandler", "route tracee");
		};
	};

	private InputStream getConnection(String url) {
		InputStream is = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			is = conn.getInputStream();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is;
	}

	private void incrementCurrentPlacemark() {
		Log.d(TAG, "valeur= " + currentPlacemark);
		Placemark pl = placemarks.get(++currentPlacemark);
		while (pl.isRoutePlacemark()) {
			pl = placemarks.get(++currentPlacemark);
		}
		SharedPreferences settings = getSharedPreferences(
				CustomPreferences.PREF_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(CustomPreferences.COURSE_CURRENT_PLACEMARK,
				currentPlacemark);
		editor.commit();
	}

	private void decrementCurrentPlacemark() {
		Placemark pl = placemarks.get(--currentPlacemark);
		while (pl.isRoutePlacemark()) {
			pl = placemarks.get(--currentPlacemark);
		}

	}

	protected List<Placemark> getPlaceMarks() {

		Bundle bundle = getIntent().getExtras();
		String course_url = bundle.getString(Course.URL_EXTRA);

		course = CourseLoader.getInstance().parse(course_url);
		type = course.getType();

		return course.getPlacemarks();
	}

	public void updateMap() {
		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.maps_icon);
		fdrawable = this.getResources()
				.getDrawable(R.drawable.maps_icon_former);
		itemizedOverlay = new CustomItemizedOverlay(drawable,
				CourseStepActivity.this);
		itemizedOverlay_prev = new CustomItemizedOverlay(fdrawable,
				CourseStepActivity.this);
		// itemizedOverlay_currentPoint = new CustomItemizedOverlay(drawable,
		// this);

		mapOverlays.clear();
		/***** load overlays ******/

		String[] formerPoint = null;
		int i = 0;

		for (Placemark placemark : placemarks) {

			String[] lL = placemark.getPoint().getCoordinates().split(",");
			int l = (new Double(Double.parseDouble(lL[1]) * 1000000))
					.intValue();
			int L = (new Double(Double.parseDouble(lL[0]) * 1000000))
					.intValue();
			Log.d(getLocalClassName(),
					String.valueOf(l) + " " + String.valueOf(L));
			GeoPoint point = new GeoPoint(l, L);
			if (bounds == null) {
				bounds = new ArrayList<GeoPoint>();
			}
			bounds.add(point);

			if (i - currentPlacemark >= -1) {
				OverlayItem overlayItem = new OverlayItem(point,
						placemark.getName(), placemark.getDescription());

				if (i == currentPlacemark) {
					Drawable d = this.getResources().getDrawable(
							R.drawable.maps_icon_current);
					itemizedOverlay_currentPoint = new CustomItemizedOverlay(d,
							CourseStepActivity.this);
					itemizedOverlay_currentPoint.addOverlay(overlayItem);
					mapOverlays.add(itemizedOverlay_currentPoint);
				} else if (!placemark.isRoutePlacemark()) {
					if (i > currentPlacemark) {
						itemizedOverlay.addOverlay(overlayItem);
					} else {
						itemizedOverlay_prev.addOverlay(overlayItem);
					}
				}
			}
			i++;

			/***** load routes *****/
			if (formerPoint != null) {
				if (i - currentPlacemark > 0) {
					roadConnectionThread t = new roadConnectionThread() {
						@Override
						public void run() {
							String url = RoadProvider.getUrl(fromLat, fromLon,
									toLat, toLon);
							InputStream is = getConnection(url);
							if (mRoads == null) {
								mRoads = new ArrayList<Road>();
							}
							mRoads.add(RoadProvider.getRoute(is));
							mHandler.sendEmptyMessage(mRoads.size() - 1);
						}
					};
					t.setCoord(formerPoint, lL);
					t.start();

				}
			} else {
				// place user at the beginning of the course
				// mapController.animateTo(point);
			}
			formerPoint = lL;

		}
		if (itemizedOverlay.size() > 0) {
			mapOverlays.add(itemizedOverlay);
		}
		if (itemizedOverlay_prev.size() > 0) {
			mapOverlays.add(itemizedOverlay_prev);
		}
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		myLocationOverlay.enableMyLocation();
		mapOverlays.add(myLocationOverlay);

		// int latFinSpan = (new
		// Double(Double.parseDouble(placemarks.get(placemarks.size()-1).getPoint().getCoordinates().split(",")[1])*
		// 1000000)).intValue();
		// int latdebSpan= (new
		// Double(Double.parseDouble(placemarks.get(0).getPoint().getCoordinates().split(",")[1])*
		// 1000000)).intValue();
		// int lonFinSpan = (new
		// Double(Double.parseDouble(placemarks.get(placemarks.size()-1).getPoint().getCoordinates().split(",")[0])*
		// 1000000)).intValue();
		// int londebspan= (new
		// Double(Double.parseDouble(placemarks.get(0).getPoint().getCoordinates().split(",")[0])*
		// 1000000)).intValue();
		// mapController.zoomToSpan(Math.abs(latFinSpan-latdebSpan)*2,
		// Math.abs(lonFinSpan-londebspan)*2);
		// GeoPoint target = new GeoPoint(Math.round((latFinSpan+latdebSpan)/2),
		// Math.round((lonFinSpan+londebspan)/2));
		// mapController.animateTo(target);

	}

	private void zoomInBounds() {

		int minLat = Integer.MAX_VALUE;
		int minLong = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int maxLong = Integer.MIN_VALUE;

		for (GeoPoint point : bounds) {
			minLat = Math.min(point.getLatitudeE6(), minLat);
			minLong = Math.min(point.getLongitudeE6(), minLong);
			maxLat = Math.max(point.getLatitudeE6(), maxLat);
			maxLong = Math.max(point.getLongitudeE6(), maxLong);
		}

		mapController.zoomToSpan(Math.abs(minLat - maxLat),
				Math.abs(minLong - maxLong));
		mapController.animateTo(new GeoPoint((maxLat + minLat) / 2,
				(maxLong + minLong) / 2));

		bounds.clear();
	}

	public void updatePlacemark() {
		Log.d("updateReceiver", "Receiver Update");
		Log.d("placemark size", "placemark size " + placemarks.size());

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (!placemarks.isEmpty()) {

			updateMap();
			if (currentPlacemark == -1) {
				AlertDialog.Builder dialog = ToolBox.getDialog(this);

				dialog.setTitle(course.getName());
				dialog.setMessage(course.getPresentation());
				dialog.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								updatePlacemark();
								dialog.dismiss();
							}
						});
				dialog.show();

				incrementCurrentPlacemark();
			} else if (currentPlacemark < placemarks.size()) {
				// Present the new objective with its description
				Placemark item = placemarks.get(currentPlacemark);

				showDirection(getCurrentFocus());

				receiverLocalization = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						stopService(new Intent(getApplicationContext(),
								LocalizationService.class));
						try {
							unregisterReceiver(receiverLocalization);
						} catch (Exception e) {
							e.printStackTrace();
						}
						Log.d(PROXIMITY_RECEIVER, "Proximity Alert");
						displayNotification();

					}
				};

				IntentFilter intentFilter = new IntentFilter(PROXIMITY_INTENT);
				registerReceiver(receiverLocalization, intentFilter);

				Intent intent = new Intent(PROXIMITY_INTENT);
				proximityIntent = PendingIntent
						.getBroadcast(this, 0, intent, 0);

				String[] lL = placemarks.get(currentPlacemark).getPoint()
						.getCoordinates().split(",");
				double l = Double.parseDouble(lL[1]);
				double L = Double.parseDouble(lL[0]);

				locationManager.addProximityAlert(
						placemarks.get(currentPlacemark).getPoint()
								.getLatitude(), // the latitude of the central
												// point of the alert region
						placemarks.get(currentPlacemark).getPoint()
								.getLongitude(), // the longitude of the central
													// point of the alert region
						POINT_RADIUS, // the radius of the central point of the
										// alert region, in meters
						PROX_ALERT_EXPIRATION, // time for this proximity alert,
												// in milliseconds, or -1 to
												// indicate no expiration
						proximityIntent // will be used to generate an Intent to
										// fire when entry to or exit from the
										// alert region is detected
						);

				Log.d(PROXIMITY_INTENT,
						"Alert Proximity Set for lat :"
								+ placemarks.get(currentPlacemark).getPoint()
										.getLatitude()
								+ ", long : "
								+ placemarks.get(currentPlacemark).getPoint()
										.getLongitude());

			} else {
				// End of the course
				SharedPreferences settings = getSharedPreferences(
						CustomPreferences.PREF_FILE, 0);
				courseId = settings.getInt(CustomPreferences.COURSE_STARTED_ID,
						-1);
				Log.d(TAG, "courseId = " + courseId);
				CustomPreferences.removeCourseStarted(settings);

				AlertDialog.Builder dialog = ToolBox.getDialog(this);

				dialog.setTitle(R.string.course_finished_title);
				dialog.setMessage(course.getEnd());
				dialog.setPositiveButton(R.string.course_finished_button_ok,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();

								// Launch the Feedback activity and close this
								// one
								Intent intent = new Intent(
										getApplicationContext(),
										FeedbackActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								intent.putExtra(
										FeedbackActivity.FEEDBACK_COURSE_ID,
										courseId);
								intent.putExtra(
										FeedbackActivity.FEEDBACK_COURSE_NAME,
										course.getName());
								startActivity(intent);
								finish();
							}
						});
				dialog.show();
			}
		}
	}

	public void displayNotification() {

		// check for class in foreground
		Context context = getBaseContext();
		ActivityManager am = (ActivityManager) context
				.getSystemService(Activity.ACTIVITY_SERVICE);
		String packageName = am.getRunningTasks(1).get(0).topActivity
				.getPackageName();
		String className = am.getRunningTasks(1).get(0).topActivity
				.getClassName();

		if ((!className.equals(CourseStepActivity.class.getName())) && (!getIntent().getExtras().containsKey("NOTIFICATION"))) {
			// send notification if not in foreground
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			Notification notification = new Notification(
					R.drawable.ic_launcher, placemarks.get(currentPlacemark)
							.getName(), System.currentTimeMillis());
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.defaults |= Notification.DEFAULT_SOUND;
			notification.defaults |= Notification.DEFAULT_VIBRATE;
			notification.number += 1;

			Intent i = new Intent(getBaseContext(), CourseStepActivity.class);
			i.putExtra(Course.ID_EXTRA, course.getId());
			i.putExtra(Course.URL_EXTRA, course.getUrl());
			i.putExtra(CustomPreferences.COURSE_CURRENT_PLACEMARK,
					currentPlacemark);
			i.putExtra("NOTIFICATION", true);

			PendingIntent activity = PendingIntent.getActivity(
					getBaseContext(), 0, i, 0);
			notification.setLatestEventInfo(getBaseContext(),
					placemarks.get(currentPlacemark).getName(),
					placemarks.get(currentPlacemark).getName(), activity);
			notificationManager.notify(0, notification);
			try {
				locationManager.removeProximityAlert(proximityIntent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			AlertDialog.Builder dialog = ToolBox.getDialog(this);

			if (placemarks.get(currentPlacemark).getQuestions() != null) {
				dialog.setTitle(placemarks.get(currentPlacemark).getName());
				dialog.setMessage(placemarks.get(currentPlacemark)
						.getGreetings());
				dialog.setPositiveButton(R.string.game_play,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
								Intent gameActivity = new Intent(
										getApplicationContext(),
										GameActivity.class);
								gameActivity.putExtra(Course.URL_EXTRA,
										course.getUrl());
								gameActivity.putExtra(Course.CURRENT_PLACEMARK,
										currentPlacemark);
								startActivity(gameActivity);
								dialog.dismiss();
							}
						});
			} else {
				dialog.setTitle(placemarks.get(currentPlacemark).getName());
				dialog.setMessage(placemarks.get(currentPlacemark)
						.getDescription());
				dialog.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int which) {
								if (!getIntent().getExtras().containsKey(Course.NEXT_PLACEMARK)) {
									currentPlacemark++;
								}
								updatePlacemark();
								dialog.dismiss();
							}
						});

			}

			dialog.show();
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.coursestep, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Intent intent;

		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			pauseGame(item.getActionView());
			intent = new Intent(this, HomeActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		case R.id.coursestep_menuItem_previous:
			previousPlacemark(item.getActionView());
			return true;
			// case R.id.coursestep_menuItem_pause:
			// pauseGame(item.getActionView());
			// return true;
		case R.id.coursestep_menuItem_next:
			nextPlacemark(item.getActionView());
			return true;
		case R.id.coursestep_menuItem_direction:
			showDirection(item.getActionView());
			return true;
		case R.id.coursestep_menuItem_help:
			showHelp(item.getActionView());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void nextPlacemark(View view) {
		if (currentPlacemark < placemarks.size() - 1) {
			incrementCurrentPlacemark();
			updatePlacemark();
		}
	}

	public void previousPlacemark(View view) {
		if (currentPlacemark > 0) {
			decrementCurrentPlacemark();
			updatePlacemark();
		}
	}

	public void pauseGame(View view) {
		Log.d("pause", "pause");
	}

	public void showDirection(View view) {
		AlertDialog.Builder dialog = ToolBox.getDialog(this);

		dialog.setTitle(placemarks.get(currentPlacemark).getName());
		dialog.setMessage(placemarks.get(currentPlacemark).getDirection());
		dialog.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		dialog.show();
	}

	public void showHelp(View view) {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.help_dialog);
		dialog.setTitle("Aide");
		dialog.setCancelable(true);

		Button button = (Button) dialog.findViewById(R.id.ButtonOK);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();
	}

}

class MapOverlay extends com.google.android.maps.Overlay {
	Road mRoad;
	ArrayList<GeoPoint> mPoints;

	public MapOverlay(Road road, MapView mv) {
		mRoad = road;
		if (road.mRoute.length > 0) {
			mPoints = new ArrayList<GeoPoint>();
			for (int i = 0; i < road.mRoute.length; i++) {
				mPoints.add(new GeoPoint((int) (road.mRoute[i][1] * 1000000),
						(int) (road.mRoute[i][0] * 1000000)));
			}
			// int moveToLat = (mPoints.get(0).getLatitudeE6() + (mPoints.get(
			// mPoints.size() - 1).getLatitudeE6() - mPoints.get(0)
			// .getLatitudeE6()) / 2);
			// int moveToLong = (mPoints.get(0).getLongitudeE6() + (mPoints.get(
			// mPoints.size() - 1).getLongitudeE6() - mPoints.get(0)
			// .getLongitudeE6()) / 2);
			// GeoPoint moveTo = new GeoPoint(moveToLat, moveToLong);

			// MapController mapController = mv.getController();
			// mapController.animateTo(moveTo);
			// mapController.setZoom(7);
		}

	}

	@Override
	public boolean draw(Canvas canvas, MapView mv, boolean shadow, long when) {
		super.draw(canvas, mv, shadow);
		drawPath(mv, canvas);
		return true;
	}

	public void drawPath(MapView mv, Canvas canvas) {
		int x1 = -1, y1 = -1, x2 = -1, y2 = -1;
		Paint paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(3);
		for (int i = 0; i < mPoints.size(); i++) {
			android.graphics.Point point = new android.graphics.Point();
			mv.getProjection().toPixels(mPoints.get(i), point);
			x2 = point.x;
			y2 = point.y;
			if (i > 0) {
				canvas.drawLine(x1, y1, x2, y2, paint);
			}
			x1 = x2;
			y1 = y2;
		}
	}

}

class roadConnectionThread extends Thread {
	double fromLat, fromLon;
	double toLat, toLon;

	public void setCoord(String[] from, String[] to) {
		this.fromLat = Double.parseDouble(from[1]);
		this.fromLon = Double.parseDouble(from[0]);
		this.toLat = Double.parseDouble(to[1]);
		this.toLon = Double.parseDouble(to[0]);
	}
}

class MyLocationListener implements LocationListener {
	public void onLocationChanged(Location location) {
		Log.d("Location", "New user position : (" + location.getLatitude()
				+ ", " + location.getLongitude() + ")");
	}

	public void onStatusChanged(String s, int i, Bundle b) {
	}

	public void onProviderDisabled(String s) {
	}

	public void onProviderEnabled(String s) {
	}
}
