package soraxas.taskw.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionMoveToSwipedDirection
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kvj.bravo7.log.Logger
import soraxas.taskw.R
import soraxas.taskw.common.Helpers
import soraxas.taskw.common.data.TaskwDataProvider
import soraxas.taskw.data.ReportInfo
import kotlin.math.roundToInt

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
class SwipeListAdapter(private val mProvider: TaskwDataProvider) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), SwipeableItemAdapter<RecyclerView.ViewHolder> {
    lateinit var info: ReportInfo
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    @JvmField
    var updateCurTaskDetailView: Runnable? = null

    @JvmField
    var listener: ItemListener? = null
    var eventListener: EventListener? = null
    private val mItemViewOnClickListener: View.OnClickListener? = null
    private val mSwipeableViewContainerOnClickListener: View.OnClickListener? = null
    private val urgMin = 0
    private val urgMax = 0

    private fun onItemViewClick(v: View, position: Int, pinned: Boolean) {
        val json = mProvider.getItem(position).json
        val taskUuid = json.optString("uuid")
        val context = v.context
        val taskDetailView = View.inflate(context, R.layout.item_one_task_detail, null)

        updateCurTaskDetailView = Runnable {
            uiScope.launch {
                populateTaskDetailView(mProvider.getJsonWithTaskUuid(taskUuid), context,
                        taskDetailView)
            }
        }
        updateCurTaskDetailView!!.run()
        showD(context, taskDetailView)

    }

    override fun getItemId(position: Int): Long {
        return mProvider.getItem(position).id
    }

    override fun getItemViewType(position: Int): Int {
        return mProvider.getItem(position).viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_HEADER -> {
                MyTitleViewHolder(inflater.inflate(R.layout.list_section_header, parent, false))
            }
            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_ITEM -> {
                MySwipeableViewHolder(inflater.inflate(R.layout
                        .list_item_draggable, parent, false))
            }
            else -> {
                throw IllegalStateException("Unexpected viewType (= $viewType)")
            }
        }
    }

    private fun update_label_left_right_attr(view: View) {
        (view.findViewById<View>(R.id.label_text) as TextView).textSize = 12f
        val icon = view.findViewById<ImageView>(R.id.label_icon)
        icon.scaleX = 0.7f
        icon.scaleY = 0.7f
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = mProvider.getItem(position)
        when (holder.itemViewType) {
            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_HEADER -> {
                val header_item = mProvider.getItem(position)
                holder as MyTitleViewHolder
                // set text
                holder.mTextView.text = header_item.text
            }
            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_ITEM -> {
                holder as MySwipeableViewHolder
                // cleanup
                (holder.mCalLabelContainer as ViewGroup).removeAllViews()
                (holder.mTagsLabelContainer as ViewGroup).removeAllViews()
                // set listeners
                // (if the item is *pinned*, click event comes to the itemView)
//                holder.itemView.setOnClickListener(mItemViewOnClickListener);
                holder.itemView.setOnClickListener { v: View -> onItemViewClick(v, position, true) }
                // (if the item is *not pinned*, click event comes to the mContainer)
                holder.mContainer.setOnClickListener { v: View -> onItemViewClick(v,
                        position, false)
                }
                //                holder.mContainer.setOnClickListener(mSwipeableViewContainerOnClickListener);

                // set text
                holder.mTextView.text = item.text
                // set all other attributes
                holder.mAnnoFlag.visibility = item.annoVisibility()
                holder.mStartedFlag.visibility = item.startedVisibility()

                // add calendar labels
                val viewContext = holder.mTextView.context
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(viewContext)
                val calLabels = item.buildCalLabels(viewContext, sharedPref)
                calLabels.forEach { v ->
                    update_label_left_right_attr(v)
                    holder.mCalLabelContainer.addView(v)
                }
                // add tags labels
                val tags = item.json!!.optJSONArray("tags")
                tags?.let {
                    val v = Helpers.createLabel(viewContext, false,
                            R.drawable.ic_label_tags, Helpers.join("m", Helpers
                            .array2List(it)))
                    update_label_left_right_attr(v)
                    holder.mTagsLabelContainer.addView(v)
                }


                // set background resource (target view ID: container)
                val swipeState = holder.swipeState
                if (swipeState.isUpdated) {
                    val bgResId: Int = when {
                        swipeState.isActive ->
                            R.drawable.bg_item_swiping_active_state
                        swipeState.isSwiping ->
                            R.drawable.bg_item_swiping_state
                        else ->
                            R.drawable.bg_item_normal_state
                    }
                    holder.mContainer.setBackgroundResource(bgResId)
                }

                // set swiping properties
                holder.swipeItemHorizontalSlideAmount = if (item.isPinned) SwipeableItemConstants
                        .OUTSIDE_OF_THE_WINDOW_LEFT else 0f


                // set swiping properties
                holder.maxLeftSwipeAmount = -0.5f
                holder.maxRightSwipeAmount = 0.5f
                holder.swipeItemHorizontalSlideAmount = if (item.isPinned) -0.5f
                else 0f
            }
        }
    }

    override fun getItemCount(): Int {
        return mProvider.count
    }

    override fun onGetSwipeReactionType(holder: RecyclerView.ViewHolder, position: Int, x: Int, y: Int): Int {
//        return Swipeable.REACTION_CAN_SWIPE_LEFT | Swipeable.REACTION_MASK_START_SWIPE_LEFT |
//                Swipeable.REACTION_CAN_SWIPE_RIGHT | Swipeable.REACTION_MASK_START_SWIPE_RIGHT |
//                Swipeable.REACTION_START_SWIPE_ON_LONG_PRESS;
        return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
    }

    override fun onSwipeItemStarted(holder: RecyclerView.ViewHolder, position: Int) {
//        notifyDataSetChanged()
    }

    override fun onSetSwipeBackground(holder: RecyclerView.ViewHolder, position: Int, type: Int) {
        var bgRes = 0
        when (type) {
            SwipeableItemConstants.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_neutral
            SwipeableItemConstants.DRAWABLE_SWIPE_LEFT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_left
            SwipeableItemConstants.DRAWABLE_SWIPE_RIGHT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_right
        }
        holder.itemView.setBackgroundResource(bgRes)
    }

    override fun onSwipeItem(holder: RecyclerView.ViewHolder, position: Int, result: Int): SwipeResultAction? {
        Log.d(TAG, "onSwipeItem(position = $position, result = $result)")
        return when (result) {
            SwipeableItemConstants.RESULT_SWIPED_RIGHT -> if (mProvider.getItem(position).isPinned) {
                // pinned --- back to default position
                UnpinResultAction(this, position)
            } else {
                // not pinned --- remove
                SwipeRightResultAction(this, position)
            }
            SwipeableItemConstants.RESULT_SWIPED_LEFT -> SwipeLeftResultAction(this, position)
            SwipeableItemConstants.RESULT_CANCELED -> if (position != RecyclerView.NO_POSITION) {
                UnpinResultAction(this, position)
            } else {
                null
            }
            else -> if (position != RecyclerView.NO_POSITION) {
                UnpinResultAction(this, position)
            } else {
                null
            }
        }
    }

    private fun populateTaskDetailView(json: JSONObject?, context: Context?, views: View): View {
        if (json == null && context != null) return views
        val urgMin = 0
        val urgMax = 100
        // TODO fix me

//        Snackbar.make(views, "Subscription Deleted", Snackbar.LENGTH_LONG)
//                .setAction("Undo",  a -> {
////                            activeSubs.add(position-1, tmp)
////                            adapter!!.notifyDataSetChanged()
//                });

        ///////////////////////////
        val status = json!!.optString("status", "pending")
        val task_status_btn = views.findViewById<ImageView>(R.id.task_status_btn)
        task_status_btn.setImageResource(Helpers.status2icon(status))
        task_status_btn.setOnClickListener {
            listener?.onStatus(json)
        }

        // clear previous contents
        (views.findViewById<View>(R.id.task_annotations) as ViewGroup).removeAllViews()
        (views.findViewById<View>(R.id.task_labels_left) as ViewGroup).removeAllViews()
        (views.findViewById<View>(R.id.task_labels_right) as ViewGroup).removeAllViews()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        bindLongCopyText(json, views.findViewById(R.id.task_description),
                json.optString("description"))
        for ((key, value) in info.fields) {
            when (key.toLowerCase()) {
                "description" -> {
                    // Set desc
                    val desc = views.findViewById<TextView>(R.id.task_description)
                    desc.text = json.optString("description")
                    val annotations = json.optJSONArray("annotations")
                    if (null != annotations && annotations.length() > 0) {
                        // Have annotations
                        if ("" == value) {
                            var i = 0
                            while (i < annotations.length()) {
                                val ann = annotations.optJSONObject(i)
                                val annView = View.inflate(context, R.layout.item_one_annotation, null)
                                val task_ann_text_view = annView.findViewById<TextView>(R.id.task_ann_text)
                                val task_ann_text = ann.optString("description",
                                        "Untitled")
                                task_ann_text_view.text = task_ann_text
                                bindLongCopyText(json, task_ann_text_view,
                                        task_ann_text)
                                (annView.findViewById<View>(R.id.task_ann_date) as TextView).text = Helpers.asDate(ann.optString("entry"), sharedPref)
                                val deleteBtn = annView.findViewById<View>(R.id.task_ann_delete_btn)
                                deleteBtn.visibility = View.VISIBLE
                                deleteBtn.setOnClickListener { v: View? ->
                                    AlertDialog.Builder(context)
                                            .setTitle("Remove annotation")
                                            .setMessage("Remove annotation '$ann'?") //                                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton(android.R.string.yes) { dialog: DialogInterface?, whichButton: Int ->
                                                listener?.onDenotate(json, ann)
                                            }
                                            .setNegativeButton(android.R.string.no, null).show()
                                }
                                val insertPoint = views.findViewById<ViewGroup>(R.id.task_annotations)
                                insertPoint.addView(annView, 0, ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT))
                                i++
                            }
                        }
                    }
                }
                "id" -> (views.findViewById<View>(R.id.task_id) as TextView).text = String.format("[%d]", json.optInt("id", -1))
                "priority" -> {
                    val index = info.priorities.indexOf(json.optString("priority", ""))
                    val pb_priority = views.findViewById<ProgressBar>(R.id.task_priority)
                    if (index == -1) {
                        pb_priority.max = 0
                        pb_priority.progress = 0
                    } else {
                        pb_priority.max = info.priorities.size - 1
                        pb_priority.progress = info.priorities.size - index - 1
                    }
                }
                "urgency" -> {
                    val pb_urgency = views.findViewById<ProgressBar>(R.id.task_urgency)
                    pb_urgency.max = urgMax - urgMin
                    pb_urgency.progress = json.optDouble("urgency").roundToInt() - urgMin
                }
                "due" -> Helpers.addLabelWithInsert(context, views, "due", true, R.drawable.ic_label_due,
                        Helpers.asDate(json.optString("due"), sharedPref))
                "wait" -> Helpers.addLabelWithInsert(context, views, "wait", true, R.drawable.ic_label_wait,
                        Helpers.asDate(json.optString("wait"), sharedPref))
                "scheduled" -> Helpers.addLabelWithInsert(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
                        Helpers.asDate(json.optString("scheduled"), sharedPref))
                "recur" -> {
                    var recur = json.optString("recur")
                    if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
                        val until = Helpers.asDate(json.optString("until"), sharedPref)
                        if (!TextUtils.isEmpty(until)) {
                            recur += String.format(" ~ %s", until)
                        }
                    }
                    Helpers.addLabelWithInsert(context, views, "recur", true, R.drawable.ic_label_recur, recur)
                }
                "project" -> Helpers.addLabelWithInsert(context, views, "project", false, R.drawable.ic_label_project,
                        json.optString("project"))
                "tags" -> Helpers.addLabelWithInsert(context, views, "tags", false, R.drawable.ic_label_tags, Helpers.join(", ", Helpers.array2List(
                        json.optJSONArray("tags"))))
                "start" -> {
                    val started = Helpers.asDate(json.optString("start"), sharedPref)
                    val isStarted = !TextUtils.isEmpty(started)
                    if ("pending".equals(status, ignoreCase = true)) { // Can be started/stopped
                        val start_stop_btn = views.findViewById<View>(R.id.task_start_stop_btn)
                        start_stop_btn.visibility = View.VISIBLE
                        start_stop_btn.setOnClickListener { v: View? ->
                            listener?.onStartStop(json)
                        }
                        (start_stop_btn as ImageView).setImageResource(if (isStarted) R.drawable.ic_action_stop else R.drawable.ic_action_start)
                    }
                }
            }
        }
        views.findViewById<View>(R.id.task_edit_btn).setOnClickListener { v: View? ->
            listener?.onEdit(json)
        }
        views.findViewById<View>(R.id.task_delete_btn).setOnClickListener { v: View? ->
            AlertDialog.Builder(context)
                    .setTitle("Delete task")
                    .setMessage("Do you really want to delete this task?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        listener?.onDelete(json)
                    }
                    .setNegativeButton(android.R.string.no, null).show()
        }
        views.findViewById<View>(R.id.task_annotate_btn).setOnClickListener {
            listener?.onAnnotate(json)
        }
        return views
    }

    private fun bindLongCopyText(json: JSONObject?, view: View?, text: String) {
        if (TextUtils.isEmpty(text) || view == null || json == null) {
            return
        }
        view.setOnLongClickListener(
                View.OnLongClickListener {
                    logger.d("Long click on description", json)
                    if (null != listener) listener!!.onCopyText(json, text)
                    true
                })
    }

    // NOTE: Make accessible with short name
    private interface Swipeable : SwipeableItemConstants
    interface EventListener {
        fun onItemRemoved(position: Int)
        fun onItemPinned(position: Int)
        fun onItemViewClicked(v: View?, pinned: Boolean)
    }

    interface ItemListener {
        fun onEdit(json: JSONObject?)
        fun onStatus(json: JSONObject?)
        fun onDelete(json: JSONObject?)
        fun onAnnotate(json: JSONObject?)
        fun onStartStop(json: JSONObject?)
        fun onDenotate(json: JSONObject?, annJson: JSONObject?)
        fun onCopyText(json: JSONObject?, text: String?)
        fun onLabelClick(json: JSONObject?, type: String?, longClick: Boolean)
    }


    interface MyViewHolder {
        val mTextView: TextView
    }

    class MyTitleViewHolder(v: View) : MyViewHolder, RecyclerView.ViewHolder(v) {
        override val mTextView: TextView = v.findViewById(R.id.task_description)
    }

    class MySwipeableViewHolder(v: View) : MyViewHolder, AbstractSwipeableItemViewHolder(v) {
        override val mTextView: TextView = v.findViewById(R.id.task_description)
        var mContainer: FrameLayout = v.findViewById(R.id.container)
        var mAnnoFlag: ImageView = v.findViewById(R.id.task_annotations_flag)
        var mStartedFlag: TextView = v.findViewById(R.id.task_started_flag)
        var mCalLabelContainer: LinearLayout = v.findViewById(R.id.cal_label_container)
        var mTagsLabelContainer: LinearLayout = v.findViewById(R.id.tags_label_container)

        override fun getSwipeableContainerView(): View {
            return mContainer
        }

    }

    private class SwipeLeftResultAction internal constructor(private val mAdapter:
                                                             SwipeListAdapter, private val mPosition: Int) : SwipeResultActionMoveToSwipedDirection() {
        private var mSetPinned = false
        override fun onPerformAction() {
            super.onPerformAction()
            val item = mAdapter.mProvider.getItem(mPosition)
            if (!item.isPinned) {
                item.isPinned = true
                mAdapter.notifyItemChanged(mPosition)
                mSetPinned = true
            }
        }

        override fun onSlideAnimationEnd() {
            super.onSlideAnimationEnd()
            if (mSetPinned && mAdapter.eventListener != null) {
//                mAdapter.mEventListener.onItemPinned(mPosition);
            }
        }

        override fun onCleanUp() {
            super.onCleanUp()
            // clear the references
//            mAdapter = null
        }

    }

    private class SwipeRightResultAction internal constructor(private val mAdapter:
                                                              SwipeListAdapter, private val mPosition: Int) : SwipeResultActionRemoveItem() {
        override fun onPerformAction() {
            super.onPerformAction()
            mAdapter.listener?.let {
                it.onStatus(mAdapter.mProvider.getItem(mPosition).json)
                // mark as done
                mAdapter.mProvider.removeItem(mPosition)
                mAdapter.notifyItemRemoved(mPosition)
            }
        }

        override fun onSlideAnimationEnd() {
            super.onSlideAnimationEnd()
            if (mAdapter.eventListener != null) {
//                mAdapter.mEventListener.onItemRemoved(mPosition);
            }
        }

        override fun onCleanUp() {
            super.onCleanUp()
            // clear the references
//            mAdapter = null
        }

    }

    private class UnpinResultAction internal constructor(private val mAdapter:
                                                         SwipeListAdapter, private val mPosition: Int) : SwipeResultActionDefault() {
        override fun onPerformAction() {
            super.onPerformAction()
            val item = mAdapter.mProvider.getItem(mPosition)
            if (item.isPinned) {
                item.isPinned = false
                mAdapter.notifyItemChanged(mPosition)
            }
        }

        override fun onCleanUp() {
            super.onCleanUp()
            // clear the references
//            mAdapter = null
        }
    }

    companion object {
        private const val TAG = "MySwipeableItemAdapter"
        var logger = Logger.forClass(SwipeListAdapter::class.java)
    }

    init {
        //        mItemViewOnClickListener = v -> onItemViewClick(v);
        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }
}