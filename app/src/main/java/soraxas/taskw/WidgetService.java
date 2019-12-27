package soraxas.taskw;

import android.app.Notification;
import android.content.Intent;
import android.widget.RemoteViewsService;

import soraxas.taskw.ui.WidgetDataProvider;

public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetDataProvider(this,intent);
    }

    @Override
    public void onCreate(){
        startForeground(1, new Notification(1, "haha", 10000));
    }
}
