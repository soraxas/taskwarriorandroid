package org.kvj.bravo7;

import android.app.Activity;
import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

abstract public class ApplicationContext extends Application {

	public class LogEntry {
		String entry;
		Date date = new Date();

		public LogEntry(String entry) {
			this.entry = entry;
		}

		public Date getDate() {
			return date;
		}

		public String getText() {
			return entry;
		}
	}

	public static final String PREF_NAME = "prefs";
	private static final String TAG = "ApplicationContext";

	private SharedPreferences preferences = null;
	private Map<String, Object> registry = new HashMap<String, Object>();
	private static final int MAX_LOG = 100;
	private Queue<LogEntry> log = new LinkedList<ApplicationContext.LogEntry>();
	private boolean initDone = false;

	public ApplicationContext() {
		super();
		// preferences =
		// PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		instance = this;
	}

	private static ApplicationContext instance = null;

	public static ApplicationContext getInstance() {
		if (!instance.initDone) {
			instance.initDone = true;
			instance.init();
		}
		return instance;
	}

	abstract protected void init();

	public SharedPreferences getPreferences() {
		if (null == preferences) {
			preferences = PreferenceManager.getDefaultSharedPreferences(this);
		}
		return preferences;
	}

	public String getStringPreference(String name, String defaultValue) {
		return getPreferences().getString(name, defaultValue);
	}

	public String getStringPreference(int name, int defaultValue) {
		return getPreferences().getString(getString(name), getString(defaultValue));
	}

	public void setStringPreference(String name, String value) {
		getPreferences().edit().putString(name, value).commit();
	}

	public List<String> getStringArrayPreference(String name) {
		List<String> result = new ArrayList<String>();
		String ids = getStringPreference(name, "");
		String[] arr = ids.split(" ");
		for (String id : arr) {
			if (id != null && !"".equals(id)) {
				result.add(id);
			}
		}
		return result;
	}

	public List<String> setStringArrayPreference(String name, String id, boolean add) {
		return setStringArrayPreference(name, id, add, -1);
	}

	public List<String> setStringArrayPreference(String name, List<String> ids) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0) {
				builder.append(" ");
			}
			builder.append(ids.get(i));
		}
		setStringPreference(name, builder.toString());
		return ids;
	}

	public List<String> setStringArrayPreference(String name, String id, boolean add, int index) {
		List<String> result = getStringArrayPreference(name);
		if (id == null || "".equals(id)) {
			return result;
		}
		for (int i = 0; i < result.size(); i++) {
			if (id.equals(result.get(i))) {
				if (add) {
					return result;// Already here
				} else {
					result.remove(i);
					i--;
				}
			}
		}
		if (add) {
			if (-1 == index) {
				result.add(id);
			} else {
				result.add(index, id);
			}
		}
		return setStringArrayPreference(name, result);
	}

	public int getIntPreference(String name, int defaultID) {
		try {
			return Integer.parseInt(getPreferences().getString(name, getString(defaultID)));
		} catch (Exception e) {
			try {
				return Integer.parseInt(getString(defaultID));
			} catch (Exception e2) {
			}
		}
		return -1;
	}

	public int getIntPreference(int name, int defaultID) {
		return getIntPreference(getString(name), defaultID);
	}

	public void setIntPreference(String name, int value) {
		getPreferences().edit().putString(name, Integer.toString(value)).commit();
	}

	public void setWidgetConfig(int id, String name) {
		setStringPreference("widget_" + id, name);
	}

	public SharedPreferences getWidgetConfig(int id, String name) {
		if (null != getStringPreference("widget_" + id, null)) {
			return getSharedPreferences("widget_" + id, Context.MODE_PRIVATE);
		}
		if (null != name) {
			setWidgetConfig(id, name);
			return getSharedPreferences("widget_" + id, Context.MODE_PRIVATE);
		}
		return null;
	}

	public SharedPreferences getWidgetConfig(int id) {
		return getWidgetConfig(id, null);
	}

	public Map<Integer, String> getWidgetConfigs(String provider) {
		Map<Integer, String> result = new LinkedHashMap<Integer, String>();
		Set<String> keys = getPreferences().getAll().keySet();
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		for (String key : keys) {
			if (key.startsWith("widget_")) {
				try {
					String name = getStringPreference(key, "");
					int widgetID = Integer.parseInt(key.substring("widget_".length()));
					AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetID);
					if (info == null) {
						Log.w(TAG, "updateWidgets no info or config for " + widgetID);
						continue;
					}
					if (provider != null && !info.provider.getClassName().equals(provider)) {
						continue;
					}
					result.put(widgetID, name);
				} catch (Exception e) {
					Log.e(TAG, "getWidgetConfigs error:", e);
				}
			}
		}
		return result;
	}

	public void updateWidgets(int id) {
		Set<String> keys = getPreferences().getAll().keySet();
		List<String> toRemove = new ArrayList<String>();
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		for (String key : keys) {
			if (key.startsWith("widget_")) {
				try {
					// Log.i(TAG, "updateWidgets: key: "+key);
					// String type = getStringPreference(key, "");
					int widgetID = Integer.parseInt(key.substring("widget_".length()));
					AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetID);
					if (info == null) {
						Log.w(TAG, "updateWidgets no info for " + widgetID);
						toRemove.add(key);
						continue;
					}
					if (id != -1 && id != widgetID) {
						// Log.w(TAG, "updateWidgets not a requested ID");
						continue;
					}
					AppWidgetProvider provider = (AppWidgetProvider) getClass().getClassLoader()
							.loadClass(info.provider.getClassName()).newInstance();
					// Log.i(TAG, "updateWidgets calling update...");
					provider.onUpdate(this, manager, new int[] { widgetID });
				} catch (Exception e) {
					Log.e(TAG, "updateWidgets error:", e);
					toRemove.add(key);
				}
			}
		}
		Editor editor = getPreferences().edit();
		for (String key : toRemove) {
			editor.remove(key);
		}
		editor.commit();
	}

	public void publishBean(Object object) {
		registry.put(object.getClass().getName(), object);
	}

	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> cl) {
		return (T) registry.get(cl.getName());
	}

	public void publishBean(String name, Object object) {
		registry.put(name, object);
	}

	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, Class<T> cl) {
		return (T) registry.get(name);
	}

	public void log(String entry) {
		synchronized (log) {
			while (log.size() > MAX_LOG) {
				log.poll();
			}
			log.add(new LogEntry(entry));
		}
	}

	public Queue<LogEntry> getLog() {
		return log;
	}

	public Integer getWidgetConfigID(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			int mAppWidgetId = extras
					.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (AppWidgetManager.INVALID_APPWIDGET_ID != mAppWidgetId) {
				return mAppWidgetId;
			}
		}
		return null;
	}

	public void setWidgetConfigDone(Activity activity) {
		Integer widgetID = getWidgetConfigID(activity.getIntent());
		if (null != widgetID) {
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
			activity.setResult(Activity.RESULT_OK, resultValue);
		}
	}

	public boolean getBooleanPreference(int name, boolean def) {
		return getPreferences().getBoolean(getString(name), def);
	}

	public boolean getBooleanPreference(int name, int def) {
		boolean defBool = false;
		try { //
			defBool = Boolean.parseBoolean(getString(def));
		} catch (Exception e) {
		}
		return getBooleanPreference(name, defBool);
	}

	public void setIntPreference(int id, int value) {
		setIntPreference(getString(id), value);
	}

	public void setStringPreference(int name, String value) {
		setStringPreference(getString(name), value);
	}

    public float dp2px(float dp) {
        Resources r = getResources();
        return r.getDisplayMetrics().density * dp;
    }

    public float sp2px(float dp) {
        Resources r = getResources();
        return r.getDisplayMetrics().scaledDensity * dp;
    }

}
