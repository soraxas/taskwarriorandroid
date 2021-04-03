package soraxas.taskw.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

import soraxas.taskw.App;
import soraxas.taskw.R;
import soraxas.taskw.data.AccountController;
import soraxas.taskw.data.Controller;

/**
 * Created by vorobyev on 12/1/15.
 */
public class RunActivity extends AppCompatActivity {

    FormController form = new FormController(new ViewFinder.ActivityViewFinder(this));
    Controller controller = App.controller();
    Logger logger = Logger.forInstance(this);
    private AccountController ac = null;
    private RunAdapter adapter = null;
    private soraxas.taskw.data.AccountController.TaskListener progressListener = null;
    private ShareActionProvider mShareActionProvider = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        RecyclerView list = (RecyclerView) findViewById(R.id.run_output);
        list.setLayoutManager(new LinearLayoutManager(this));
        setSupportActionBar(toolbar);
        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_ACCOUNT);
        form.add(new TransientAdapter<>(new ListStringBundleAdapter(), null), App.KEY_RUN_OUTPUT);
        form.add(new TextViewCharSequenceAdapter(R.id.run_command, null), App.KEY_RUN_COMMAND);
        form.load(this, savedInstanceState);

        // Enter key trigger button press of running command
        final EditText edittext = findViewById(R.id.run_command);
        edittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND)) {
                    //do what you want on the press of 'done'
                    run();
                    // close keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(edittext.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        // Set enter key custom test
        edittext.setImeActionLabel("Run", KeyEvent.KEYCODE_ENTER);

        progressListener = MainActivity
                .setupProgressListener(this, (ProgressBar) findViewById(R.id.progress));
        ac = controller.accountController(form);
        if (null == ac) {
            controller.toastMessage("Invalid arguments", false);
            finish();
            return;
        }
        adapter = new RunAdapter(form.getValue(App.KEY_RUN_OUTPUT, ArrayList.class));
        list.setAdapter(adapter);
        toolbar.setSubtitle(ac.name());
    }

    private void shareAll() {
        CharSequence text = adapter.allText();
        if (TextUtils.isEmpty(text)) { // Error
            controller.toastMessage("Nothing to share", false);
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        if (null != mShareActionProvider) {
            logger.d("Share provider set");
            mShareActionProvider.setShareIntent(sendIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_run, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_tb_run_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        // Return true to display menu
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tb_run_run:
                run();
                return true;
            case R.id.menu_tb_run_copy:
                copyAll();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyAll() {
        CharSequence text = adapter.allText();
        if (TextUtils.isEmpty(text)) { // Error
            controller.toastMessage("Nothing to copy", false);
            return;
        }
        controller.copyToClipboard(text);
    }

    private void run() {
        final String input = form.getValue(App.KEY_RUN_COMMAND);
        if (TextUtils.isEmpty(input)) {
            controller.toastMessage("Input is empty", false);
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
                out.data().addAll(err.data());
                adapter.addAll(out.data());
                shareAll();
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

        public CharSequence allText() {
            StringBuilder sb = new StringBuilder();
            for (String line : data) { // Copy to
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb;
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
