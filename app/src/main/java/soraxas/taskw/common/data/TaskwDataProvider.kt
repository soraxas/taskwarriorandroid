package soraxas.taskw.common.data

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Pair
import android.view.View
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import org.json.JSONObject
import soraxas.taskw.R
import soraxas.taskw.common.Helpers
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

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
    var mUUIDtoData: LinkedHashMap<String, TaskwData> = LinkedHashMap()
    var mUUIDtoDataValuesArray: MutableList<TaskwData> = ArrayList()

    private var mLastRemovedData: TaskwData? = null
    private var mLastRemovedPosition = -1
    private val useProjectAsDivider = true

    private fun getOrCreateItem(item_type: Int, uuid: String, json: JSONObject,
                                swipeReaction: Int): TaskwData {
        mUUIDtoData[uuid]?.let {
            if (it.json != json) {
                // need to update existing items value
                it.set_contained_values(item_type, uuid, json, swipeReaction)
            }
            return it
        }
        // need to create a new item
        return TaskwData(item_type, uuid, json, swipeReaction)
    }
//    var urgMin: Int = 0
//    var urgMax: Int = 100

    fun update_report_info(list: List<JSONObject>, allPinnedTasks: Set<String>) {
//      var jsonData: MutableList<JSONObject?> = ArrayList()
//    private val mData: MutableList<TaskwData> = ArrayList()
        val swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP or RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN

        val newDataContainer: MutableList<TaskwData> = ArrayList()
        val newUUIDtoData = LinkedHashMap<String, TaskwData>()

        val pinnedTasks: MutableList<JSONObject> = ArrayList()

        if (!useProjectAsDivider) {
            for (json in list) {
                val uuid: String = json.optString("uuid")
                newUUIDtoData[uuid] = getOrCreateItem(ITEM_VIEW_TYPE_SECTION_ITEM,
                        uuid, json, swipeReaction)
            }
        } else {
            // a map of list of tasks (where each list of task is a project)
            val project: MutableMap<String, Pair<MutableList<JSONObject>, Double>?> = HashMap()
            for (json in list) {
                val uuid = json.optString("uuid")
                if (uuid in allPinnedTasks) {
                    // retrieve the pinned tasks and display them at the front
                    pinnedTasks.add(json)
                    continue
                }
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
            val sorted_proj: MutableList<String> = ArrayList(project.keys)
            sorted_proj.sortWith(
                    Comparator { a: String?, b: String? -> project[a]!!.second.compareTo(project[b]!!.second) })

            // inject the pinned task at the very front
            if (pinnedTasks.isNotEmpty()) {
                sorted_proj.add(0, "[pinned]")
                project["[pinned]"] = Pair(pinnedTasks, 0.0)
            }
            // put back into result
            for (projKey in sorted_proj) {
                run {
                    // add the header
                    var projKeyAsUuid = when (projKey) {
                        "" -> "[no project]"
                        else -> projKey
                    }
                    // add project count to the end of the key
                    projKeyAsUuid += " (" + project[projKey]!!.first.size + ")"
                    newUUIDtoData[projKeyAsUuid] = getOrCreateItem(ITEM_VIEW_TYPE_SECTION_HEADER,
                            projKeyAsUuid,
                            JSONObject(), swipeReaction)
                }
                for (json in project[projKey]!!.first) {
                    val uuid: String = json.optString("uuid")
                    newUUIDtoData[uuid] = getOrCreateItem(ITEM_VIEW_TYPE_SECTION_ITEM, uuid, json,
                            swipeReaction)
                }
            }
        }

        // set the member variable as the newly built (and ordered) result
        mUUIDtoData = newUUIDtoData
        mUUIDtoDataValuesArray = ArrayList(newUUIDtoData.values)
    }

    fun getJsonWithTaskUuid(uuid: String): JSONObject? {
        return mUUIDtoData[uuid]?.json
    }

    val count: Int
        get() = mUUIDtoDataValuesArray.size

    fun getItem(index: Int): TaskwData {
        if (index < 0 || index >= count) {
            throw IndexOutOfBoundsException("index = $index")
        }
        return mUUIDtoDataValuesArray[index]
    }

//    fun undoLastRemoval(): Int {
//        return if (mLastRemovedData != null) {
//            val insertedPosition: Int
//            insertedPosition = if (mLastRemovedPosition >= 0 && mLastRemovedPosition < mData.size) {
//                mLastRemovedPosition
//            } else {
//                mData.size
//            }
//            mData.add(insertedPosition, mLastRemovedData!!)
//            mLastRemovedData = null
//            mLastRemovedPosition = -1
//            insertedPosition
//        } else {
//            -1
//        }
//    }

//    fun moveItem(fromPosition: Int, toPosition: Int) {
//        if (fromPosition == toPosition) {
//            return
//        }
//        val item = mData.removeAt(fromPosition)
//        mData.add(toPosition, item)
//        mLastRemovedPosition = -1
//    }
//
//    fun swapItem(fromPosition: Int, toPosition: Int) {
//        if (fromPosition == toPosition) {
//            return
//        }
//        Collections.swap(mData, toPosition, fromPosition)
//        mLastRemovedPosition = -1
//    }

    fun removeItem(position: Int) {
        // this might make the view be in inconsistent state compared to the hashmap
        val removedItem = mUUIDtoDataValuesArray.removeAt(position)
        mLastRemovedData = removedItem
        mLastRemovedPosition = position
    }

    class TaskwData {
        lateinit var text: String
        var hasAnno = false
        var hasStarted = false
        lateinit var json: JSONObject
        var viewType = 0
            private set
        var isPinned = false
        lateinit var uuidStr: String
        lateinit var uuid: UUID
        var id: Long = -1

        val urgency: Double
            get() = json.optDouble("urgency", 0.0)

        private fun _uuid_to_long(): Long {
            return uuid.mostSignificantBits and kotlin.Long.MAX_VALUE
        }

        internal constructor(viewType: Int, uuid: String, json: JSONObject,
                             swipeReaction:
                             Int) {
            set_contained_values(viewType, uuid, json, swipeReaction)
        }

        fun set_contained_values(viewType: Int, uuidStr: String, json: JSONObject,
                                 swipeReaction: Int) {
            when (viewType) {
                ITEM_VIEW_TYPE_SECTION_HEADER -> {
                    this.json = json
                    this.text = uuidStr
                    this.uuidStr = uuidStr
                    // header's uuid is not a valid uuid format. We will construct a
                    // uuid with its byte-array instead.
                    this.uuid = UUID.nameUUIDFromBytes(uuidStr.toByteArray())
                    id = _uuid_to_long()
                }
                ITEM_VIEW_TYPE_SECTION_ITEM -> {
                    this.json = json
                    this.viewType = viewType
                    text = makeText(json.optString("description"), swipeReaction)
                    hasStarted = !TextUtils.isEmpty(json.optString("start"))
                    hasAnno = json.optJSONArray("annotations") != null
                    this.uuidStr = uuidStr
                    uuid = UUID.fromString(uuidStr)
                    id = _uuid_to_long()
                }
                else -> {
                    throw AssertionError("Assertion failed")
                }
            }
        }

        fun buildCalLabels(context: Context,
                           sharedPref: SharedPreferences?): List<View> {
            val views: MutableList<View> = ArrayList()
            val it = json.keys()
            while (it.hasNext()) {
                val key = it.next()
                var view: View
                if (key == "due")
                    view = Helpers.createLabel(context, true, R.drawable.ic_label_due,
                            Helpers.asDate(json.optString("due"), sharedPref))
                else if (key == "wait")
                    view = Helpers.createLabel(context, true, R.drawable.ic_label_wait,
                            Helpers.asDate(json.optString("wait"), sharedPref))
                else if (key == "scheduled")
                    view = Helpers.createLabel(context, true,
                            R.drawable.ic_label_scheduled,
                            Helpers.asDate(json.optString("scheduled"), sharedPref))
                else if (key == "recur") {
                    var recur = json.optString("recur")
                    var until = json.optString("until")
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