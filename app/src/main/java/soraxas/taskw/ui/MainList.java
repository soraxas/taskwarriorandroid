package soraxas.taskw.ui;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;

import org.json.JSONObject;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.List;

import soraxas.taskw.App;
import soraxas.taskw.R;
import soraxas.taskw.common.data.TaskwDataProvider;
import soraxas.taskw.data.Controller;
import soraxas.taskw.data.ReportInfo;
import soraxas.taskw.demo_s_longpress.SwipeOnLongPressExampleActivity;
import soraxas.taskw.demo_s_longpress.SwipeOnLongPressExampleAdapter;

/**
 * Created by vorobyev on 11/19/15.
 */
public class MainList extends Fragment {

    Controller controller = App.controller();
    Logger logger = Logger.forInstance(this);
    //    private MainListAdapter adapter = null;
    private String account = null;


    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    public SwipeOnLongPressExampleAdapter mAdapter;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewSwipeManager mRecyclerViewSwipeManager;
    private RecyclerViewTouchActionGuardManager mRecyclerViewTouchActionGuardManager;

    TaskwDataProvider mDataProvider;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        mRecyclerView = view.findViewById(R.id.list_main_list);
        mLayoutManager = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        mRecyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        mRecyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        mRecyclerViewTouchActionGuardManager.setEnabled(true);


        // swipe manager
        mRecyclerViewSwipeManager = new RecyclerViewSwipeManager();

        //adapter
        mDataProvider = new TaskwDataProvider();
        final SwipeOnLongPressExampleAdapter myItemAdapter = new SwipeOnLongPressExampleAdapter(mDataProvider);
        myItemAdapter.setEventListener(new SwipeOnLongPressExampleAdapter.EventListener() {
            @Override
            public void onItemRemoved(int position) {
                ((SwipeOnLongPressExampleActivity) getActivity()).onItemRemoved(position);
            }

            @Override
            public void onItemPinned(int position) {
                ((SwipeOnLongPressExampleActivity) getActivity()).onItemPinned(position);
            }

            @Override
            public void onItemViewClicked(View v, boolean pinned) {


//                final JSONObject json = data.get(0);
//
//
//                Context context = v.getContext();
//
//                View taskDetailView = inflateTaskDetailView(json, context);
//                bindTaskDetailView(taskDetailView, json);
//                showD(context, taskDetailView);
//
//                Intent myIntent = new Intent(context, SwipeOnLongPressExampleActivity.class);
////                myIntent.putExtra("key", value); //Optional parameters
//                context.startActivity(myIntent);
//
//
//                onItemViewClick(v, pinned);





                /*



                v.getContext();

//    final MainListAdapter.ListViewHolder holder, int position



        // load data
        boolean last = getItemCount() - 1 == position;
        final JSONObject json = data.get(position);

        Context context = holder.itemView.getContext();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);


        holder.itemView.findViewById(R.id.task_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = holder.itemView.getContext();

                View taskDetailView = inflateTaskDetailView(json, context);
                bindTaskDetailView(taskDetailView, json);
                showD(context, taskDetailView);



                Intent myIntent = new Intent(context, SwipeOnLongPressExampleActivity.class);
//                myIntent.putExtra("key", value); //Optional parameters
                context.startActivity(myIntent);


            }
        });


        // color card that has been started
        if (info.fields.containsKey("start")) {
            String status = json.optString("status", "pending");
            if ("pending".equalsIgnoreCase(status)) {
                boolean isStarted = !TextUtils.isEmpty(json.optString("start"));
                // this has started and is pending
                if (isStarted) {
                    holder.card.setBackgroundResource(R.color.DarkCyan);
                    holder.itemView.findViewById(R.id.left).setBackgroundResource(R.color.DarkRed);
                    ((ImageView) holder.itemView.findViewById(R.id.task_start_stop_btn)).setImageResource(R.drawable.ic_action_stop);
                } else {
                    holder.itemView.findViewById(R.id.left).setBackgroundResource(R.color.LightGreen);
                    ((ImageView) holder.itemView.findViewById(R.id.task_start_stop_btn)).setImageResource(R.drawable.ic_action_start);
                }
            }
        }

        // bind swipe menu functionality

        holder.itemView.findViewById(R.id.left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onStartStop(json);
            }
        });



        View views = holder.itemView;

        // remove previous data
//        views.findViewById(R.id.task_urgency).setVisibility(View.GONE);
//        views.findViewById(R.id.task_priority).setVisibility(View.GONE);
//        views.findViewById(R.id.task_annotations_flag).setVisibility(View.GONE);
        boolean hasAnno = json.optJSONArray("annotations") != null;
        views.findViewById(R.id.task_annotations_flag).setVisibility(hasAnno ? View.VISIBLE : View.GONE);

        ((ViewGroup) views.findViewById(R.id.task_labels_left)).removeAllViews();
        ((ViewGroup) views.findViewById(R.id.task_labels_right)).removeAllViews();

        boolean hasBottomLabels = false;
        // update view with data
//        for (Map.Entry<String, String> field : info.fields.entrySet()) {
//            switch (field.getKey().toLowerCase()) {
//                case "description":
//                    // Set desc
//                    TextView desc = views.findViewById(R.id.task_description);
//                    desc.setText(json.optString("description"));
//                    desc.setClickable(false);
//                    desc.setFocusable(false);
//                    break;
//                case "priority":
//                    int index = info.priorities.indexOf(json.optString("priority", ""));
//                    if (index == -1) {
//                        ProgressBar pb = views.findViewById(R.id.task_priority);
//                        pb.setMax(0);
//                        pb.setProgress(0);
//                    } else {
//                        ProgressBar pb = views.findViewById(R.id.task_priority);
//                        pb.setMax(info.priorities.size() - 1);
//                        pb.setProgress(info.priorities.size() - index - 1);
//                    }
//                    break;
//                case "urgency":
//                    ProgressBar pb = views.findViewById(R.id.task_urgency);
//                    pb.setMax(urgMax - urgMin);
//                    pb.setProgress((int) Math.round(json.optDouble("urgency")) - urgMin);
//                    break;
//                case "due":
//                    addLabel(context, views, "due", true, R.drawable.ic_label_due,
//                            asDate(json.optString("due"), sharedPref));
//                    hasBottomLabels = true;
//                    break;
//                case "wait":
//                    addLabel(context, views, "wait", true, R.drawable.ic_label_wait,
//                            asDate(json.optString("wait"), sharedPref));
//                    hasBottomLabels = true;
//                    break;
//                case "scheduled":
//                    addLabel(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
//                            asDate(json.optString("scheduled"), sharedPref));
//                    hasBottomLabels = true;
//                    break;
//                case "recur":
//                    String recur = json.optString("recur");
//                    if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
//                        String until = asDate(json.optString("until"), sharedPref);
//                        if (!TextUtils.isEmpty(until)) {
//                            recur += String.format(" ~ %s", until);
//                        }
//                    }
//                    addLabel(context, views, "recur", true, R.drawable.ic_label_recur, recur);
//                    hasBottomLabels = true;
//                    break;
//                case "project":
//                    addLabel(context, views, "project", false, R.drawable.ic_label_project,
//                            json.optString("project"));
//                    hasBottomLabels = true;
//                    break;
//                case "tags":
//                    addLabel(context, views, "tags", false, R.drawable.ic_label_tags, join(", ", array2List(
//                            json.optJSONArray("tags"))));
//                    hasBottomLabels = true;
//                    break;
//            }
//        }


        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String k = it.next();
            switch (k) {
                case "description":
                    // Set desc
                    TextView desc = views.findViewById(R.id.task_description);
                    desc.setText(json.optString("description"));
                    break;
                case "priority":
                    int index = info.priorities.indexOf(json.optString("priority", ""));
                    if (index == -1) {
                        ProgressBar pb = views.findViewById(R.id.task_priority);
                        pb.setMax(0);
                        pb.setProgress(0);
                    } else {
                        ProgressBar pb = views.findViewById(R.id.task_priority);
                        pb.setMax(info.priorities.size() - 1);
                        pb.setProgress(info.priorities.size() - index - 1);
                    }
                    break;
                case "urgency":
                    ProgressBar pb = views.findViewById(R.id.task_urgency);
                    pb.setMax(urgMax - urgMin);
                    pb.setProgress((int) Math.round(json.optDouble("urgency")) - urgMin);
                    break;
                case "due":
                    addLabel(context, views, "due", true, R.drawable.ic_label_due,
                            asDate(json.optString("due"), sharedPref));
                    hasBottomLabels = true;
                    break;
                case "wait":
                    addLabel(context, views, "wait", true, R.drawable.ic_label_wait,
                            asDate(json.optString("wait"), sharedPref));
                    hasBottomLabels = true;
                    break;
                case "scheduled":
                    addLabel(context, views, "scheduled", true, R.drawable.ic_label_scheduled,
                            asDate(json.optString("scheduled"), sharedPref));
                    hasBottomLabels = true;
                    break;
                case "recur":
                    String recur = json.optString("recur");
                    if (!TextUtils.isEmpty(recur) && info.fields.containsKey("until")) {
                        String until = asDate(json.optString("until"), sharedPref);
                        if (!TextUtils.isEmpty(until)) {
                            recur += String.format(" ~ %s", until);
                        }
                    }
                    addLabel(context, views, "recur", true, R.drawable.ic_label_recur, recur);
                    hasBottomLabels = true;
                    break;
                case "project":
                    addLabel(context, views, "project", false, R.drawable.ic_label_project,
                            json.optString("project"));
                    hasBottomLabels = true;
                    break;
                case "tags":
                    addLabel(context, views, "tags", false, R.drawable.ic_label_tags, join(", ", array2List(
                            json.optJSONArray("tags"))));
                    hasBottomLabels = true;
                    break;
            }
        }

        if (!hasBottomLabels){
            // make label match parent
            views.findViewById(R.id.task_description_outer).setLayoutParams(
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }


                */


            }
        });

        mAdapter = myItemAdapter;

        mWrappedAdapter = mRecyclerViewSwipeManager.createWrappedAdapter(myItemAdapter);      // wrap for swiping

        final GeneralItemAnimator animator = new SwipeDismissItemAnimator();

        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);  // requires *wrapped* adapter
        mRecyclerView.setItemAnimator(animator);


        // additional decorations
        //noinspection StatementWithEmptyBody
        if (supportsViewElevation()) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            mRecyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.material_shadow_z1)));
        }
        mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.list_divider_h), true));

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        //
        // priority: TouchActionGuard > Swipe > DragAndDrop
        mRecyclerViewTouchActionGuardManager.attachRecyclerView(mRecyclerView);
        mRecyclerViewSwipeManager.attachRecyclerView(mRecyclerView);
    }

    private void onItemViewClick(View v, boolean pinned) {
        int position = mRecyclerView.getChildAdapterPosition(v);
        if (position != RecyclerView.NO_POSITION) {
            ((MainActivity) getActivity()).onItemClicked(position);
        }


//                Context context = holder.itemView.getContext();
//
//                View taskDetailView = inflateTaskDetailView(json, context);
//                bindTaskDetailView(taskDetailView, json);
//                showD(context, taskDetailView);


//        Context context = getContext();
//        Intent myIntent = new Intent(context, SwipeOnLongPressExampleActivity.class);
////                myIntent.putExtra("key", value); //Optional parameters
//        context.startActivity(myIntent);


    }

    private boolean supportsViewElevation() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    public void load(final FormController form, final Runnable afterLoad) {
        // load the chosen report
        this.account = form.getValue(App.KEY_ACCOUNT);
        final String report = form.getValue(App.KEY_REPORT);
        final String query = form.getValue(App.KEY_QUERY);
        new Tasks.ActivitySimpleTask<ReportInfo>(getActivity()) {

            @Override
            protected ReportInfo doInBackground() {
                logger.d("Load:", query, report);
                return controller.accountController(account).taskReportInfo(report, query);
            }

            @Override
            public void finish(ReportInfo result) {
                mAdapter.info = result;
                if (null != afterLoad) afterLoad.run();
                reload();
            }
        }.exec();
    }

    public void reload() {
        if (null == mAdapter.info || null == account) return;
        // Load all items
        new Tasks.ActivitySimpleTask<List<JSONObject>>(getActivity()) {

            @Override
            protected List<JSONObject> doInBackground() {
                logger.d("Exec:", mAdapter.info.query);
                List<JSONObject> list = controller.accountController(account).taskList(mAdapter.info.query);
                mAdapter.info.sort(list); // Sorted according to report spec.
                return list;
            }

            @Override
            public void finish(List<JSONObject> result) {
                mDataProvider.update_report_info(result, mAdapter.info);

//                mWindowAttachCount

                if (mAdapter.update_cur_taskDetailView != null)
                    mAdapter.update_cur_taskDetailView.run();
                mAdapter.update(result, mAdapter.info);
//                logger.d("Loaded:", info, result);

            }
        }.exec();

    }

    public void listener(MainListAdapter.ItemListener listener) {
        this.mAdapter.listener = listene`r;
    }


//    public ReportInfo reportInfo() {
//        return info;
//    }


}
