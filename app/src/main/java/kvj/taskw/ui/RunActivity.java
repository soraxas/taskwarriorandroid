package kvj.taskw.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.ViewFinder;
import org.kvj.bravo7.form.impl.bundle.ListStringBundleAdapter;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.List;

import kvj.taskw.App;
import kvj.taskw.R;
import kvj.taskw.data.AccountController;
import kvj.taskw.data.Controller;

/**
 * Created by vorobyev on 12/1/15.
 */
public class RunActivity extends AppCompatActivity {

    FormController form = new FormController(new ViewFinder.ActivityViewFinder(this));
    Controller controller = App.controller();
    Logger logger = Logger.forInstance(this);
    private AccountController ac = null;
    private RunAdapter adapter = null;
    private kvj.taskw.data.AccountController.TaskListener progressListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        Toolbar toolbar = (Toolbar) findViewById(R.id.run_toolbar);
        RecyclerView list = (RecyclerView) findViewById(R.id.run_output);
        list.setLayoutManager(new LinearLayoutManager(this));
        setSupportActionBar(toolbar);
        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_ACCOUNT);
        form.add(new TransientAdapter<>(new ListStringBundleAdapter(), null), App.KEY_RUN_OUTPUT);
        form.add(new TextViewCharSequenceAdapter(R.id.run_command, null), App.KEY_RUN_COMMAND);
        form.load(this, savedInstanceState);
        progressListener = MainActivity
            .setupProgressListener(this, (ProgressBar) findViewById(R.id.run_progress));
        ac = controller.accountController(form);
        if (null == ac) {
            controller.messageShort("Invalid arguments");
            finish();
            return;
        }
        adapter = new RunAdapter(form.getValue(App.KEY_RUN_OUTPUT, ArrayList.class));
        list.setAdapter(adapter);
        findViewById(R.id.run_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                run();
            }
        });
        toolbar.setSubtitle(ac.name());
    }

    private void run() {
        final String input = form.getValue(App.KEY_RUN_COMMAND);
        if (TextUtils.isEmpty(input)) {
            controller.messageShort("Input is empty");
            return;
        }
        adapter.clear();
        final AccountController.ListAggregator out = new AccountController.ListAggregator();
        final AccountController.ListAggregator err = new AccountController.ListAggregator();
        new Tasks.ActivitySimpleTask<Boolean>(this) {

            @Override
            protected Boolean doInBackground() {
                int result = ac.taskCustom(input, out, err);
                return 0 == result;
            }

            @Override
            public void finish(Boolean result) {
                // No op
                out.data().addAll(err.data());
                adapter.addAll(out.data());
            }
        }.exec();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != adapter) {
            form.setValue(App.KEY_RUN_OUTPUT, adapter.data);
        }
        form.save(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ac.listeners().add(progressListener, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ac.listeners().remove(progressListener);
    }

    class RunAdapter extends RecyclerView.Adapter<RunAdapter.RunAdapterItem> {

        ArrayList<String> data = new ArrayList<>();

        public RunAdapter(ArrayList<String> data) {
            if (null != data) {
                this.data.addAll(data);
            }
        }

        @Override
        public RunAdapterItem onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RunAdapterItem(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_run_output, parent, false));
        }

        @Override
        public void onBindViewHolder(RunAdapterItem holder, int position) {
            holder.text.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        void clear() {
            notifyItemRangeRemoved(0, data.size());
            data.clear();
        }

        public synchronized void addAll(List<String> data) {
            int from = getItemCount();
            this.data.addAll(data);
            notifyItemRangeInserted(from, data.size());
        }

        class RunAdapterItem extends RecyclerView.ViewHolder implements View.OnLongClickListener {

            private final TextView text;

            public RunAdapterItem(View itemView) {
                super(itemView);
                itemView.setOnLongClickListener(this);
                this.text = (TextView) itemView.findViewById(R.id.run_item_text);
            }

            @Override
            public boolean onLongClick(View v) {
                controller.copyToClipboard(data.get(getAdapterPosition()));
                return true;
            }
        }
    }

}