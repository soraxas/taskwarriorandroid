package soraxas.taskw.common.data

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Pair
import android.view.View
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import org.json.JSONObject
import soraxas.taskw.BuildConfig
import soraxas.taskw.R
import soraxas.taskw.common.Helpers
import java.util.*
import kotlin.collections.HashMap

/*
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
class TaskwDataProvider {
    val mData: MutableList<TaskwData> = ArrayList()
    val mUuidToData: MutableMap<String, TaskwData> = HashMap()

    private var mLastRemovedData: TaskwData? = null
    private var mLastRemovedPosition = -1
    private val useProjectAsDivider = true

    fun update_report_info(list: List<JSONObject>) {
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


        mData.clear()
        mUuidToData.clear()

//    var jsonData: MutableList<JSONObject?> = ArrayList()
//    private val mData: MutableList<TaskwData> = ArrayList()

        if (!useProjectAsDivider) {
            for (json in list) {
                val swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP or RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN
                val data = TaskwData(ITEM_VIEW_TYPE_SECTION_ITEM, json,
                        swipeReaction)
                mData.add(data)
                mUuidToData[data.uuidStr] = data
            }
        } else {
            // a map of list of tasks (where each list of task is a project)
            val project: MutableMap<String, Pair<MutableList<JSONObject>, Double>?> = HashMap()
            var id = 0
            for (json in list) {
                //
                val proj = json.optString("project", "")
                var urgency = json.optDouble("urgency")
                var proj_container: MutableList<JSONObject>
                if (project.containsKey(proj)) {
                    val old_pair = project[proj]
                    proj_container = old_pair!!.first
                    if (urgency < old_pair.second) urgency = old_pair.second
                } else {
                    proj_container = ArrayList()
                }
                proj_container.add(json)
                project[proj] = Pair(proj_container, urgency)
            }
            // sort by urgency
            val sorted_proj: List<String> = ArrayList(project.keys)
            Collections.sort(sorted_proj
            ) { a: String?, b: String? -> project[a]!!.second.compareTo(project[b]!!.second) }

            // put back into result
            for (proj_key in sorted_proj) {
                mData.add(TaskwData(ITEM_VIEW_TYPE_SECTION_HEADER,
                        if (proj_key == "") "[no project]" else proj_key))
                for (json in project[proj_key]!!.first) {
                    val swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP or RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN
                    val data = TaskwData(ITEM_VIEW_TYPE_SECTION_ITEM,
                            json, swipeReaction)
                    mData.add(data)
                    mUuidToData[data.uuidStr] = data
                }
            }
        }
    }

    fun getJsonWithTaskUuid(uuid: String): JSONObject? {
        var json: JSONObject? = null
        for (data in mData) {
            if (data.json.optString("uuid") == uuid) {
                json = data.json
                break
            }
        }
        return json
    }

    val count: Int
        get() = mData.size

    fun getItem(index: Int): TaskwData {
        if (index < 0 || index >= count) {
            throw IndexOutOfBoundsException("index = $index")
        }
        return mData[index]
    }

    fun undoLastRemoval(): Int {
        return if (mLastRemovedData != null) {
            val insertedPosition: Int
            insertedPosition = if (mLastRemovedPosition >= 0 && mLastRemovedPosition < mData.size) {
                mLastRemovedPosition
            } else {
                mData.size
            }
            mData.add(insertedPosition, mLastRemovedData!!)
            mLastRemovedData = null
            mLastRemovedPosition = -1
            insertedPosition
        } else {
            -1
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) {
            return
        }
        val item = mData.removeAt(fromPosition)
        mData.add(toPosition, item)
        mLastRemovedPosition = -1
    }

    fun swapItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) {
            return
        }
        Collections.swap(mData, toPosition, fromPosition)
        mLastRemovedPosition = -1
    }

    fun removeItem(position: Int) {
        val removedItem = mData.removeAt(position)
        mLastRemovedData = removedItem
        mLastRemovedPosition = position
    }

    class TaskwData {
        val text: String
        var hasAnno = false
        var hasStarted = false
        var json: JSONObject
        var viewType = 0
            private set
        var isPinned = false
        val uuidStr: String
        val uuid: UUID
        val id: Long by lazy {
            _uuid_to_long()
        }

        private fun _uuid_to_long(): Long {
            return uuid.mostSignificantBits and kotlin.Long.MAX_VALUE
        }

        internal constructor(viewType: Int, description: String) {
            if (BuildConfig.DEBUG && viewType != ITEM_VIEW_TYPE_SECTION_HEADER) {
                throw AssertionError("Assertion failed")
            }
            text = description
            json = JSONObject()  // placeholder
            uuidStr = description
            uuid = UUID.nameUUIDFromBytes(text.toByteArray())
        }

        internal constructor(viewType: Int, json: JSONObject, swipeReaction: Int) {
            this.json = json
            this.viewType = viewType
            text = makeText(json.optString("description"), swipeReaction)
            hasStarted = !TextUtils.isEmpty(json.optString("start"))
            hasAnno = json.optJSONArray("annotations") != null
            uuidStr = json.optString("uuid")
            uuid = UUID.fromString(uuidStr)


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

        fun buildCalLabels(context: Context,
                           sharedPref: SharedPreferences?): List<View> {
            val views: MutableList<View> = ArrayList()
            val it = json!!.keys()
            while (it.hasNext()) {
                val key = it.next()
                var view: View
                if (key == "due")
                    view = Helpers.createLabel(context, true, R.drawable.ic_label_due,
                            Helpers.asDate(json!!.optString("due"), sharedPref))
                else if (key == "wait")
                    view = Helpers.createLabel(context, true, R.drawable.ic_label_wait,
                            Helpers.asDate(json!!.optString("wait"), sharedPref))
                else if (key == "scheduled")
                    view = Helpers.createLabel(context, true,
                            R.drawable.ic_label_scheduled,
                            Helpers.asDate(json!!.optString("scheduled"), sharedPref))
                else if (key == "recur") {
                    var recur = json!!.optString("recur")
                    var until = json!!.optString("until")
                    if (!TextUtils.isEmpty(recur) && !TextUtils.isEmpty(until)) {
                        until = Helpers.asDate(until, sharedPref)
                        if (!TextUtils.isEmpty(until)) {
                            recur += String.format(" ~ %s", until)
                        }
                    }
                    view = Helpers.createLabel(context, true, R.drawable.ic_label_recur,
                            recur)
                } else {
                    continue
                }
                views.add(view)
            }
            return views
        }

        val isSectionHeader: Boolean
            get() = false

        override fun toString(): String {
            return text
        }

        fun annoVisibility(): Int {
            return if (hasAnno) View.VISIBLE else View.GONE
        }

        fun startedVisibility(): Int {
            return if (hasStarted) View.VISIBLE else View.INVISIBLE
        }

        private fun makeText(text: String, swipeReaction: Int): String {
            return text
            //            return String.valueOf(id) + " - " + text;
        }
    }

    companion object {
        const val ITEM_VIEW_TYPE_SECTION_HEADER = 0
        const val ITEM_VIEW_TYPE_SECTION_ITEM = 1
    }
}