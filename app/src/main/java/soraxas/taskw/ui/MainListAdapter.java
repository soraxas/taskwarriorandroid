package soraxas.taskw.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

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
import soraxas.taskw.utils.DateConverter;

import static soraxas.taskw.ui.TaskDetailActivityKt.showD;

/**
 * Created by vorobyev on 11/19/15.
 */
public class MainListAdapter extends RecyclerView.Adapter<MainListAdapter.ListViewHolder> {

    private final int lastMargin;
    private int urgMin;
    private int urgMax;
    private Accessor<JSONObject, String> uuidAcc = new Accessor<JSONObject, String>() {
        @Override
        public String get(JSONObject object) {
            return object.optString("uuid");
        }
    };

    public MainListAdapter(Resources resources) {
        lastMargin = (int) resources.getDimension(R.dimen.last_task_margin);
    }

    public interface ItemListener {
        public void onEdit(JSONObject json);

        public void onStatus(JSONObject json);

        public void onDelete(JSONObject json);

        public void onAnnotate(JSONObject json);

        public void onStartStop(JSONObject json);

        public void onDenotate(JSONObject json, JSONObject annJson);

        public void onCopyText(JSONObject json, String text);

        public void onLabelClick(JSONObject json, String type, boolean longClick);
    }

    List<JSONObject> data = new ArrayList<>();
    static Logger logger = Logger.forClass(MainListAdapter.class);
    private ReportInfo info = null;
    private ItemListener listener = null;

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void bindLongCopyText(final JSONObject json, View view, final String text) {
        if (TextUtils.isEmpty(text) || view == null || json == null) {
            return;
        }
        view.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        logger.d("Long click on description", json);
                        if (null != listener) listener.onCopyText(json, text);
                        return true;
                    }
                });
    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_one_task);

        // create view and viewHolder
        ListViewHolder viewHolder = new ListViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_one_card, parent, false), views);

        // set all as initially gone
        views.setViewVisibility(R.id.task_urgency, View.GONE);
        views.setViewVisibility(R.id.task_priority, View.GONE);
        views.setViewVisibility(R.id.task_annotations_flag, View.GONE);

        views.removeAllViews(R.id.task_labels_left);
        views.removeAllViews(R.id.task_labels_right);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ListViewHolder holder, int position) {
        // load data
        boolean last = getItemCount() - 1 == position;
        holder.itemView.setPadding(0, 0, 0, last ? lastMargin : 0);
        final JSONObject json = data.get(position);

        Context context = holder.itemView.getContext();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        // reset all as gone

//        final TextView desc = holder.card.findViewById(R.id.card_card);
//
//        desc.setClickable(false);
//        desc.setFocusable(false);


//        holder.itemView.findViewById(R.id.task_description).setOnClickListener(new View.OnClickListener() {
////        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Context context = holder.itemView.getContext();
//
//                View taskDetailView = inflateTaskDetailView(json, context);
//                bindTaskDetailView(taskDetailView, json);
//                showD(context, taskDetailView);
//            }
//        });


//        holder.itemView.findViewById(R.id.task_view_inner).setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                // run action on release
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//
//                    Context context = holder.itemView.getContext();
//
//                    View taskDetailView = inflateTaskDetailView(json, context);
//                    bindTaskDetailView(taskDetailView, json);
//                    showD(context, taskDetailView);
//                }
//                // ignore all touch events
//                return true;
//            }
//        });

        holder.itemView.findViewById(R.id.task_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = holder.itemView.getContext();

                View taskDetailView = inflateTaskDetailView(json, context);
                bindTaskDetailView(taskDetailView, json);
                showD(context, taskDetailView);
            }
        });


        // color card that has been started
        if (info.fields.containsKey("start")) {
            String status = json.optString("status", "pending");
            if ("pending".equalsIgnoreCase(status)) {
                boolean isStarted = !TextUtils.isEmpty(json.optString("start"));
                // this has started and is pending
                if (isStarted) {
                    holder.card.setBackgroundResource(R.color.DarkCyan);
                }
            }
        }


        View views = holder.itemView;

        // remove previous data
//        views.findViewById(R.id.task_urgency).setVisibility(View.GONE);
//        views.findViewById(R.id.task_priority).setVisibility(View.GONE);
//        views.findViewById(R.id.task_annotations_flag).setVisibility(View.GONE);
        boolean hasAnno = json.optJSONArray("annotations") != null;
        views.findViewById(R.id.task_annotations_flag).setVisibility(hasAnno ? View.VISIBLE : View.GONE);

        ((ViewGroup) views.findViewById(R.id.task_labels_left)).removeAllViews();
        ((ViewGroup) views.findViewById(R.id.task_labels_right)).removeAllViews();

        boolean hasBottomLabels = false;
        // update view with data
//        for (Map.Entry<String, String> field : info.fields.entrySet()) {
//            switch (field.getKey().toLowerCase()) {
//                case "description":
//                    // Set desc
//                    TextView desc = views.findViewById(R.id.task_description);
//                    desc.setText(json.optString("description"));
//                    desc.setClickable(false);
//                    desc.setFocusable(false);
//                    break;
//                case "priority":
//                    int index = info.priorities.indexOf(json.optString("priority", ""));
//                    if (index == -1) {
//                        ProgressBar pb = views.findViewById(R.id.task_priority);
//                        pb.setMax(0);
//                        pb.setProgress(0);
//                    } else {
//                        ProgressBar pb = views.findViewById(R.id.task_priority);
//                        pb.setMax(info.priorities.size() - 1);
//                        pb.setProgress(info.priorities.size() - index - 1);
//                    }
//                    break;
//                case "urgency":
//                    ProgressBar pb = views.findViewById(R.id.task_urgency);
//                    pb.setMax(urgMax - urgMin);
//                    pb.setProgress((int) Math.round(json.optDouble("urgency")) - urgMin);
//                    break;
//                case "due":
//                    addLabel(context, views, "due", true, R.drawable.ic_label_due,
//                            asDate(json.optString("due"), sharedPref));
//                    hasBottomLabels = true;
//                    break;
//                case "wait":
//                    addLabel(context, views, "wait", true, R.drawable.ic_label_wait,
//                            asDate(json.optString("wait"), sharedPref));
//                    hasBottomLabels = true;
//                    break;
//                case "scheduled":
//                    addLabel(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
//                            asDate(json.optString("scheduled"), sharedPref));
//                    hasBottomLabels = true;
//                    break;
//                case "recur":
//                    String recur = json.optString("recur");
//                    if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
//                        String until = asDate(json.optString("until"), sharedPref);
//                        if (!TextUtils.isEmpty(until)) {
//                            recur += String.format(" ~ %s", until);
//                        }
//                    }
//                    addLabel(context, views, "recur", true, R.drawable.ic_label_recur, recur);
//                    hasBottomLabels = true;
//                    break;
//                case "project":
//                    addLabel(context, views, "project", false, R.drawable.ic_label_project,
//                            json.optString("project"));
//                    hasBottomLabels = true;
//                    break;
//                case "tags":
//                    addLabel(context, views, "tags", false, R.drawable.ic_label_tags, join(", ", array2List(
//                            json.optJSONArray("tags"))));
//                    hasBottomLabels = true;
//                    break;
//            }
//        }


        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String k = it.next();
            switch (k) {
                case "description":
                    // Set desc
                    TextView desc = views.findViewById(R.id.task_description);
                    desc.setText(json.optString("description"));
                    break;
                case "priority":
                    int index = info.priorities.indexOf(json.optString("priority", ""));
                    if (index == -1) {
                        ProgressBar pb = views.findViewById(R.id.task_priority);
                        pb.setMax(0);
                        pb.setProgress(0);
                    } else {
                        ProgressBar pb = views.findViewById(R.id.task_priority);
                        pb.setMax(info.priorities.size() - 1);
                        pb.setProgress(info.priorities.size() - index - 1);
                    }
                    break;
                case "urgency":
                    ProgressBar pb = views.findViewById(R.id.task_urgency);
                    pb.setMax(urgMax - urgMin);
                    pb.setProgress((int) Math.round(json.optDouble("urgency")) - urgMin);
                    break;
                case "due":
                    addLabel(context, views, "due", true, R.drawable.ic_label_due,
                            asDate(json.optString("due"), sharedPref));
                    hasBottomLabels = true;
                    break;
                case "wait":
                    addLabel(context, views, "wait", true, R.drawable.ic_label_wait,
                            asDate(json.optString("wait"), sharedPref));
                    hasBottomLabels = true;
                    break;
                case "scheduled":
                    addLabel(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
                            asDate(json.optString("scheduled"), sharedPref));
                    hasBottomLabels = true;
                    break;
                case "recur":
                    String recur = json.optString("recur");
                    if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
                        String until = asDate(json.optString("until"), sharedPref);
                        if (!TextUtils.isEmpty(until)) {
                            recur += String.format(" ~ %s", until);
                        }
                    }
                    addLabel(context, views, "recur", true, R.drawable.ic_label_recur, recur);
                    hasBottomLabels = true;
                    break;
                case "project":
                    addLabel(context, views, "project", false, R.drawable.ic_label_project,
                            json.optString("project"));
                    hasBottomLabels = true;
                    break;
                case "tags":
                    addLabel(context, views, "tags", false, R.drawable.ic_label_tags, join(", ", array2List(
                            json.optJSONArray("tags"))));
                    hasBottomLabels = true;
                    break;
            }
        }

        if (!hasBottomLabels){
            // make label match parent
            views.findViewById(R.id.task_description_outer).setLayoutParams(
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }


    private void bindTaskDetailView(View view, final JSONObject json) {
        final ViewGroup annotations = (ViewGroup) view.findViewById(R.id.task_annotations);

        bindLongCopyText(json, view.findViewById(R.id.task_description), json.optString("description"));
        view.findViewById(R.id.task_edit_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != listener)
                    listener.onEdit(json);
            }
        });
        view.findViewById(R.id.task_status_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != listener)
                            listener.onStatus(json);
                    }
                });
        view.findViewById(R.id.task_delete_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != listener)
                            listener.onDelete(json);
                    }
                });
        view.findViewById(R.id.task_annotate_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != listener)
                            listener.onAnnotate(json);
                    }
                });
        view.findViewById(R.id.task_start_stop_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (null != listener)
                            listener.onStartStop(json);
                    }
                });
        JSONArray annotationsArr = json.optJSONArray("annotations");
        if (null != annotationsArr && annotationsArr.length() == annotations.getChildCount()) {
            // Bind delete button
            for (int i = 0; i < annotationsArr.length(); i++) { // Show and bind delete button
                JSONObject jsonAnn = annotationsArr.optJSONObject(i);
                bindLongCopyText(json,
                        annotations.getChildAt(i).findViewById(R.id.task_ann_text),
                        jsonAnn.optString("description"));
                View deleteBtn = annotations.getChildAt(i).findViewById(R.id.task_ann_delete_btn);
                if (null != deleteBtn) {
                    deleteBtn.setVisibility(View.VISIBLE);
                    deleteBtn.setOnClickListener(denotate(json, jsonAnn));
                }
            }
        }

    }

    private View inflateTaskDetailView(final JSONObject json, Context context) {

        View views = View.inflate(context, R.layout.item_one_task_detail, null);


        // bottom buttons

//        boolean last = getItemCount() - 1 == position;
//        holder.itemView.setPadding(0, 0, 0, last ? lastMargin : 0);
//        final JSONObject json = data.get(position);
//        holder.card.removeAllViews();
//        TaskView card = fill(holder.itemView.getContext(), json, info, urgMin, urgMax);
//        holder.card.addView(card.remoteView.apply(holder.itemView.getContext(), holder.card));
//        setupLabelListeners(holder.itemView.getContext(), json,
//                (ViewGroup) holder.card.findViewById(R.id.task_labels_left), card.leftColumn);
//        setupLabelListeners(holder.itemView.getContext(), json,
//                (ViewGroup) holder.card.findViewById(R.id.task_labels_right), card.rightColumn);
//        View bottomBtns = views.findViewById(R.id.task_bottom_btns);
////        ViewGroup annotations2 = views.findViewById(R.id.task_annotations);
//        View id = views.findViewById(R.id.task_id);
//        bottomBtns.setVisibility(View.GONE);
//
//        bottomBtns.setVisibility(View.VISIBLE);
////        annotations2.setVisibility(View.VISIBLE);
//        id.setVisibility(View.VISIBLE);


        ///////////////////////////


        String status = json.optString("status", "pending");


        ((ImageView) views.findViewById(R.id.task_status_btn)).setImageResource(status2icon(status));

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        for (Map.Entry<String, String> field : info.fields.entrySet()) {
            switch (field.getKey().toLowerCase()) {
                case "description":
                    // Set desc
                    TextView desc = views.findViewById(R.id.task_description);
                    desc.setText(json.optString("description"));

                    JSONArray annotations = json.optJSONArray("annotations");
                    if (null != annotations && annotations.length() > 0) {
                        // Have annotations

                        if ("".equals(field.getValue())) {
                            for (int i = 0; i < annotations.length(); i++) {
                                JSONObject ann = annotations.optJSONObject(i);

                                View annView = View.inflate(context, R.layout.item_one_annotation, null);
                                ((TextView) annView.findViewById(R.id.task_ann_text)).setText(ann.optString("description", "Untitled"));
                                ((TextView) annView.findViewById(R.id.task_ann_date)).setText(asDate(ann.optString("entry"), sharedPref));

                                ViewGroup insertPoint = views.findViewById(R.id.task_annotations);
                                insertPoint.addView(annView, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
                            }
                        }
                    }
                    break;
                case "id":
                    ((TextView) views.findViewById(R.id.task_id)).setText(String.format("[%d]", json.optInt("id", -1)));
                    break;
                case "priority":
                    int index = info.priorities.indexOf(json.optString("priority", ""));
                    if (index == -1) {
                        ProgressBar pb = views.findViewById(R.id.task_priority);
                        pb.setMax(0);
                        pb.setProgress(0);
                    } else {
                        ProgressBar pb = views.findViewById(R.id.task_priority);
                        pb.setMax(info.priorities.size() - 1);
                        pb.setProgress(info.priorities.size() - index - 1);
                    }
                    break;
                case "urgency":
                    ProgressBar pb = views.findViewById(R.id.task_urgency);
                    pb.setMax(urgMax - urgMin);
                    pb.setProgress((int) Math.round(json.optDouble("urgency")) - urgMin);
                    break;
                case "due":
                    addLabel(context, views, "due", true, R.drawable.ic_label_due,
                            asDate(json.optString("due"), sharedPref));
                    break;
                case "wait":
                    addLabel(context, views, "wait", true, R.drawable.ic_label_wait,
                            asDate(json.optString("wait"), sharedPref));
                    break;
                case "scheduled":
                    addLabel(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
                            asDate(json.optString("scheduled"), sharedPref));
                    break;
                case "recur":
                    String recur = json.optString("recur");
                    if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
                        String until = asDate(json.optString("until"), sharedPref);
                        if (!TextUtils.isEmpty(until)) {
                            recur += String.format(" ~ %s", until);
                        }
                    }
                    addLabel(context, views, "recur", true, R.drawable.ic_label_recur, recur);
                    break;
                case "project":
                    addLabel(context, views, "project", false, R.drawable.ic_label_project,
                            json.optString("project"));
                    break;
                case "tags":
                    addLabel(context, views, "tags", false, R.drawable.ic_label_tags, join(", ", array2List(
                            json.optJSONArray("tags"))));
                    break;
                case "start":
                    String started = asDate(json.optString("start"), sharedPref);
                    boolean isStarted = !TextUtils.isEmpty(started);

                    if ("pending".equalsIgnoreCase(status)) { // Can be started/stopped
                        views.findViewById(R.id.task_start_stop_btn).setVisibility(View.VISIBLE);
                        ((ImageView) views.findViewById(R.id.task_start_stop_btn)).setImageResource(isStarted ? R.drawable.ic_action_stop : R.drawable.ic_action_start);
                    }
                    break;
            }

        }
        return views;
    }


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

    private View.OnClickListener denotate(final JSONObject json, final JSONObject annJson) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != listener) {
                    listener.onDenotate(json, annJson);
                }
            }
        };
    }

    interface Accessor<O, V> {
        V get(O object);
    }

    private static <O, V> int indexOf(Collection<O> from, Accessor<O, V> acc, V value) {
        int index = 0;
        for (O item : from) { // $COMMENT
            if (value.equals(acc.get(item))) { // Found
                return index;
            }
            index++;
        }
        return -1;
    }

    public <O, V> void morph(List<O> from, List<O> to, Accessor<O, V> acc) {
        for (int i = 0; i < to.size(); ) {
            O item = to.get(i);
            V id = acc.get(item);
            boolean remove = indexOf(from, acc, id) == -1; // Item not found in new array
            if (remove) { //
                notifyItemRemoved(i);
                to.remove(i);
            } else {
                i++;
            }
        }
        for (int i = 0; i < from.size(); i++) {
            O item = from.get(i);
            V id = acc.get(item);
            int idx = indexOf(to, acc, id); // Location in old array
            if (idx == -1) { // Add item
                notifyItemInserted(i);
                to.add(i, item);
            } else {
                notifyItemMoved(idx, i);
                to.remove(idx);
                to.add(i, item);
                notifyItemChanged(i);
            }
        }
    }

    public void update(List<JSONObject> list, ReportInfo info) {
        this.info = info;
        boolean hasUrgency = info.fields.containsKey("urgency");
        if (hasUrgency && !list.isEmpty()) { // Search
            double min = list.get(0).optDouble("urgency");
            double max = min;
            for (JSONObject json : list) { // Find min and max
                double urg = json.optDouble("urgency");
                if (min > urg) {
                    min = urg;
                }
                if (max < urg) {
                    max = urg;
                }
            }
            urgMin = (int) Math.floor(min);
            urgMax = (int) Math.ceil(max);
        }
        morph(list, data, uuidAcc);
//        data.clear();
//        data.addAll(list);
//        notifyDataSetChanged();
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

    public static DateFormat formattedFormat = new SimpleDateFormat("yyyy-MM-dd");
    public static DateFormat formattedFormatDT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static DateFormat formattedISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");


    private static int status2icon(String status) {
        if ("deleted".equalsIgnoreCase(status)) return R.drawable.ic_status_deleted;
        if ("completed".equalsIgnoreCase(status)) return R.drawable.ic_status_completed;
        if ("waiting".equalsIgnoreCase(status)) return R.drawable.ic_status_waiting;
        if ("recurring".equalsIgnoreCase(status)) return R.drawable.ic_status_recurring;
        return R.drawable.ic_status_pending;
    }

    private static void addLabel(Context context, TaskView view, String code, boolean left, int icon, String text) {
        if (TextUtils.isEmpty(text)) { // No label
            return;
        }
        RemoteViews line = new RemoteViews(context.getPackageName(), left ?
                R.layout.item_one_label_left :
                R.layout.item_one_label_right);
        line.setTextViewText(R.id.label_text, text);
        line.setImageViewResource(R.id.label_icon, icon);
        view.remoteView.addView(left ? R.id.task_labels_left : R.id.task_labels_right, line);
        (left ? view.leftColumn : view.rightColumn).add(code);
    }

    private static void addLabel(Context context, RemoteViews view, String code, boolean left, int icon, String text) {
        if (TextUtils.isEmpty(text)) { // No label
            return;
        }
        RemoteViews line = new RemoteViews(context.getPackageName(), left ?
                R.layout.item_one_label_left :
                R.layout.item_one_label_right);
        line.setTextViewText(R.id.label_text, text);
        line.setImageViewResource(R.id.label_icon, icon);
        view.addView(left ? R.id.task_labels_left : R.id.task_labels_right, line);
//        (left ? view.leftColumn : view.rightColumn).add(code);
    }

    private static void addLabel(Context context, View view, String code, boolean left, int icon, String text) {
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

    public void listener(ItemListener listener) {
        this.listener = listener;
    }

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
