package soraxas.taskw.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.expandable.ExpandableItemState
import com.h6ah4i.android.widget.advrecyclerview.expandable.ExpandableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.expandable.ExpandableSwipeableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemState
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultAction
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionDefault
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionMoveToSwipedDirection
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemAdapter
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
import java.text.DecimalFormat
import java.util.*
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
class SwipeListAdapter(private val mProvider: TaskwDataProvider,
                       private val mExpandableItemManager: RecyclerViewExpandableItemManager,
                       private val mainList: MainList) :
        AbstractExpandableItemAdapter<SwipeListAdapter.MyGroupViewHolder, SwipeListAdapter.MyChildViewHolder>(),
        ExpandableSwipeableItemAdapter<SwipeListAdapter.MyGroupViewHolder, SwipeListAdapter.MyChildViewHolder> {
    lateinit var info: ReportInfo
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    @JvmField
    var updateCurTaskDetailView: Runnable? = null

    @JvmField
    var curTaskDetailViewDialog: MaterialDialog? = null

    @JvmField
    var listener: ItemListener? = null
    private val mItemViewOnClickListener: View.OnClickListener? = null
    private val mSwipeableViewContainerOnClickListener: View.OnClickListener? = null
    private val urgMin = 0
    private val urgMax = 0

    private fun onItemViewClick(v: View, groupPosition: Int, childPosition: Int, pinned: Boolean) {
        val json = mProvider.getItem(groupPosition, childPosition).json
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
        curTaskDetailViewDialog = showD(context, taskDetailView)
    }

//    override fun getItemId(position: Int): Long {
//        return mProvider.getItem(position).id
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return mProvider.getItem(position).viewType
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        val inflater = LayoutInflater.from(parent.context)
//        return when (viewType) {
//            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_HEADER -> {
//                MyTitleViewHolder(inflater.inflate(R.layout.list_section_header, parent, false))
//            }
//            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_ITEM -> {
//                MySwipeableViewHolder(inflater.inflate(R.layout
//                        .list_item_draggable, parent, false))
//            }
//            else -> {
//                throw IllegalStateException("Unexpected viewType (= $viewType)")
//            }
//        }
//    }

    private fun update_label_left_right_attr(view: View) {
        (view.findViewById<View>(R.id.label_text) as TextView).textSize = 12f
        val icon = view.findViewById<ImageView>(R.id.label_icon)
        icon.scaleX = 0.7f
        icon.scaleY = 0.7f
    }

//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val item = mProvider.getItem(position)
//        when (holder.itemViewType) {
//            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_HEADER -> {
//                val header_item = mProvider.getItem(position)
//                holder as MyTitleViewHolder
//                // set text
//                holder.mTextView.text = header_item.text
//            }
//            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_ITEM -> {
//                holder as MySwipeableViewHolder
//                // cleanup
//                (holder.mCalLabelContainer as ViewGroup).removeAllViews()
//                (holder.mTagsLabelContainer as ViewGroup).removeAllViews()
//                // set listeners
//                // (if the item is *pinned*, click event comes to the itemView)
////                holder.itemView.setOnClickListener(mItemViewOnClickListener);
//                holder.itemView.setOnClickListener { v: View -> onItemViewClick(v, position, true) }
//                // (if the item is *not pinned*, click event comes to the mContainer)
//                holder.mContainer.setOnClickListener { v: View ->
//                    onItemViewClick(v, position, false)
//                }
//                //                holder.mContainer.setOnClickListener(mSwipeableViewContainerOnClickListener);
//
//                // set text
//                val formatter = DecimalFormat("#0.0")
//                holder.mTextView.text = item.text
//                holder.mUrgencyText.text = formatter.format(item.urgency)
//                // set all other attributes
//                holder.mAnnoFlag.visibility = item.annoVisibility()
//                holder.mStartedFlag.visibility = item.startedVisibility()
//
//                // add calendar labels
//                val viewContext = holder.mTextView.context
//                val sharedPref = PreferenceManager.getDefaultSharedPreferences(viewContext)
//                val calLabels = item.buildCalLabels(viewContext, sharedPref)
//                calLabels.forEach { v ->
//                    update_label_left_right_attr(v)
//                    holder.mCalLabelContainer.addView(v)
//                }
//                // add tags labels
//                val tags = item.json!!.optJSONArray("tags")
//                tags?.let {
//                    val v = Helpers.createLabel(viewContext, false,
//                            R.drawable.ic_label_tags, Helpers.join("m", Helpers
//                            .array2List(it)))
//                    update_label_left_right_attr(v)
//                    holder.mTagsLabelContainer.addView(v)
//                }
//
//
//                // set background resource (target view ID: container)
//                val swipeState = holder.swipeState
//                if (swipeState.isUpdated) {
//                    val bgResId: Int = when {
//                        swipeState.isActive ->
//                            R.drawable.bg_item_swiping_active_state
//                        swipeState.isSwiping ->
//                            R.drawable.bg_item_swiping_state
//                        else ->
//                            R.drawable.bg_item_normal_state
//                    }
//                    holder.mContainer.setBackgroundResource(bgResId)
//                }
//
//                // set swiping properties
////                holder.swipeItemHorizontalSlideAmount = if (item.isPinned) SwipeableItemConstants
////                        .OUTSIDE_OF_THE_WINDOW_LEFT else 0f
//
//
//                // set swiping properties
////                holder.maxLeftSwipeAmount = -0.5f
//                holder.maxLeftSwipeAmount = -1f
//                holder.maxRightSwipeAmount = 0.5f
//                holder.swipeItemHorizontalSlideAmount = if (item.isPinned) -0.5f
//                else 0f
//            }
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return mProvider.count
//    }

//    override fun onGetSwipeReactionType(holder: RecyclerView.ViewHolder, position: Int, x: Int, y: Int): Int {
////        return Swipeable.REACTION_CAN_SWIPE_LEFT | Swipeable.REACTION_MASK_START_SWIPE_LEFT |
////                Swipeable.REACTION_CAN_SWIPE_RIGHT | Swipeable.REACTION_MASK_START_SWIPE_RIGHT |
////                Swipeable.REACTION_START_SWIPE_ON_LONG_PRESS;
//        return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
//    }
//
//    override fun onSwipeItemStarted(holder: RecyclerView.ViewHolder, position: Int) {
////        notifyDataSetChanged()
//    }
//
//    override fun onSetSwipeBackground(holder: RecyclerView.ViewHolder, position: Int, type: Int) {
//        var bgRes = 0
//        when (type) {
//            SwipeableItemConstants.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_neutral
//            SwipeableItemConstants.DRAWABLE_SWIPE_LEFT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_left
//            SwipeableItemConstants.DRAWABLE_SWIPE_RIGHT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_right
//        }
//        holder.itemView.setBackgroundResource(bgRes)
//    }
//
//    override fun onSwipeItem(holder: RecyclerView.ViewHolder, position: Int, result: Int): SwipeResultAction? {
//        Log.d(TAG, "onSwipeItem(position = $position, result = $result)")
//        val onPinActions = { taskData: TaskwDataProvider.TaskwData ->
//            mainList.togglePinnedTask(taskData.uuid)
//            mainList.reload()
//            true
//        }
//
//        return when (result) {
//            SwipeableItemConstants.RESULT_SWIPED_RIGHT -> if (mProvider.getItem(position).isPinned) {
//                // pinned --- back to default position
//                UnpinResultAction(this, position)
//            } else {
//                // not pinned --- remove
//                SwipeRightResultAction(this, position)
//            }
//            SwipeableItemConstants.RESULT_SWIPED_LEFT -> SwipeLeftResultAction(this,
//                    position, onPinActions)
//            SwipeableItemConstants.RESULT_CANCELED -> if (position != RecyclerView.NO_POSITION) {
//                UnpinResultAction(this, position)
//            } else {
//                null
//            }
//            else -> if (position != RecyclerView.NO_POSITION) {
//                UnpinResultAction(this, position)
//            } else {
//                null
//            }
//        }
//    }

    private fun populateTaskDetailView(json: JSONObject?, context: Context?, view: View): View {
        if (json == null && context != null) return view
        val urgMin = 0
        val urgMax = 100
        // TODO fix me

//        Snackbar.make(view, "Subscription Deleted", Snackbar.LENGTH_LONG)
//                .setAction("Undo",  a -> {
////                            activeSubs.add(position-1, tmp)
////                            adapter!!.notifyDataSetChanged()
//                });

        ///////////////////////////
        val status = json!!.optString("status", "pending")
        val taskStatusBtn = view.findViewById<ImageView>(R.id.task_status_btn)
        taskStatusBtn.setImageResource(Helpers.status2icon(status))
        taskStatusBtn.setOnClickListener {
            listener?.onStatus(json, view = view)
        }

        // clear previous contents
        (view.findViewById<View>(R.id.task_annotations) as ViewGroup).removeAllViews()
        (view.findViewById<View>(R.id.task_labels_left) as ViewGroup).removeAllViews()
        (view.findViewById<View>(R.id.task_labels_right) as ViewGroup).removeAllViews()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        bindLongCopyText(json, view.findViewById(R.id.task_description),
                json.optString("description"))
        for ((key, value) in info.fields) {
            when (key.toLowerCase()) {
                "description" -> {
                    // Set desc
                    val desc = view.findViewById<TextView>(R.id.task_description)
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
                                                listener?.onDenotate(json, ann, view
                                                = v)
                                            }
                                            .setNegativeButton(android.R.string.no, null).show()
                                }
                                val insertPoint = view.findViewById<ViewGroup>(R.id.task_annotations)
                                insertPoint.addView(annView, 0, ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT))
                                i++
                            }
                        }
                    }
                }
                "id" -> (view.findViewById<View>(R.id.task_id) as TextView).text = String.format("[%d]", json.optInt("id", -1))
                "priority" -> {
                    val index = info.priorities.indexOf(json.optString("priority", ""))
                    val pb_priority = view.findViewById<ProgressBar>(R.id.task_priority)
                    if (index == -1) {
                        pb_priority.max = 0
                        pb_priority.progress = 0
                    } else {
                        pb_priority.max = info.priorities.size - 1
                        pb_priority.progress = info.priorities.size - index - 1
                    }
                }
                "urgency" -> {
                    val pb_urgency = view.findViewById<ProgressBar>(R.id.task_urgency)
                    pb_urgency.max = urgMax - urgMin
                    pb_urgency.progress = json.optDouble("urgency").roundToInt() - urgMin
                }
                "due" -> Helpers.addLabelWithInsert(context, view, "due", true, R.drawable.ic_label_due,
                        Helpers.asDate(json.optString("due"), sharedPref))
                "wait" -> Helpers.addLabelWithInsert(context, view, "wait", true, R.drawable.ic_label_wait,
                        Helpers.asDate(json.optString("wait"), sharedPref))
                "scheduled" -> Helpers.addLabelWithInsert(context, view, "scheduled", true, R.drawable.ic_label_scheduled,
                        Helpers.asDate(json.optString("scheduled"), sharedPref))
                "recur" -> {
                    var recur = json.optString("recur")
                    if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
                        val until = Helpers.asDate(json.optString("until"), sharedPref)
                        if (!TextUtils.isEmpty(until)) {
                            recur += String.format(" ~ %s", until)
                        }
                    }
                    Helpers.addLabelWithInsert(context, view, "recur", true, R.drawable.ic_label_recur, recur)
                }
                "project" -> Helpers.addLabelWithInsert(context, view, "project", false, R.drawable.ic_label_project,
                        json.optString("project"))
                "tags" -> Helpers.addLabelWithInsert(context, view, "tags", false, R.drawable.ic_label_tags, Helpers.join(", ", Helpers.array2List(
                        json.optJSONArray("tags"))))
                "start" -> {
                    val started = Helpers.asDate(json.optString("start"), sharedPref)
                    val isStarted = !TextUtils.isEmpty(started)
                    if ("pending".equals(status, ignoreCase = true)) { // Can be started/stopped
                        val start_stop_btn = view.findViewById<View>(R.id.task_start_stop_btn)
                        start_stop_btn.visibility = View.VISIBLE
                        start_stop_btn.setOnClickListener { v: View? ->
                            listener?.onStartStop(json, view = v)
                        }
                        (start_stop_btn as ImageView).setImageResource(if (isStarted) R.drawable.ic_action_stop else R.drawable.ic_action_start)
                    }
                }
            }
        }
        view.findViewById<View>(R.id.task_edit_btn).setOnClickListener { v: View? ->
            listener?.onEdit(json, view = v)
        }
        view.findViewById<View>(R.id.task_delete_btn).setOnClickListener { v: View? ->
            AlertDialog.Builder(context)
                    .setTitle("Delete task")
                    .setMessage("Do you really want to delete this task?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        listener?.onDelete(json, view = null)
                        curTaskDetailViewDialog?.dismiss()
                    }
                    .setNegativeButton(android.R.string.no, null).show()
        }
        view.findViewById<View>(R.id.task_annotate_btn).setOnClickListener { v: View? ->
            listener?.onAnnotate(json, view = v)
        }
        return view
    }

    private fun bindLongCopyText(json: JSONObject?, view: View?, text: String) {
        if (TextUtils.isEmpty(text) || view == null || json == null) {
            return
        }
        view.setOnLongClickListener(
                View.OnLongClickListener {
                    logger.d("Long click on description", json)
                    if (null != listener) listener!!.onCopyText(json, text, view =
                    mainList.view)
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
        fun onEdit(json: JSONObject, view: View?)
        fun onStatus(json: JSONObject, view: View?)
        fun onDelete(json: JSONObject, view: View?)
        fun onAnnotate(json: JSONObject, view: View?)
        fun onStartStop(json: JSONObject, view: View?)
        fun onDenotate(json: JSONObject, annJson: JSONObject, view: View?)
        fun onCopyText(json: JSONObject, text: String, view: View?)
        fun onLabelClick(json: JSONObject, type: String, longClick: Boolean, view:
        View?)
    }

    abstract class MyBaseViewHolder(v: View) : AbstractDraggableSwipeableItemViewHolder(v), ExpandableItemViewHolder {
        var mContainer: FrameLayout = v.findViewById(R.id.container)


        //        var mDragHandle: View = v.findViewById(android.R.id.drag_handle)
        private val mExpandState: ExpandableItemState = ExpandableItemState()

        override fun getSwipeableContainerView(): View {
            return mContainer
        }

        override fun getExpandStateFlags(): Int {
            return mExpandState.flags
        }

        override fun setExpandStateFlags(flags: Int) {
            mExpandState.flags = flags
        }

        override fun getExpandState(): ExpandableItemState {
            return mExpandState
        }

    }


    class MyGroupViewHolder(v: View) : MyBaseViewHolder(v) {
        val mProjectName: TextView = v.findViewById(R.id.task_project_name)
        val mProjectTasksCount: TextView = v.findViewById(R.id.task_project_tasks_count)
        val mIndicator: ImageView = v.findViewById(R.id.indicator)
        val mTopLineHighlight: ImageView = v.findViewById(R.id.top_line_highlight)

//        var mIndicator: ExpandableItemIndicator
//
//        init {
//            mIndicator = v.findViewById(android.R.id.indicator)
//        }
    }

    class MyChildViewHolder(v: View) : MyBaseViewHolder(v) {
        val mTextView: TextView = v.findViewById(R.id.task_description)
        var mAnnoFlag: ImageView = v.findViewById(R.id.task_annotations_flag)
        var mStartedFlag: TextView = v.findViewById(R.id.task_started_flag)
        var mCalLabelContainer: LinearLayout = v.findViewById(R.id.cal_label_container)
        var mTagsLabelContainer: LinearLayout = v.findViewById(R.id.tags_label_container)
        var mUrgencyText: TextView = v.findViewById(R.id.task_urgency_text)
    }

//    class MySwipeableViewHolder(v: View) : AbstractSwipeableItemViewHolder(v) {
//        override val mTextView: TextView = v.findViewById(R.id.task_description)
//        var mContainer: FrameLayout = v.findViewById(R.id.container)
//        var mAnnoFlag: ImageView = v.findViewById(R.id.task_annotations_flag)
//        var mStartedFlag: TextView = v.findViewById(R.id.task_started_flag)
//        var mCalLabelContainer: LinearLayout = v.findViewById(R.id.cal_label_container)
//        var mTagsLabelContainer: LinearLayout = v.findViewById(R.id.tags_label_container)
//        var mUrgencyText: TextView = v.findViewById(R.id.task_urgency_text)
//
//        override fun getSwipeableContainerView(): View {
//            return mContainer
//        }
//
//    }

    private class SwipeLeftResultAction internal constructor(
            private val mAdapter: SwipeListAdapter,
            private val groupPos: Int,
            private val itemPos: Int,
            private val onPinAction: (TaskwDataProvider.TaskwData) -> Boolean
    ) :
            SwipeResultActionMoveToSwipedDirection() {
        private var mSetPinned = false
        override fun onPerformAction() {
            super.onPerformAction()
            onPinAction(mAdapter.mProvider.getItem(groupPos, itemPos))

//
//            onPinAction(item)
//            mAdapter.mProvider.removeItem(mPosition)


//            mAdapter.notifyDataSetChanged()

//            if (!item.isPinned) {
//                item.isPinned = true
//                mAdapter.notifyItemChanged(mPosition)
//                mSetPinned = true
//            }
        }

        override fun getResultActionType(): Int {
            return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM
//            return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION
        }

        override fun onSlideAnimationEnd() {
            super.onSlideAnimationEnd()
//            if (mSetPinned && mAdapter.eventListener != null) {
//                mAdapter.mEventListener.onItemPinned(mPosition);
//            }
        }

    }

    private class SwipeRightResultAction internal constructor(
            private val mAdapter: SwipeListAdapter,
            private val groupPosition: Int,
            private val childPosition: Int) :
            SwipeResultActionRemoveItem() {
        override fun onPerformAction() {
            super.onPerformAction()
            mAdapter.listener?.let {
                it.onStatus(mAdapter.mProvider.getItem(groupPosition, childPosition)
                        .json, view = mAdapter.mainList.view)
                // mark as done
                mAdapter.mProvider.removeItem(groupPosition, childPosition)
                mAdapter.mExpandableItemManager.notifyChildItemRemoved(groupPosition,
                        childPosition)
//                mAdapter.notifyItemRemoved(mPosition)
            }
        }

        override fun onSlideAnimationEnd() {
            super.onSlideAnimationEnd()
//            if (mAdapter.eventListener != null) {
//                mAdapter.mEventListener.onItemRemoved(mPosition);
//            }
        }

    }

    private class UnpinResultAction internal constructor(
            private val mAdapter: SwipeListAdapter,
            private val groupPosition: Int,
            private val childPosition: Int) :
            SwipeResultActionDefault() {
        override fun onPerformAction() {
            super.onPerformAction()
            val item = mAdapter.mProvider.getItem(groupPosition, childPosition)
            if (item.isPinned) {
                item.isPinned = false
//                mAdapter.notifyItemChanged(mPosition)
            }
        }

    }

    companion object {
        private const val TAG = "MySwipeableItemAdapter"
        var logger = Logger.forClass(SwipeListAdapter::class.java)
    }

    init {
        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true)
    }

    override fun onCheckCanExpandOrCollapseGroup(holder: MyGroupViewHolder, groupPosition: Int, x: Int, y: Int, expand: Boolean): Boolean {
        return true
        // check the item is *not* pinned
//        if (mProvider.getGroupItem(groupPosition).isPinned()) {
//            // return false to raise View.OnClickListener#onClick() event
//            return false
//        }
//
//        // check is enabled
//        if (!(holder.itemView.isEnabled && holder.itemView.isClickable)) {
//            return false
//        }
//        val containerView: View = holder.mContainer
//        val dragHandleView: View = holder.mDragHandle
//        val offsetX = containerView.left + (containerView.translationX + 0.5f).toInt()
//        val offsetY = containerView.top + (containerView.translationY + 0.5f).toInt()
//        return !ViewUtils.hitTest(dragHandleView, x - offsetX, y - offsetY)
//        TODO("Not yet implemented")

        return holder.itemView.isEnabled && holder.itemView.isClickable;
        return true
    }

    override fun getInitialGroupExpandedState(groupPosition: Int): Boolean {
        // NOTE:
        // This method can also be used to control initial state of group items.
        // Make sure to call `setDefaultGroupsExpandedState(false)` to take effect.
        val collapsedGroup = mainList.getCollapsedGroups()
        if (mProvider.getGroup(groupPosition).groupName in collapsedGroup)
            return false
        return true
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): MyChildViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MyChildViewHolder(inflater.inflate(R.layout
                .list_item_draggable, parent, false))
//        return when (viewType) {
//            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_HEADER -> {
//                MyTitleViewHolder(inflater.inflate(R.layout.list_section_header, parent, false))
//            }
//            TaskwDataProvider.ITEM_VIEW_TYPE_SECTION_ITEM -> {
//                MySwipeableViewHolder(inflater.inflate(R.layout
//                        .list_item_draggable, parent, false))
//            }
//            else -> {
//                throw IllegalStateException("Unexpected viewType (= $viewType)")
//            }
//        }
    }

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): MyGroupViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MyGroupViewHolder(inflater.inflate(R.layout.list_section_header,
                parent, false))
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return mProvider.getItem(groupPosition, childPosition).id
    }

    override fun getGroupId(groupPosition: Int): Long {
        return mProvider.getGroup(groupPosition).id
    }

    override fun getChildCount(groupPosition: Int): Int {
        return mProvider.getItemSize(groupPosition)
    }

    override fun getGroupCount(): Int {
        return mProvider.getGroupSize()
    }

    override fun onBindChildViewHolder(holder: MyChildViewHolder, groupPosition: Int, childPosition: Int, viewType: Int) {
        // cleanup
        (holder.mCalLabelContainer as ViewGroup).removeAllViews()
        (holder.mTagsLabelContainer as ViewGroup).removeAllViews()
        // set listeners
        // (if the item is *pinned*, click event comes to the itemView)
//                holder.itemView.setOnClickListener(mItemViewOnClickListener);
        holder.itemView.setOnClickListener { v: View -> onItemViewClick(v, groupPosition, childPosition, true) }
        // (if the item is *not pinned*, click event comes to the mContainer)
        holder.mContainer.setOnClickListener { v: View ->
            onItemViewClick(v, groupPosition, childPosition, false)
        }
        //                holder.mContainer.setOnClickListener(mSwipeableViewContainerOnClickListener);

        val item = mProvider.getItem(groupPosition, childPosition)
        // set text
        val formatter = DecimalFormat("#0.0")
        holder.mTextView.text = item.text
        holder.mUrgencyText.text = formatter.format(item.urgency)
        // set all other attributes
        holder.mAnnoFlag.visibility = item.annoVisibility
        holder.mStartedFlag.visibility = item.startedVisibility

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
//                holder.swipeItemHorizontalSlideAmount = if (item.isPinned) SwipeableItemConstants
//                        .OUTSIDE_OF_THE_WINDOW_LEFT else 0f


        // set swiping properties
//                holder.maxLeftSwipeAmount = -0.5f
        holder.maxLeftSwipeAmount = -1f
        holder.maxRightSwipeAmount = 0.5f
        holder.swipeItemHorizontalSlideAmount = if (item.isPinned) -0.5f
        else 0f
//        TODO("Not yet implemented")
    }

    private fun interpolate(a: Float, b: Float, proportion: Float): Float {
        return a + (b - a) * proportion
    }

    /** Returns an interpoloated color, between `a` and `b`  */
    private fun interpolateColor(a: Int, b: Int, proportion: Float): Int {
        val hsva = FloatArray(3)
        val hsvb = FloatArray(3)
        Color.colorToHSV(a, hsva)
        Color.colorToHSV(b, hsvb)
        for (i in 0..2) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion)
        }
        return Color.HSVToColor(hsvb)
    }

    override fun onBindGroupViewHolder(holder: MyGroupViewHolder, groupPosition: Int, viewType: Int) {
        val headerItem = mProvider.getGroup(groupPosition)
        holder.mProjectName.text = headerItem.text
        holder.mProjectTasksCount.text = "(${headerItem.groupItemNum})"
        holder.mContainer.elevation = 2f

        val rnd = Random()
        val color: Int = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
//        val oriColor = (holder.mContainer.background as ColorDrawable).color
        val oriColor = Color.argb(255, 255, 255, 255)

        holder.mContainer.setBackgroundColor(ColorUtils.blendARGB(color, oriColor, 0.5f))
        holder.mTopLineHighlight.setBackgroundColor(color)

//        holder.itemView.setOnClickListener(mItemViewOnClickListener);

        // set background resource (target view ID: container)
        val dragState: DraggableItemState = holder.dragState
        val expandState: ExpandableItemState = holder.expandState
        val swipeState: SwipeableItemState = holder.swipeState
        if (dragState.isUpdated || expandState.isUpdated || swipeState.isUpdated) {
            val bgResId: Int
            val animateIndicator = expandState.hasExpandedStateChanged()
//            if (dragState.isActive) {
//                bgResId = android.R.drawable.bg_group_item_dragging_active_state
//
//                // need to clear drawable state here to get correct appearance of the dragging item.
//                DrawableUtils.clearState(holder.mContainer.foreground)
//            } else if (dragState.isDragging) {
//                bgResId = android.R.drawable.bg_group_item_dragging_statec
//            } else if (swipeState.isActive) {
//                bgResId = android.R.drawable.bg_group_item_swiping_active_state
//            } else if (swipeState.isSwiping) {
//                bgResId = android.R.drawable.bg_group_item_swiping_state
//            } else if (expandState.isExpanded) {
//                bgResId = android.R.drawable.bg_group_item_expanded_state
//            } else {
//                bgResId = android.R.drawable.bg_group_item_normal_state
//            }
//            holder.mContainer.setBackgroundResource(bgResId)

            // animate indicator
            holder.mIndicator.setImageResource(
                    if (expandState.isExpanded)
                        R.drawable.ic_expand_more_to_expand_less
                    else
                        R.drawable.ic_expand_less_to_expand_more
            );
            (holder.mIndicator.drawable as Animatable).start();
        }

        // set swiping properties
//        holder.setSwipeItemHorizontalSlideAmount(
//                if (item.isPinned()) Swipeable.OUTSIDE_OF_THE_WINDOW_LEFT else 0)
    }

    override fun onGetChildItemSwipeReactionType(holder: MyChildViewHolder, groupPosition: Int, childPosition: Int, x: Int, y: Int): Int {
//        return Swipeable.REACTION_CAN_SWIPE_LEFT | Swipeable.REACTION_MASK_START_SWIPE_LEFT |
//                Swipeable.REACTION_CAN_SWIPE_RIGHT | Swipeable.REACTION_MASK_START_SWIPE_RIGHT |
//                Swipeable.REACTION_START_SWIPE_ON_LONG_PRESS;
        return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
    }

    override fun onGetGroupItemSwipeReactionType(holder: MyGroupViewHolder, groupPosition: Int, x: Int, y: Int): Int {
//        return Swipeable.REACTION_CAN_SWIPE_LEFT | Swipeable.REACTION_MASK_START_SWIPE_LEFT |
//                Swipeable.REACTION_CAN_SWIPE_RIGHT | Swipeable.REACTION_MASK_START_SWIPE_RIGHT |
//                Swipeable.REACTION_START_SWIPE_ON_LONG_PRESS;
        return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H
    }

    override fun onSetChildItemSwipeBackground(holder: MyChildViewHolder, groupPosition: Int, childPosition: Int, type: Int) {
        var bgRes = 0
        when (type) {
            SwipeableItemConstants.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_neutral
            SwipeableItemConstants.DRAWABLE_SWIPE_LEFT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_left
            SwipeableItemConstants.DRAWABLE_SWIPE_RIGHT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_right
        }
        holder.itemView.setBackgroundResource(bgRes)
    }

    override fun onSetGroupItemSwipeBackground(holder: MyGroupViewHolder, groupPosition: Int, type: Int) {
        var bgRes = 0
        when (type) {
            SwipeableItemConstants.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_neutral
            SwipeableItemConstants.DRAWABLE_SWIPE_LEFT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_left
            SwipeableItemConstants.DRAWABLE_SWIPE_RIGHT_BACKGROUND -> bgRes = R.drawable.bg_swipe_item_right
        }
        holder.itemView.setBackgroundResource(bgRes)
    }

    override fun onSwipeChildItemStarted(holder: MyChildViewHolder, groupPosition: Int, childPosition: Int) {
//        notifyDataSetChanged()
    }

    override fun onSwipeGroupItemStarted(holder: MyGroupViewHolder, groupPosition: Int) {
//        notifyDataSetChanged()
    }

    override fun onSwipeChildItem(holder: MyChildViewHolder, groupPosition: Int,
                                  childPosition: Int, result: Int): SwipeResultAction? {
        val onPinActions = { taskData: TaskwDataProvider.TaskwData ->
            mainList.togglePinnedTask(taskData.uuid)
            mainList.reload()
            true
        }

        return when (result) {
            SwipeableItemConstants.RESULT_SWIPED_RIGHT -> if (mProvider
                            .getItem(groupPosition, childPosition)
                            .isPinned) {
                // pinned --- back to default position
                UnpinResultAction(this, groupPosition, childPosition)
            } else {
                // not pinned --- remove
                SwipeRightResultAction(this, groupPosition, childPosition)
            }
            SwipeableItemConstants.RESULT_SWIPED_LEFT -> SwipeLeftResultAction(this,
                    groupPosition,
                    childPosition, onPinActions)
            SwipeableItemConstants.RESULT_CANCELED -> if (childPosition != RecyclerView
                            .NO_POSITION && childPosition != RecyclerView
                            .NO_POSITION) {
                UnpinResultAction(this, groupPosition, childPosition)
            } else {
                null
            }
            else -> if (childPosition != RecyclerView.NO_POSITION) {
                UnpinResultAction(this, groupPosition, childPosition)
            } else {
                null
            }
        }
    }

    override fun onSwipeGroupItem(holder: MyGroupViewHolder, groupPosition: Int,
                                  result: Int): SwipeResultAction? {
        val onPinActions = { taskData: TaskwDataProvider.TaskwData ->
            mainList.togglePinnedTask(taskData.uuid)
            mainList.reload()
            true
        }

        return null

//        return when (result) {
//            SwipeableItemConstants.RESULT_SWIPED_RIGHT -> if (mProvider
//                            .getGroup(groupPosition)
//                            .isPinned) {
//                // pinned --- back to default groupPosition
//                UnpinResultAction(this, groupPosition)
//            } else {
//                // not pinned --- remove
//                SwipeRightResultAction(this, groupPosition)
//            }
//            SwipeableItemConstants.RESULT_SWIPED_LEFT -> SwipeLeftResultAction(this,
//                    groupPosition, onPinActions)
//            SwipeableItemConstants.RESULT_CANCELED -> if (groupPosition != RecyclerView
//                            .NO_POSITION) {
//                UnpinResultAction(this, groupPosition)
//            } else {
//                null
//            }
//            else -> if (groupPosition != RecyclerView.NO_POSITION) {
//                UnpinResultAction(this, groupPosition)
//            } else {
//                null
//            }
//        }
    }

}