package soraxas.taskw.common.data

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.view.View
import com.h6ah4i.android.widget.advrecyclerview.adapter.ItemIdComposer
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import org.json.JSONObject
import soraxas.taskw.R
import soraxas.taskw.common.Helpers
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class TaskwDataProvider {
    var mUUIDtoData: LinkedHashMap<String, TaskwDataItem> = LinkedHashMap()
//    var mUUIDtoDataValuesArray: MutableList<TaskwData> = ArrayList()

    private var groups: MutableList<TaskwDataGroup> = ArrayList()
    private var items: MutableList<MutableList<TaskwDataItem>> = ArrayList()

    private val useProjectAsDivider = true

//    private fun getOrCreateItem(uuid: String, json: JSONObject,
//                                swipeReaction: Int): TaskwDataItem {
//        (mUUIDtoData[uuid] as TaskwDataItem).let {
//            if (it.json != json) {
//                // need to update existing items value
//                it.set_contained_values(uuid, json, swipeReaction)
//            }
//            return it
//        }
//        // need to create a new item
//        return TaskwDataItem(uuid, json, swipeReaction)
//    }
//
//    private fun getOrCreateGroup(uuid: String, json: JSONObject,
//                                swipeReaction: Int): TaskwDataGroup {
//        (mUUIDtoData[uuid] as TaskwDataGroup).let {
//            if (it.json != json) {
//                // need to update existing items value
//                it.set_contained_values(uuid, json, swipeReaction)
//            }
//            return it
//        }
//        // need to create a new item
//        return TaskwDataGroup(uuid, json, swipeReaction)
//    }
//    var urgMin: Int = 0
//    var urgMax: Int = 100

    fun updateReportInfo(list: List<JSONObject>, allPinnedTasks: Set<String>) {
        val swipeReaction = RecyclerViewSwipeManager.REACTION_CAN_SWIPE_UP or RecyclerViewSwipeManager.REACTION_CAN_SWIPE_DOWN

        val newUUIDtoData = LinkedHashMap<String, TaskwDataItem>()
        val pinnedTasks: MutableList<TaskwDataItem> = ArrayList()

        groups.clear()
        items.clear()

        if (!useProjectAsDivider) {
            // add the header
            val projectName = "[All projects]"
            val headerTitle = "$projectName (${list.size})"
//            val group = getOrCreateItem(ITEM_VIEW_TYPE_SECTION_HEADER,
//                    projectName,
//                    JSONObject().put("text", headerTitle), swipeReaction)
            val group = TaskwDataGroup(projectName, list.size, swipeReaction)
//            newUUIDtoData[projectName] = group
            groups.add(group)
            items.add(ArrayList<TaskwDataItem>())
            for (json in list) {
                val uuid: String = json.optString("uuid")
                val item = TaskwDataItem(uuid, json, swipeReaction)
                newUUIDtoData[uuid] = item
                items[items.size - 1].add(item)  // add to latest group
            }
        } else {
            // a map of list of tasks (where each list of task is a project)
            val project: MutableMap<String, MutableList<TaskwDataItem>> = HashMap()
            val projectUrgency: MutableMap<String, Double> = HashMap()
            for (json in list) {
                val uuid = json.optString("uuid")
                val task = TaskwDataItem(uuid, json, swipeReaction)
                if (uuid in allPinnedTasks) {
                    // retrieve the pinned tasks and display them at the front
                    pinnedTasks.add(task)
                    continue
                }
                //
                lateinit var projContainer: MutableList<TaskwDataItem>
                project[task.project]?.let {
                    // contain existing project container
                    projContainer = it
                    if (task.urgency > projectUrgency[task.project]!!) {
                        projectUrgency[task.project] = task.urgency
                    }
                } ?: run {
                    // does not contain existing project container
                    projContainer = ArrayList()
                    project[task.project] = projContainer
                    projectUrgency[task.project] = task.urgency
                }
                projContainer.add(task)  // append task to project container
            }
            // sort by urgency
            val sortedProj: MutableList<String> = ArrayList(project.keys)
            // we sort in reverse (larger number first)
            sortedProj.sortWith(
                    Comparator { a: String?, b: String? ->
                        projectUrgency[b]!!
                                .compareTo(projectUrgency[a]!!)
                    })

            // inject the pinned task at the very front
            if (pinnedTasks.isNotEmpty()) {
                sortedProj.add(0, "[pinned]")
                project["[pinned]"] = pinnedTasks
            }
            // put back into result
            for (projKey in sortedProj) {
                with(project[projKey]!!) {
                    // add the header
                    val projKeyAsUuid = when (projKey) {
                        "" -> "[no project]"
                        else -> projKey
                    }
                    // add project count to the end of the key
                    val headerTitle = "$projKeyAsUuid (${this.size})"
                    val group = TaskwDataGroup(projKeyAsUuid, this.size, swipeReaction)
//                    newUUIDtoData[projKeyAsUuid] = group
                    groups.add(group)
                    items.add(ArrayList<TaskwDataItem>())
                    for (task in this) {
                        newUUIDtoData[task.uuid] = task
                        items[items.size - 1].add(task)  // add to latest group
                    }
                }
            }
        }

        // set the member variable as the newly built (and ordered) result
        mUUIDtoData = newUUIDtoData
    }

    fun getJsonWithTaskUuid(uuid: String): JSONObject? {
//        return (mUUIDtoData[uuid] as TaskwDataItem).json
        return mUUIDtoData[uuid]?.json
    }

//    val count: Int
//        get() = mUUIDtoDataValuesArray.size

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


    fun getGroupSize(): Int {
        return groups.size
    }

    fun getItemSize(groupPos: Int): Int {
        return items[groupPos].size
    }

    fun getGroup(groupPos: Int): TaskwDataGroup {
        return groups[groupPos]
    }

    fun getItem(groupPos: Int, itemPos: Int): TaskwDataItem {
        return items[groupPos][itemPos]
    }

    fun removeGroup(groupPos: Int) {
        groups.removeAt(groupPos)
        items.removeAt(groupPos)
    }

    fun removeItem(groupPos: Int, itemPos: Int) {
        items[groupPos].removeAt(itemPos)
    }

    abstract class TaskwData {
        lateinit var text: String
        var viewType = 0
            private set
        abstract var uuid: String
        lateinit var uuid_: UUID
        var id: Long = -1

        protected fun _uuid_to_long(): Long {
            var id: Long = uuid_.mostSignificantBits
            if (id < 0) {
                id = -((-id) % ItemIdComposer.MAX_GROUP_ID)
            } else {
                id %= ItemIdComposer.MAX_GROUP_ID
            }
            return id
//            return uuid_.mostSignificantBits and Long.MAX_VALUE
        }

    }

    class TaskwDataItem(override var uuid: String, json: JSONObject,
                        swipeReaction:
                        Int) : TaskwData() {
        lateinit var json: JSONObject
        val hasAnno: Boolean
            get() = json.optJSONArray("annotations") != null
        val hasStarted: Boolean
            get() = !TextUtils.isEmpty(json.optString("start"))
        var isPinned = false

        val urgency: Double
            get() = json.optDouble("urgency", 0.0)

        val project: String
            get() = json.optString("project", "")


        val annoVisibility: Int
            get() = if (hasAnno) View.VISIBLE else View.GONE


        val startedVisibility: Int
            get() = if (hasStarted) View.VISIBLE else View.INVISIBLE

        init {
            set_contained_values(uuid, json, swipeReaction)
        }

        fun set_contained_values(uuidStr: String, json: JSONObject,
                                 swipeReaction: Int) {
            this.json = json
            text = json.optString("description")
            this.uuid = uuidStr
            uuid_ = UUID.fromString(uuidStr)
            id = _uuid_to_long()
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
    }

    class TaskwDataGroup(groupName: String, val groupItemNum: Int, swipeReaction: Int) :
            TaskwData() {
        var isPinned = false
        lateinit var groupName: String

        override var uuid: String = ""
            get() = groupName

        init {
            set_contained_values(groupName, swipeReaction)
        }

        fun set_contained_values(groupName: String,
                                 swipeReaction: Int) {
            this.text = groupName
            this.groupName = groupName
            // header's uuid is not a valid uuid format. We will construct a
            // uuid with its byte-array instead.
            this.uuid_ = UUID.nameUUIDFromBytes(this.uuid.toByteArray())
            id = _uuid_to_long()
        }
    }

    companion object {
        const val ITEM_VIEW_TYPE_SECTION_HEADER = 0
        const val ITEM_VIEW_TYPE_SECTION_ITEM = 1
    }
}