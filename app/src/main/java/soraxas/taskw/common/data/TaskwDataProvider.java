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


import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;

import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import soraxas.taskw.data.ReportInfo;


public class TaskwDataProvider {
    public static final int ITEM_VIEW_TYPE_SECTION_HEADER = 0;
    public static final int ITEM_VIEW_TYPE_SECTION_ITEM = 1;

    private List<TaskwData> mData;
    private TaskwData mLastRemovedData;
    private int mLastRemovedPosition = -1;

    public TaskwDataProvider() {
        mData = new LinkedList<>();
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
        for (JSONObject json : list) {
            final long id = mData.size();
            final int swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP | RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN;
            mData.add(new TaskwData(id, ITEM_VIEW_TYPE_SECTION_ITEM, json, swipeReaction));
        }


//        protected ReportInfo doInBackground() {
//            logger.d("Load:", query, report);
//            return controller.accountController(account).taskReportInfo(report, query);
//        }
//        final String atoz = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//        for (int i = 0; i < 2; i++) {
//            for (int j = 0; j < atoz.length(); j++) {
//                final long id = mData.size();
////                final int viewType = 0;
//                final int viewType = j % 5 == 0 ? 0 : 1;
//                String text = Character.toString(atoz.charAt(j));
//                if (j % 3 == 0) {
//                    text = "2020-01-04 00:34:04.829 18077-18135";
//                }
//                final int swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP | RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN;
//                mData.add(new ConcreteData(id, viewType, text, swipeReaction));
//            }
//        }ing atoz = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
//        for (int i = 0; i < 2; i++) {
//            for (int j = 0; j < atoz.length(); j++) {
//                final long id = mData.size();
////                final int viewType = 0;
//                final int viewType = j % 5 == 0 ? 0 : 1;
//                String text = Character.toString(atoz.charAt(j));
//                if (j % 3 == 0) {
//                    text = "2020-01-04 00:34:04.829 18077-18135";
//                }
//                final int swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP | RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN;
//                mData.add(new ConcreteData(id, viewType, text, swipeReaction));
//            }
//        }
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

        private final long mId;
        @NonNull
        private final String mText;
        private final int mViewType;
        private boolean mPinned;
        public boolean hasAnno;
        public boolean hasStarted;

        TaskwData(long id, int viewType, @NonNull JSONObject json, int swipeReaction) {
            mId = id;
            mViewType = viewType;
            mText = makeText(id, json.optString("description"), swipeReaction);
            hasStarted = !TextUtils.isEmpty(json.optString("start"));
            hasAnno = json.optJSONArray("annotations") != null;
        }

        private static String makeText(long id, String text, int swipeReaction) {
            return String.valueOf(id) + " - " + text;
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

        public int annoVisibility(){
            return hasAnno ? View.VISIBLE : View.INVISIBLE;
        }

        public int startedVisibility(){
            return hasStarted ? View.VISIBLE : View.INVISIBLE;
        }
    }
}
