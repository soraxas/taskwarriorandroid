package soraxas.taskw.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;

import org.json.JSONObject;
import org.kvj.bravo7.util.Tasks;

import java.util.List;

import soraxas.taskw.App;
import soraxas.taskw.R;
import soraxas.taskw.WidgetService;
import soraxas.taskw.data.Controller;
import soraxas.taskw.data.ReportInfo;

import static soraxas.taskw.ui.MainListAdapter.logger;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link TaskReportWidgetConfigureActivity TaskReportWidgetConfigureActivity}
 */
public class TaskReportWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

//        CharSequence widgetText = TaskReportWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
//        // Construct the RemoteViews object
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_report_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);
//
//
//        views.setRemoteAdapter(R.id.list_main_list,
//                new Intent(context, WidgetService.class));
//
//
//        Controller controller = App.controller();
//        String account = "2879c1b3-d6f9-4173-8711-980eeeb9a400";
//
//        final String report = "next";
//
//        ArrayAdapter adapter = new ArrayAdapter<JSONObject>(context, R.layout.tasks_report_widget, R.id.list_main_list);
//        ReportInfo info = controller.accountController(account).taskReportInfo(report, "");
//
//        List<JSONObject> list = controller.accountController(account).taskList(info.query);
//        info.sort(list); // Sorted according to report spec.
//
//        adapter.addAll(list);
//
//
//
//        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.tasks_report_widget);
//
//        updateViews.setRemoteAdapter(R.id.list_main_list, );
//
//
////        list = (ListView) updateViews.set
////        list.setLayoutManager(new LinearLayoutManager(getContext()));
////        adapter = new MainListAdapter(getResources());
////        list.setAdapter(adapter);
//
//
//
//
//
//
//
////        adapter.update(result, info);
////
//////        this.account = form.getValue(App.KEY_ACCOUNT);
//////        final String report = form.getValue(App.KEY_REPORT);
//////        final String query = form.getValue(App.KEY_QUERY);
////        new Tasks.ActivitySimpleTask<ReportInfo>(getActivity()){
////
////            @Override
////            protected ReportInfo doInBackground() {
////                logger.d("Load:", query, report);
////                return controller.accountController(account).taskReportInfo(report, query);
////            }
////
////            @Override
////            public void finish(ReportInfo result) {
////                info = result;
////                if (null != afterLoad) afterLoad.run();
////                reload();
////            }
////        }.exec();
//
//
//
//
//        // Instruct the widget manager to update the widget
//        appWidgetManager.updateAppWidget(appWidgetId, views);
//
//
//
//
//
//
//
//


        // Construct the RemoteViews object which defines the view of out widget

        // Instruct the widget manager to update the widget
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.fromParts("content", String.valueOf(appWidgetId), null));


//        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.task_report_widget);
        views.setRemoteAdapter(R.id.widget_listView, intent);



//        /** PendingIntent to launch the MainActivity when the widget was clicked **/
//        Intent launchMain = new Intent(context, MainActivity.class);
//        PendingIntent pendingMainIntent = PendingIntent.getActivity(context, 0, launchMain, 0);
//        views.setOnClickPendingIntent(R.id.widget_listView, pendingMainIntent);


        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,R.id.widget_listView);
        appWidgetManager.updateAppWidget(appWidgetId, views);










    }

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

