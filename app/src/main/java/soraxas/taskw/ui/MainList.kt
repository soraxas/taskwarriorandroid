package soraxas.taskw.ui

import android.graphics.drawable.NinePatchDrawable
import android.os.Build
import android.os.Bundle
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
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager
import kotlinx.coroutines.GlobalScope
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
    lateinit var mRecyclerView: RecyclerView
    lateinit var mAdapter: SwipListAdapter
    var controller = controller()
    var logger = Logger.forInstance(this)
    var mDataProvider: TaskwDataProvider? = null
    private var account: String? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mWrappedAdapter: RecyclerView.Adapter<*>? = null
    private var mRecyclerViewSwipeManager: RecyclerViewSwipeManager? = null
    private var mRecyclerViewTouchActionGuardManager: RecyclerViewTouchActionGuardManager? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mRecyclerView = view.findViewById(R.id.list_main_list)
        mLayoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        mRecyclerView.setLayoutManager(mLayoutManager)

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = RecyclerViewTouchActionGuardManager()
        mRecyclerViewTouchActionGuardManager!!.setInterceptVerticalScrollingWhileAnimationRunning(true)
        mRecyclerViewTouchActionGuardManager!!.isEnabled = true


        // swipe manager
        mRecyclerViewSwipeManager = RecyclerViewSwipeManager()

        //adapter
        mDataProvider = TaskwDataProvider()
        mAdapter = SwipListAdapter(mDataProvider!!)
        mAdapter!!.eventListener = object : SwipListAdapter.EventListener {
            override fun onItemRemoved(position: Int) {
//                ((SwipeOnLongPressExampleActivity) getActivity()).onItemRemoved(position);
            }

            override fun onItemPinned(position: Int) {
//                ((SwipeOnLongPressExampleActivity) getActivity()).onItemPinned(position);
            }

            override fun onItemViewClicked(v: View?, pinned: Boolean) {}
        }
        mWrappedAdapter = mRecyclerViewSwipeManager!!.createWrappedAdapter(mAdapter!!) // wrap for swiping
        val animator: GeneralItemAnimator = SwipeDismissItemAnimator()

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.supportsChangeAnimations = false
        mRecyclerView.setLayoutManager(mLayoutManager)
        mRecyclerView.setAdapter(mWrappedAdapter) // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator)


        // additional decorations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            mRecyclerView.addItemDecoration(ItemShadowDecorator((ContextCompat.getDrawable(requireContext(), R.drawable.material_shadow_z1) as NinePatchDrawable?)!!))
        }
        mRecyclerView.addItemDecoration(SimpleListDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.list_divider_h), true))

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager!!.attachRecyclerView(mRecyclerView)
        mRecyclerViewSwipeManager!!.attachRecyclerView(mRecyclerView)
    }

    fun load(form: FormController, afterLoad: Runnable?) {
        // load the chosen report
        account = form.getValue<String>(App.KEY_ACCOUNT)
        val report = form.getValue<String>(App.KEY_REPORT)
        val query = form.getValue<String>(App.KEY_QUERY)

        GlobalScope.launch {
            logger.d("Load:", query, report)
            var result: ReportInfo = controller.accountController(account).taskReportInfo(report, query)
            mAdapter!!.info = result
            afterLoad?.run()
            reload()
        }
    }

    fun reload() {
        if (null == mAdapter!!.info || null == account) return
        // Load all items
        GlobalScope.launch {
            logger.d("Exec:", mAdapter!!.info!!.query)
            val list = controller.accountController(account).taskList(mAdapter!!.info!!.query)
            mAdapter!!.info!!.sort(list) // Sorted
            // according to report spec.
            mDataProvider!!.update_report_info(list, mAdapter!!.info)

//                mWindowAttachCount
            if (mAdapter!!.update_cur_taskDetailView != null) mAdapter!!.update_cur_taskDetailView!!.run()
            mAdapter!!.notifyDataSetChanged()
        }
    }

    fun listener(listener: SwipListAdapter.ItemListener?) {
        mAdapter!!.listener = listener
    }
}