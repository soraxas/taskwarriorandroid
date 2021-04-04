package soraxas.taskw.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.log.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import soraxas.taskw.R;
import soraxas.taskw.data.ReportInfo;
import soraxas.taskw.demo_s_longpress.SwipeOnLongPressExampleAdapter;
import soraxas.taskw.utils.DateConverter;

public class MainListAdapter  {

    private int urgMin;
    private int urgMax;

    List<JSONObject> data = new ArrayList<>();
    static Logger logger = Logger.forClass(MainListAdapter.class);
    private ReportInfo info = null;
    private SwipeOnLongPressExampleAdapter.ItemListener listener = null;







    private void setupLabelListeners(final Context context, final JSONObject json, ViewGroup groupView, List<String> column) {
        for (int i = 0; i < column.size(); i++) { // Attach to every item
            if (i >= groupView.getChildCount()) { // Out of bounds
                continue;
            }
            final String data = column.get(i);
            groupView.getChildAt(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != listener) { //
                        listener.onLabelClick(json, data, false);
                    }
                }
            });
            groupView.getChildAt(i).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (null != listener) { //
                        listener.onLabelClick(json, data, true);
                    }
                    return true;
                }
            });
        }
    }


    public static class ListViewHolder extends RecyclerView.ViewHolder {

        private final CardView card;
        private final RemoteViews remoteView;

        public ListViewHolder(final View itemView, RemoteViews rv) {
            super(itemView);
            card = (CardView) itemView.findViewById(R.id.card_card);
            remoteView = rv;
        }
    }

    public static class TaskView {
        public RemoteViews remoteView = null;
        public List<String> leftColumn = new ArrayList<>();
        public List<String> rightColumn = new ArrayList<>();
    }


    public static DateFormat formattedFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static DateFormat formattedFormatDT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static DateFormat formattedISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");



}
