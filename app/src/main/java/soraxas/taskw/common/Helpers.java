package soraxas.taskw.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.kvj.bravo7.log.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import soraxas.taskw.R;
import soraxas.taskw.ui.MainListAdapter;
import soraxas.taskw.utils.DateConverter;

public class Helpers {
    public static int status2icon(String status) {
        if ("deleted".equalsIgnoreCase(status)) return R.drawable.ic_status_deleted;
        if ("completed".equalsIgnoreCase(status)) return R.drawable.ic_status_completed;
        if ("waiting".equalsIgnoreCase(status)) return R.drawable.ic_status_waiting;
        if ("recurring".equalsIgnoreCase(status)) return R.drawable.ic_status_recurring;
        return R.drawable.ic_status_pending;
    }

    public static void addLabel(Context context, View view, String code, boolean left,
                                int icon, String text) {
        if (TextUtils.isEmpty(text)) { // No label
            return;
        }
        View line = View.inflate(context, left ?
                R.layout.item_one_label_left : R.layout.item_one_label_right, null);
        ((TextView) line.findViewById(R.id.label_text)).setText(text);
        ((ImageView) line.findViewById(R.id.label_icon)).setImageResource(icon);

        ViewGroup insertPoint = (ViewGroup) view.findViewById(left ? R.id.task_labels_left : R.id.task_labels_right);
        insertPoint.addView(line, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
    }

    public static String join(String with, Iterable<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String item : list) { // Join
            if (TextUtils.isEmpty(item)) { // Skip all empty
                continue;
            }
            if (sb.length() > 0) { // Not empty
                sb.append(with);
            }
            sb.append(item);
        }
        return sb.toString();
    }

    public static Collection<String> array2List(JSONArray arr) {
        List<String> result = new ArrayList<>();
        if (null == arr) {
            return result;
        }
        for (int i = 0; i < arr.length(); i++) { // $COMMENT
            String value = arr.optString(i);
            if (!TextUtils.isEmpty(value)) { // Only non-empty
                result.add(value);
            }
        }
        return result;
    }

    ////////////////////////////////////////////////////////////

    public static String asDate(String due, boolean forceIgnoreMidnightTime, SharedPreferences sharedPref) {
        return asDate(due, null, forceIgnoreMidnightTime, sharedPref);
    }

    public static String asDate(String due, SharedPreferences sharedPref) {
        return asDate(due, null, false, sharedPref);
    }

    public static String asDate(String due, DateFormat format, SharedPreferences sharedPref) {
        return asDate(due, format, false, sharedPref);
    }

    public static String asDate(String due, DateFormat format, boolean forceIgnoreMidnightTime, SharedPreferences sharedPref) {
        DateFormat jsonFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        jsonFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        if (TextUtils.isEmpty(due)) { // No value
            return null;
        }
        try {
            Date parsed = jsonFormat.parse(due);
            if (null == format) { // Flexible -> date or date/time

                if (sharedPref.getBoolean("useSimpleDatetime", true)) {
                    // This is the datetime format taskwarrior returns
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    int countdown = Integer.parseInt(sharedPref.getString("dateCountDownThreshold",
                            "7"));

                    return DateConverter.convertToString(dateFormat.parse(due), countdown, forceIgnoreMidnightTime);
                }
            }
            return format.format(parsed);
        } catch (Exception e) {
            Logger logger = Logger.forClass(MainListAdapter.class);
            logger.e(e, "Failed to parse Date:", due);
        }
        return null;
    }
}
