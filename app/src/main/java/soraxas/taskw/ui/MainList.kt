package soraxas.taskw.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.NinePatchDrawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.log.Logger
import soraxas.taskw.App
import soraxas.taskw.App.Companion.controller
import soraxas.taskw.R
import soraxas.taskw.common.data.TaskwDataProvider
import soraxas.taskw.data.ReportInfo

//import soraxas.taskw.demo_s_longpress.SwipeOnLongPressExampleActivity;
/**
 * Created by vorobyev on 11/19/15.
 */
class MainList : Fragment() {
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private val workerScope = CoroutineScope(Dispatchers.Default + job)
    lateinit var mRecyclerView: RecyclerView
    lateinit var mAdapter: SwipeListAdapter
    var controller = controller()
    var logger = Logger.forInstance(this)
    private var account: String? = null
    private lateinit var mDataProvider: TaskwDataProvider
    var mRecyclerViewSwipeManager: RecyclerViewSwipeManager? = null
    private lateinit var mRecyclerViewTouchActionGuardManager:
            RecyclerViewTouchActionGuardManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }


    val SAVED_STATE_EXPANDABLE_ITEM_MANAGER: String = "RecyclerViewExpandableItemManager"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mRecyclerView = view.findViewById(R.id.list_main_list)
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = RecyclerViewTouchActionGuardManager()
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true)
        mRecyclerViewTouchActionGuardManager.isEnabled = true

        // swipe manager
        mRecyclerViewSwipeManager = RecyclerViewSwipeManager()

        // expand manager
        val eimSavedState: Parcelable? = savedInstanceState?.getParcelable(SAVED_STATE_EXPANDABLE_ITEM_MANAGER)
        val mRecyclerViewExpandableItemManager = RecyclerViewExpandableItemManager(eimSavedState);
        mRecyclerViewExpandableItemManager.setOnGroupExpandListener { groupPosition, fromUser, payload ->
            removeCollapseGroup(mDataProvider.getGroup(groupPosition).groupName)
        }
        mRecyclerViewExpandableItemManager.setOnGroupCollapseListener { groupPosition, fromUser, payload ->
            addCollapseGroup(mDataProvider.getGroup(groupPosition).groupName)
        }
        //adapter
        mDataProvider = TaskwDataProvider()
        mAdapter = SwipeListAdapter(mDataProvider, mRecyclerViewExpandableItemManager, this)

        // wrap for expanding
        var mWrappedAdapter = mRecyclerViewExpandableItemManager.createWrappedAdapter(mAdapter)


        val animator: GeneralItemAnimator = SwipeDismissItemAnimator()

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.supportsChangeAnimations = false

        // additional decorations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            mRecyclerView.addItemDecoration(ItemShadowDecorator((ContextCompat.getDrawable(requireContext(), R.drawable.material_shadow_z1) as NinePatchDrawable?)!!))
        }
        mRecyclerView.addItemDecoration(SimpleListDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.list_divider_h), true))



        mWrappedAdapter = mRecyclerViewSwipeManager!!.createWrappedAdapter(mWrappedAdapter) // wrap for swiping
        mRecyclerView.adapter = mWrappedAdapter
        mRecyclerView.itemAnimator = animator

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView)
        mRecyclerViewSwipeManager!!.attachRecyclerView(mRecyclerView)
        mRecyclerViewExpandableItemManager.attachRecyclerView(mRecyclerView)
    }

    val prefFileName: String = "taskListPref"
    val pinnedTasksPrefKey: String = "pinnedTasksList"
    val collapsedGroupPrefKey: String = "pinnedTasksList"

    fun getPinnedTasks(): Set<String> {
        context?.let {
            val pref: SharedPreferences = it.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
            return pref.getStringSet(pinnedTasksPrefKey, HashSet<String>())!!
        }
        return HashSet()
    }

    fun togglePinnedTask(uuid: String): Boolean {
        val sharedPref = context?.getSharedPreferences(prefFileName,
                Context.MODE_PRIVATE) ?: return false
        with(sharedPref.edit()) {
            // make a copy as we should not modify the result returned by sharedPref
            // (or else it wont detect any new changes)
            val pinnedTasks = HashSet(sharedPref.getStringSet(pinnedTasksPrefKey,
                    HashSet<String>())!!)
            // toggle task
            if (uuid in pinnedTasks)
                pinnedTasks.remove(uuid)
            else
                pinnedTasks.add(uuid)
            putStringSet(pinnedTasksPrefKey, pinnedTasks)
            apply()
        }
        return true
    }

    fun getCollapsedGroups(): Set<String> {
        context?.let {
            val pref: SharedPreferences = it.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
            return pref.getStringSet(collapsedGroupPrefKey, HashSet<String>())!!
        }
        return HashSet()
    }

    fun addCollapseGroup(groupName: String): Boolean {
        return toggleCollapseGroup(groupName, 1)
    }

    fun removeCollapseGroup(groupName: String): Boolean {
        return toggleCollapseGroup(groupName, -1)
    }

    fun toggleCollapseGroup(groupName: String, value: Int = 0): Boolean {
        val sharedPref = context?.getSharedPreferences(prefFileName,
                Context.MODE_PRIVATE) ?: return false
        with(sharedPref.edit()) {
            // make a copy as we should not modify the result returned by sharedPref
            // (or else it wont detect any new changes)
            val collapsedGroup = HashSet(sharedPref.getStringSet(collapsedGroupPrefKey,
                    HashSet<String>())!!)
            if (value == 0) {
                // toggle task
                if (groupName in collapsedGroup)
                    collapsedGroup.remove(groupName)
                else
                    collapsedGroup.add(groupName)
            } else if (value == 1) {
                collapsedGroup.add(groupName)
            } else if (value == -1) {
                collapsedGroup.remove(groupName)
            }
            putStringSet(collapsedGroupPrefKey, collapsedGroup)
            apply()
        }
        return true
    }

    fun load(form: FormController, afterLoad: Runnable?) {
        workerScope.launch {
            // load the chosen report
            account = form.getValue<String>(App.KEY_ACCOUNT)
            val report = form.getValue<String>(App.KEY_REPORT)
            val query = form.getValue<String>(App.KEY_QUERY)!!

            logger.d("Load:", query, report)
            val result: ReportInfo = controller.accountController(account).taskReportInfo(report, query)
            mAdapter.info = result
            afterLoad?.run()
            reload()
        }
    }

    fun reload() {
        if (null == account) return
        // Load all items
        workerScope.launch {
            logger.d("Exec:", mAdapter.info.query)
            val list = controller.accountController(account).taskList(mAdapter.info.query)
            mAdapter.info.sort(list) // Sorted
            // according to report spec.
            mDataProvider.updateReportInfo(list, getPinnedTasks())
            mAdapter.updateCurTaskDetailView?.run()
            notifyUiUpdate()
        }
    }

    private fun notifyUiUpdate() {
        // always run in the ui thread
        uiScope.launch {
            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job.cancel()
    }

    fun listener(listener: SwipeListAdapter.ItemListener?) {
        mAdapter.listener = listener
    }
}