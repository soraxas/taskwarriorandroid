package soraxas.taskw.demo_s_longpress;/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemState;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionMoveToSwipedDirection;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.utils.RecyclerViewAdapterUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.log.Logger;

import java.util.List;
import java.util.Map;

import soraxas.taskw.R;
import soraxas.taskw.common.data.TaskwDataProvider;
import soraxas.taskw.data.ReportInfo;

import static soraxas.taskw.common.Helpers.addLabelWithInsert;
import static soraxas.taskw.common.Helpers.array2List;
import static soraxas.taskw.common.Helpers.asDate;
import static soraxas.taskw.common.Helpers.join;
import static soraxas.taskw.common.Helpers.status2icon;
import static soraxas.taskw.common.data.TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_HEADER;
import static soraxas.taskw.common.data.TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_ITEM;
import static soraxas.taskw.ui.TaskDetailActivityKt.showD;


public class SwipeOnLongPressExampleAdapter
        extends RecyclerView.Adapter<SwipeOnLongPressExampleAdapter.MyViewHolder>
        implements SwipeableItemAdapter<SwipeOnLongPressExampleAdapter.MyViewHolder> {
    private static final String TAG = "MySwipeableItemAdapter";

    // NOTE: Make accessible with short name
    private interface Swipeable extends SwipeableItemConstants {
    }

    private TaskwDataProvider mProvider;
    private EventListener mEventListener;
    private View.OnClickListener mItemViewOnClickListener;
    private View.OnClickListener mSwipeableViewContainerOnClickListener;
    public ReportInfo info = null;
    private int urgMin;
    private int urgMax;

    public View cur_taskDetailView;
    public Runnable update_cur_taskDetailView;

    static Logger logger = Logger.forClass(SwipeOnLongPressExampleAdapter.class);
    public ItemListener listener = null;


    public interface EventListener {
        void onItemRemoved(int position);

        void onItemPinned(int position);

        void onItemViewClicked(View v, boolean pinned);
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

    public static class MyViewHolder extends AbstractSwipeableItemViewHolder {
        public FrameLayout mContainer;
        public TextView mTextView;
        public ImageView mAnnoFlag;
        public TextView mStartedFlag;
        public LinearLayout mCalLabelContainer;

        public MyViewHolder(View v) {
            super(v);
            mContainer = v.findViewById(R.id.container);
            mAnnoFlag = v.findViewById(R.id.task_annotations_flag);
            mStartedFlag = v.findViewById(R.id.task_started_flag);
            mTextView = v.findViewById(R.id.task_description);
            mCalLabelContainer = v.findViewById(R.id.cal_label_container);
        }

        @Override
        @NonNull
        public View getSwipeableContainerView() {
            return mContainer;
        }
    }

    public SwipeOnLongPressExampleAdapter(TaskwDataProvider dataProvider) {
        mProvider = dataProvider;
//        mItemViewOnClickListener = v -> onItemViewClick(v);

        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    private JSONObject getJsonWithTaskId(int id) {
        JSONObject json = null;
        for (JSONObject j : mProvider.jsonData) {
            if (j != null && j.optInt("id") == id) {
                json = j;
                break;
            }
        }
        return json;
    }

    private void onItemViewClick(View v, int position, boolean pinned) {
        final JSONObject json = mProvider.jsonData.get(position);
        int task_id = json.optInt("id");


        Context context = v.getContext();

        View taskDetailView = inflateTaskDetailView(json, context);
//        populateTaskDetailView(json, context, taskDetailView);
//        bindTaskDetailView(taskDetailView, json);


        this.cur_taskDetailView = taskDetailView;
        this.update_cur_taskDetailView = () -> {
            // we can reduce the follow to use the json directly. However, this
            // generalise better to subsequent updates
            final JSONObject _json = getJsonWithTaskId(task_id);
            populateTaskDetailView(_json, context,
                    taskDetailView);

        };
        this.update_cur_taskDetailView.run();

        showD(context, taskDetailView);

//        Intent myIntent = new Intent(context, SwipeOnLongPressExampleActivity.class);
////                myIntent.putExtra("key", value); //Optional parameters
//        context.startActivity(myIntent);

    }

    private void onSwipeableViewContainerClick(View v) {
        if (mEventListener != null) {
            mEventListener.onItemViewClicked(RecyclerViewAdapterUtils.getParentViewHolderItemView(v), false);  // false --- not pinned
        }
    }

    @Override
    public long getItemId(int position) {
        return mProvider.getItem(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return mProvider.getItem(position).getViewType();
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

//        final View v = inflater.inflate(R.layout.list_item, parent, false);
        final View v;

        switch (viewType) {
            case ITEM_VIEW_TYPE_SECTION_HEADER:
                v = inflater.inflate(R.layout.list_section_header, parent, false);
                break;
            case ITEM_VIEW_TYPE_SECTION_ITEM:
//                v = inflater.inflate((viewType == 0) ? R.layout.list_item_draggable : R.layout.list_item2_draggable, parent, false);
                v = inflater.inflate(R.layout.list_item_draggable, parent, false);
                break;
            default:
                throw new IllegalStateException("Unexpected viewType (= " + viewType + ")");
        }


        return new MyViewHolder(v);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        final TaskwDataProvider.TaskwData item = mProvider.getItem(position);
        switch (holder.getItemViewType()) {
            case ITEM_VIEW_TYPE_SECTION_HEADER:
                final TaskwDataProvider.TaskwData header_item = mProvider.getItem(position);
                // set text
                holder.mTextView.setText(header_item.getText());
                break;
            case ITEM_VIEW_TYPE_SECTION_ITEM:
                // cleanup
                ((ViewGroup) holder.mCalLabelContainer).removeAllViews();
                // set listeners
                // (if the item is *pinned*, click event comes to the itemView)
//                holder.itemView.setOnClickListener(mItemViewOnClickListener);
                holder.itemView.setOnClickListener(v -> onItemViewClick(v, position, true));
                // (if the item is *not pinned*, click event comes to the mContainer)
                holder.mContainer.setOnClickListener(v -> onItemViewClick(v, position, false));
//                holder.mContainer.setOnClickListener(mSwipeableViewContainerOnClickListener);

                // set text
                holder.mTextView.setText(item.getText());
                // set all other attributes
                holder.mAnnoFlag.setVisibility(item.annoVisibility());
                holder.mStartedFlag.setVisibility(item.startedVisibility());

                // add calendar tags
                Context viewContext = holder.mTextView.getContext();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(viewContext);
                List<View> calLabels = item.buildCalLabels(viewContext, sharedPref);
                if (calLabels.size() > 0){
//                    holder.mCalLabelContainer.setVisibility(View.VISIBLE);
                    holder.mCalLabelContainer.setPadding(35, 0, 35, 0);
                    for (View v : calLabels) {
                        holder.mCalLabelContainer.addView(v);
                    }
                }


                // set background resource (target view ID: container)
                final SwipeableItemState swipeState = holder.getSwipeState();

                if (swipeState.isUpdated()) {
                    int bgResId;

                    if (swipeState.isActive()) {
                        bgResId = R.drawable.bg_item_swiping_active_state;
                    } else if (swipeState.isSwiping()) {
                        bgResId = R.drawable.bg_item_swiping_state;
                    } else {
                        bgResId = R.drawable.bg_item_normal_state;
                    }

                    holder.mContainer.setBackgroundResource(bgResId);
                }

                // set swiping properties
                holder.setSwipeItemHorizontalSlideAmount(
                        item.isPinned() ? Swipeable.OUTSIDE_OF_THE_WINDOW_LEFT : 0);


                // set swiping properties
                holder.setMaxLeftSwipeAmount(-0.5f);
                holder.setMaxRightSwipeAmount(0.5f);
                holder.setSwipeItemHorizontalSlideAmount(item.isPinned() ? -0.5f : 0);

                break;
        }
    }

    @Override
    public int getItemCount() {
        return mProvider.getCount();
    }

    @Override
    public int onGetSwipeReactionType(@NonNull MyViewHolder holder, int position, int x, int y) {
//        return Swipeable.REACTION_CAN_SWIPE_LEFT | Swipeable.REACTION_MASK_START_SWIPE_LEFT |
//                Swipeable.REACTION_CAN_SWIPE_RIGHT | Swipeable.REACTION_MASK_START_SWIPE_RIGHT |
//                Swipeable.REACTION_START_SWIPE_ON_LONG_PRESS;
        return Swipeable.REACTION_CAN_SWIPE_BOTH_H;
    }

    @Override
    public void onSwipeItemStarted(@NonNull MyViewHolder holder, int position) {
        notifyDataSetChanged();
    }

    @Override
    public void onSetSwipeBackground(@NonNull MyViewHolder holder, int position, int type) {
        int bgRes = 0;
        switch (type) {
            case Swipeable.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND:
                bgRes = R.drawable.bg_swipe_item_neutral;
                break;
            case Swipeable.DRAWABLE_SWIPE_LEFT_BACKGROUND:
                bgRes = R.drawable.bg_swipe_item_left;
                break;
            case Swipeable.DRAWABLE_SWIPE_RIGHT_BACKGROUND:
                bgRes = R.drawable.bg_swipe_item_right;
                break;
        }

        holder.itemView.setBackgroundResource(bgRes);
    }

    @Override
    public SwipeResultAction onSwipeItem(@NonNull MyViewHolder holder, final int position, int result) {
        Log.d(TAG, "onSwipeItem(position = " + position + ", result = " + result + ")");

        switch (result) {
            // swipe right
            case Swipeable.RESULT_SWIPED_RIGHT:
                if (mProvider.getItem(position).isPinned()) {
                    // pinned --- back to default position
                    return new UnpinResultAction(this, position);
                } else {
                    // not pinned --- remove
                    return new SwipeRightResultAction(this, position);
                }
                // swipe left -- pin
            case Swipeable.RESULT_SWIPED_LEFT:
                return new SwipeLeftResultAction(this, position);
            // other --- do nothing
            case Swipeable.RESULT_CANCELED:
            default:
                if (position != RecyclerView.NO_POSITION) {
                    return new UnpinResultAction(this, position);
                } else {
                    return null;
                }
        }
    }

    public EventListener getEventListener() {
        return mEventListener;
    }

    public void setEventListener(EventListener eventListener) {
        mEventListener = eventListener;
    }

    private static class SwipeLeftResultAction extends SwipeResultActionMoveToSwipedDirection {
        private SwipeOnLongPressExampleAdapter mAdapter;
        private final int mPosition;
        private boolean mSetPinned;

        SwipeLeftResultAction(SwipeOnLongPressExampleAdapter adapter, int position) {
            mAdapter = adapter;
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();

            TaskwDataProvider.TaskwData item = mAdapter.mProvider.getItem(mPosition);

            if (!item.isPinned()) {
                item.setPinned(true);
                mAdapter.notifyItemChanged(mPosition);
                mSetPinned = true;
            }
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();

            if (mSetPinned && mAdapter.mEventListener != null) {
                mAdapter.mEventListener.onItemPinned(mPosition);
            }
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            // clear the references
            mAdapter = null;
        }
    }

    private static class SwipeRightResultAction extends SwipeResultActionRemoveItem {
        private SwipeOnLongPressExampleAdapter mAdapter;
        private final int mPosition;

        SwipeRightResultAction(SwipeOnLongPressExampleAdapter adapter, int position) {
            mAdapter = adapter;
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();

            mAdapter.mProvider.removeItem(mPosition);
            mAdapter.notifyItemRemoved(mPosition);
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();

            if (mAdapter.mEventListener != null) {
                mAdapter.mEventListener.onItemRemoved(mPosition);
            }
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            // clear the references
            mAdapter = null;
        }
    }

    private static class UnpinResultAction extends SwipeResultActionDefault {
        private SwipeOnLongPressExampleAdapter mAdapter;
        private final int mPosition;

        UnpinResultAction(SwipeOnLongPressExampleAdapter adapter, int position) {
            mAdapter = adapter;
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();

            TaskwDataProvider.TaskwData item = mAdapter.mProvider.getItem(mPosition);
            if (item.isPinned()) {
                item.setPinned(false);
                mAdapter.notifyItemChanged(mPosition);
            }
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            // clear the references
            mAdapter = null;
        }
    }

    public void update(List<JSONObject> list, ReportInfo info) {
//        this.info = info;
//        boolean hasUrgency = info.fields.containsKey("urgency");
//        if (hasUrgency && !list.isEmpty()) { // Search
//            double min = list.get(0).optDouble("urgency");
//            double max = min;
//            for (JSONObject json : list) { // Find min and max
//                double urg = json.optDouble("urgency");
//                if (min > urg) {
//                    min = urg;
//                }
//                if (max < urg) {
//                    max = urg;
//                }
//            }
//            urgMin = (int) Math.floor(min);
//            urgMax = (int) Math.ceil(max);
//        }
//        morph(list, data, uuidAcc);
//        data.clear();
//        data.addAll(list);


        notifyDataSetChanged();
    }


    private View inflateTaskDetailView(final JSONObject json, Context context) {

        return View.inflate(context, R.layout.item_one_task_detail, null);
    }

    private View populateTaskDetailView(final JSONObject json, Context context, View views) {
        if (json == null && context != null)
            return views;

        int urgMin = 0;
        int urgMax = 100;
        // TODO fix me


        ///////////////////////////

        String status = json.optString("status", "pending");

        ImageView task_status_btn = views.findViewById(R.id.task_status_btn);
        task_status_btn.setImageResource(status2icon(status));
        task_status_btn.setOnClickListener(
                v -> {
                    if (null != listener) {
                        listener.onStatus(json);
                    }
                });

        // clear previous contents
        ((ViewGroup) views.findViewById(R.id.task_annotations)).removeAllViews();
        ((ViewGroup) views.findViewById(R.id.task_labels_left)).removeAllViews();
        ((ViewGroup) views.findViewById(R.id.task_labels_right)).removeAllViews();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        bindLongCopyText(json, views.findViewById(R.id.task_description),
                json.optString("description"));


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
                                TextView task_ann_text_view =
                                        annView.findViewById(R.id.task_ann_text);
                                String task_ann_text = ann.optString("description",
                                        "Untitled");
                                task_ann_text_view.setText(task_ann_text);
                                bindLongCopyText(json, task_ann_text_view,
                                        task_ann_text);
                                ((TextView) annView.findViewById(R.id.task_ann_date)).setText(asDate(ann.optString("entry"), sharedPref));
                                View deleteBtn = annView.findViewById(R.id.task_ann_delete_btn);
                                deleteBtn.setVisibility(View.VISIBLE);
                                deleteBtn.setOnClickListener(v -> {
                                    new AlertDialog.Builder(context)
                                            .setTitle("Remove annotation")
                                            .setMessage("Remove annotation '" + ann + "'?")
//                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                                if (null != listener) {
                                                    listener.onDenotate(json, ann);
                                                }
                                            })
                                            .setNegativeButton(android.R.string.no, null).show();
                                });
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
                    ProgressBar pb_priority = views.findViewById(R.id.task_priority);
                    if (index == -1) {
                        pb_priority.setMax(0);
                        pb_priority.setProgress(0);
                    } else {
                        pb_priority.setMax(info.priorities.size() - 1);
                        pb_priority.setProgress(info.priorities.size() - index - 1);
                    }
                    break;
                case "urgency":
                    ProgressBar pb_urgency = views.findViewById(R.id.task_urgency);
                    pb_urgency.setMax(urgMax - urgMin);
                    pb_urgency.setProgress((int) Math.round(json.optDouble("urgency")) - urgMin);
                    break;
                case "due":
                    addLabelWithInsert(context, views, "due", true, R.drawable.ic_label_due,
                            asDate(json.optString("due"), sharedPref));
                    break;
                case "wait":
                    addLabelWithInsert(context, views, "wait", true, R.drawable.ic_label_wait,
                            asDate(json.optString("wait"), sharedPref));
                    break;
                case "scheduled":
                    addLabelWithInsert(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
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
                    addLabelWithInsert(context, views, "recur", true, R.drawable.ic_label_recur, recur);
                    break;
                case "project":
                    addLabelWithInsert(context, views, "project", false, R.drawable.ic_label_project,
                            json.optString("project"));
                    break;
                case "tags":
                    addLabelWithInsert(context, views, "tags", false, R.drawable.ic_label_tags, join(", ", array2List(
                            json.optJSONArray("tags"))));
                    break;
                case "start":
                    String started = asDate(json.optString("start"), sharedPref);
                    boolean isStarted = !TextUtils.isEmpty(started);

                    if ("pending".equalsIgnoreCase(status)) { // Can be started/stopped
                        View start_stop_btn =
                                views.findViewById(R.id.task_start_stop_btn);
                        start_stop_btn.setVisibility(View.VISIBLE);
                        start_stop_btn.setOnClickListener(
                                v -> {
                                    if (null != listener) {
                                        listener.onStartStop(json);
                                    }
                                });
                        ((ImageView) start_stop_btn).setImageResource(isStarted ? R.drawable.ic_action_stop : R.drawable.ic_action_start);
                    }
                    break;
            }

        }
        views.findViewById(R.id.task_edit_btn).setOnClickListener(v -> {
            if (null != listener) {
                listener.onEdit(json);
            }
        });
        views.findViewById(R.id.task_delete_btn).setOnClickListener(
                v -> {
//                    if (null != listener) {
//                        listener.onDelete(json);
//                    }
                    new AlertDialog.Builder(context)
                            .setTitle("Delete task")
                            .setMessage("Do you really want to delete this task?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                if (null != listener) {
                                    listener.onDelete(json);
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                });
        views.findViewById(R.id.task_annotate_btn).setOnClickListener(
                v -> {
                    if (null != listener) {
                        listener.onAnnotate(json);
                    }
                });
        return views;
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

}