package soraxas.taskw

import android.content.Intent
import android.widget.RemoteViewsService
import soraxas.taskw.ui.WidgetDataProvider

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetDataProvider(this, intent)
    }

    override fun onCreate() {
//        startForeground(1, new Notification(1, "haha", 10000));
    }
}