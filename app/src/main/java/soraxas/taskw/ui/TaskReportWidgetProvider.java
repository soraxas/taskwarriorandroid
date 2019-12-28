package soraxas.taskw.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import soraxas.taskw.R;
import soraxas.taskw.WidgetService;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link TaskReportWidgetConfigureActivity TaskReportWidgetConfigureActivity}
 */
public class TaskReportWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object which defines the view of out widget

        // Instruct the widget manager to update the widget
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.fromParts("content", String.valueOf(appWidgetId), null));


//        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_task_report_layout);
        views.setRemoteAdapter(R.id.widget_listView, intent);



//        /** PendingIntent to launch the MainActivity when the widget was clicked **/
//        Intent launchMain = new Intent(context, MainActivity.class);
//        PendingIntent pendingMainIntent = PendingIntent.getActivity(context, 0, launchMain, 0);
//        views.setOnClickPendingIntent(R.id.widget_listView, pendingMainIntent);


        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,R.id.widget_listView);
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());
//        ComponentName thisWidget = new ComponentName(context.getApplicationContext(), TaskReportWidgetProvider.class);
//        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
//        if (appWidgetIds != null && appWidgetIds.length > 0) {
//            onUpdate(context, appWidgetManager, appWidgetIds);
//        }
//    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            TaskReportWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

