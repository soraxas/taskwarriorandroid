package soraxas.taskw.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import soraxas.taskw.App;
import soraxas.taskw.R;
import soraxas.taskw.common.Helpers;
import soraxas.taskw.data.AccountController;
import soraxas.taskw.data.Controller;
import soraxas.taskw.data.ReportInfo;


public class WidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private Intent intent;
    private AccountController ac;
    private ReportInfo info;
    List<JSONObject> tasklist;


    //For obtaining the activity's context and intent
    public WidgetDataProvider(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    private void initController() {

        Controller controller = App.controller();
        ac = controller.accountController(controller.currentAccount(), true);
        info = ac.taskReportInfo("next", "");

        tasklist = ac.taskList(info.query);
        info.sort(tasklist); // Sorted according to report spec.

//        final long identityToken = Binder.clearCallingIdentity();
//        /**This is done because the widget runs as a separate thread
//         when compared to the current app and hence the app's data won't be accessible to it
//         because I'm using a content provided **/
//
//        Binder.restoreCallingIdentity(identityToken);
    }

    @Override
    public void onCreate() {
        initController();
    }

    @Override
    public void onDataSetChanged() {
        /** Listen for data changes and initialize the cursor again **/
        initController();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return tasklist.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        /** Populate your widget's single list item **/
        JSONObject obj = tasklist.get(i);

        String due = null;

        try {
            due = (String) obj.get("due");
        } catch (JSONException e) {
        }
        RemoteViews remoteViews;

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_task_report_item);
        remoteViews.removeAllViews(R.id.widget_item_container);

        RemoteViews task_desc = new RemoteViews(context.getPackageName(), R.layout.item_simple_textview);
        remoteViews.addView(R.id.widget_item_container, task_desc);

        // if the item has due date, add date label
        if (due != null) {
            // dynamically add the date label
            RemoteViews date_label = new RemoteViews(context.getPackageName(), R.layout.item_one_label_left);
            remoteViews.addView(R.id.widget_item_container, date_label);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            remoteViews.setTextViewText(R.id.label_text, Helpers.asDate(due, true,
                    sharedPref));
        }

        try {
            remoteViews.setTextViewText(R.id.widget_item_desc, (String) obj.get("description"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}