package soraxas.taskw.common.data;/*
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


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soraxas.taskw.BuildConfig;
import soraxas.taskw.R;
import soraxas.taskw.data.ReportInfo;

import static soraxas.taskw.common.Helpers.asDate;
import static soraxas.taskw.common.Helpers.createLabel;


public class TaskwDataProvider {
    public static final int ITEM_VIEW_TYPE_SECTION_HEADER = 0;
    public static final int ITEM_VIEW_TYPE_SECTION_ITEM = 1;

    private List<TaskwData> mData;
    public List<JSONObject> jsonData;
    private TaskwData mLastRemovedData;
    private int mLastRemovedPosition = -1;

    public TaskwDataProvider() {
        mData = new ArrayList<>();
        jsonData = new ArrayList<>();
    }

    public void update_report_info(List<JSONObject> list, ReportInfo info) {
//
//        int urgMin;
//        int urgMax;
//
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

        mData.clear();
        jsonData.clear();
        boolean separate_projects = true;

        if (!separate_projects) {
            for (JSONObject json : list) {
                final long id = mData.size();
                final int swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP | RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN;
                mData.add(new TaskwData(id, ITEM_VIEW_TYPE_SECTION_ITEM, json, swipeReaction));
                jsonData.add(json);
            }
        } else {
            // a map of list of tasks (where each list of task is a project)
            Map<String, Pair<List<JSONObject>, Double>> project =
                    new HashMap<>();
            int id = 0;
            for (JSONObject json : list) {
                //
                String proj = json.optString("project", "");
                double urgency = json.optDouble("urgency");
                List<JSONObject> proj_container;
                if (project.containsKey(proj)) {
                    Pair<List<JSONObject>, Double> old_pair = project.get(proj);
                    proj_container = old_pair.first;
                    if (urgency < old_pair.second)
                        urgency = old_pair.second;
                } else {
                    proj_container = new ArrayList<>();
                }
                proj_container.add(json);
                project.put(proj, new Pair<>(proj_container, urgency));
            }
            // sort by urgency
            List<String> sorted_proj = new ArrayList<>(project.keySet());
            Collections.sort(sorted_proj,
                    (a, b) -> project.get(a).second.compareTo(project.get(b).second));

            // put back into result
            for (String proj_key : sorted_proj) {
                mData.add(new TaskwData(id++, ITEM_VIEW_TYPE_SECTION_HEADER,
                        proj_key.equals("") ? "[no project]" : proj_key));
                jsonData.add(null);  // this is a header
                for (JSONObject json : project.get(proj_key).first) {
                    final int swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP | RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN;
                    TaskwData data = new TaskwData(id++, ITEM_VIEW_TYPE_SECTION_ITEM,
                            json, swipeReaction);
                    mData.add(data);
                    jsonData.add(json);
                }
            }

        }


    }

    public int getCount() {
        return mData.size();
    }

    public TaskwData getItem(int index) {
        if (index < 0 || index >= getCount()) {
            throw new IndexOutOfBoundsException("index = " + index);
        }

        return mData.get(index);
    }

    public int undoLastRemoval() {
        if (mLastRemovedData != null) {
            int insertedPosition;
            if (mLastRemovedPosition >= 0 && mLastRemovedPosition < mData.size()) {
                insertedPosition = mLastRemovedPosition;
            } else {
                insertedPosition = mData.size();
            }

            mData.add(insertedPosition, mLastRemovedData);

            mLastRemovedData = null;
            mLastRemovedPosition = -1;

            return insertedPosition;
        } else {
            return -1;
        }
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        final TaskwData item = mData.remove(fromPosition);

        mData.add(toPosition, item);
        mLastRemovedPosition = -1;
    }

    public void swapItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        Collections.swap(mData, toPosition, fromPosition);
        mLastRemovedPosition = -1;
    }

    public void removeItem(int position) {
        //noinspection UnnecessaryLocalVariable
        final TaskwData removedItem = mData.remove(position);

        mLastRemovedData = removedItem;
        mLastRemovedPosition = position;
    }

    public static final class TaskwData {

        private long mId;
        @NonNull
        private final String mText;
        private int mViewType;
        private boolean mPinned;
        public boolean hasAnno;
        public boolean hasStarted;

        public JSONObject json;

        TaskwData(long id, int viewType, String description) {
            if (BuildConfig.DEBUG && viewType != ITEM_VIEW_TYPE_SECTION_HEADER) {
                throw new AssertionError("Assertion failed");
            }
            mText = description;
        }

        TaskwData(long id, int viewType, @NonNull JSONObject json, int swipeReaction) {
            this.json = json;
            mId = id;
            mViewType = viewType;
            mText = makeText(id, json.optString("description"), swipeReaction);
            hasStarted = !TextUtils.isEmpty(json.optString("start"));
            hasAnno = json.optJSONArray("annotations") != null;

//            for (Iterator<String> it = json.keys(); it.hasNext(); ) {
//                String key = it.next();
//
//                switch (key) {
//                    case "description":
//                        break;
//                    case "id":
//                        ((TextView) views.findViewById(R.id.task_id)).setText(String.format("[%d]", json.optInt("id", -1)));
//                        break;
//                    case "priority":
//                        int index = info.priorities.indexOf(json.optString("priority", ""));
//                        ProgressBar pb_priority = views.findViewById(R.id.task_priority);
//                        if (index == -1) {
//                            pb_priority.setMax(0);
//                            pb_priority.setProgress(0);
//                        } else {
//                            pb_priority.setMax(info.priorities.size() - 1);
//                            pb_priority.setProgress(info.priorities.size() - index - 1);
//                        }
//                        break;
//                    case "urgency":
//                        ProgressBar pb_urgency = views.findViewById(R.id.task_urgency);
//                        pb_urgency.setMax(urgMax - urgMin);
//                        pb_urgency.setProgress((int) Math.round(json.optDouble("urgency")) - urgMin);
//                        break;
//                    case "due":
//                        addLabel(context, views, "due", true, R.drawable.ic_label_due,
//                                asDate(json.optString("due"), sharedPref));
//                        break;
//                    case "wait":
//                        addLabel(context, views, "wait", true, R.drawable.ic_label_wait,
//                                asDate(json.optString("wait"), sharedPref));
//                        break;
//                    case "scheduled":
//                        addLabel(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
//                                asDate(json.optString("scheduled"), sharedPref));
//                        break;
//                    case "recur":
//                        String recur = json.optString("recur");
//                        if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
//                            String until = asDate(json.optString("until"), sharedPref);
//                            if (!TextUtils.isEmpty(until)) {
//                                recur += String.format(" ~ %s", until);
//                            }
//                        }
//                        addLabel(context, views, "recur", true, R.drawable.ic_label_recur, recur);
//                        break;
//                    case "tags":
//                        addLabel(context, views, "tags", false, R.drawable.ic_label_tags, join(", ", array2List(
//                                json.optJSONArray("tags"))));
//                        break;
//                }
//            }
        }

        public List<View> buildCalLabels(Context context,
                                         SharedPreferences sharedPref) {
            List<View> views = new ArrayList<View>();
            for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                String key = it.next();
                View view;
                switch (key) {
                    case "due":
                        view = createLabel(context, true, R.drawable.ic_label_due,
                                asDate(json.optString("due"), sharedPref));
//                        createLabel(context, true, R.drawable.ic_label_due,
//                                asDate(json.optString("due")));
                        break;
                    case "wait":
                        view = createLabel(context, true, R.drawable.ic_label_wait,
                                asDate(json.optString("wait"), sharedPref));
                        break;
                    case "scheduled":
                        view = createLabel(context, true,
                                R.drawable.ic_label_scheduled,
                                asDate(json.optString("scheduled"), sharedPref));
                        break;
                    case "recur":
                        String recur = json.optString("recur");
                        String until = json.optString("until");
                        if (!TextUtils.isEmpty(recur) && !TextUtils.isEmpty(until)) {
                            until = asDate(until, sharedPref);
                            if (!TextUtils.isEmpty(until)) {
                                recur += String.format(" ~ %s", until);
                            }
                        }
                        view = createLabel(context, true, R.drawable.ic_label_recur,
                                recur);
                        break;
                    default:
                        continue;
                }
                views.add(view);
            }
            return views;
        }

        private static String makeText(long id, String text, int swipeReaction) {
            return text;
//            return String.valueOf(id) + " - " + text;
        }

        public boolean isSectionHeader() {
            return false;
        }

        public int getViewType() {
            return mViewType;
        }

        public long getId() {
            return mId;
        }

        @NonNull
        public String toString() {
            return mText;
        }

        public String getText() {
            return mText;
        }

        public boolean isPinned() {
            return mPinned;
        }

        public void setPinned(boolean pinned) {
            mPinned = pinned;
        }

        public int annoVisibility() {
            return hasAnno ? View.VISIBLE : View.GONE;
        }

        public int startedVisibility() {
            return hasStarted ? View.VISIBLE : View.INVISIBLE;
        }
    }
}
