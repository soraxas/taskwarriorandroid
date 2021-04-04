package soraxas.taskw.ui;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.ViewFinder;
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.bravo7.log.Logger;
import org.kvj.bravo7.util.DataUtil;
import org.kvj.bravo7.util.Tasks;

import java.util.ArrayList;
import java.util.Map;

import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import soraxas.taskw.App;
import soraxas.taskw.R;
import soraxas.taskw.data.AccountController;
import soraxas.taskw.data.Controller;

import static soraxas.taskw.ui.TaskDetailActivityKt.showD;

public class MainActivity extends AppCompatActivity implements Controller.ToastMessageListener {

    Logger logger = Logger.forInstance(this);

    Controller controller = App.controller();
    private AccountController ac = null;
    private Toolbar toolbar = null;
    private DrawerLayout navigationDrawer = null;
    private NavigationView navigation = null;
    private ViewGroup filterPanel = null;
    private PtrClassicFrameLayout mPtrFrame;

    private FormController form = new FormController(new ViewFinder.ActivityViewFinder(this));
    private MainList list = null;
    private Runnable updateTitleAction = new Runnable() {
        @Override
        public void run() {
            if (null != toolbar) toolbar.setSubtitle(list.mAdapter.info.description);
        }
    };
    private FloatingActionButton addButton = null;
    private ProgressBar progressBar = null;
    private AccountController.TaskListener progressListener = null;
    private TextView accountNameDisplay = null;
    private TextView accountNameID = null;
    private ViewGroup header = null;
    private PopupMenu.OnMenuItemClickListener accountMenuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_account_add:
                    controller.addAccount(MainActivity.this);
                    break;
                case R.id.menu_account_set_def:
                    if (null == ac) return false;
                    controller.setDefault(ac.id());
                    break;
            }
            navigationDrawer.closeDrawers();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller.toastListeners().add(this);
        setContentView(R.layout.activity_list);
        toolbar = findViewById(R.id.toolbar);
        navigationDrawer = (DrawerLayout) findViewById(R.id.list_navigation_drawer);
        navigation = (NavigationView) findViewById(R.id.list_navigation);
        header = (ViewGroup) navigation.inflateHeaderView(R.layout.item_nav_header);
        navigation.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        onNavigationMenu(item);
                        return true;
                    }
                });

        list = (MainList) getSupportFragmentManager().findFragmentById(R.id.list_list_fragment);


//        if (savedInstanceState == null) {
//            getSupportFragmentManager().beginTransaction()
//                    .add(new ExampleDataProviderFragment(), "data provider")
//                    .commit();
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.container, new SwipeOnLongPressExampleFragment(), "list view")
//                    .commit();
//        }

        addButton = (FloatingActionButton) findViewById(R.id.list_add_btn);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        accountNameDisplay = (TextView) header.findViewById(R.id.list_nav_account_name);
        accountNameID = (TextView) header.findViewById(R.id.list_nav_account_id);
        filterPanel = (ViewGroup) findViewById(R.id.list_filter_block);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navigationDrawer.isDrawerOpen(Gravity.LEFT)) {
                    navigationDrawer.closeDrawers();
                } else {
                    navigationDrawer.openDrawer(Gravity.LEFT);
                }
            }
        });
        header.findViewById(R.id.list_nav_menu_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAccountMenu(v);
            }
        });
        list.listener(new MainListAdapter.ItemListener() {
            @Override
            public void onEdit(JSONObject json) {
                // Start editor
                edit(json);
            }

            @Override
            public void onStatus(JSONObject json) {
                changeStatus(json);
            }

            @Override
            public void onDelete(JSONObject json) {
                doOp(String.format("Task '%s' deleted", json.optString("description")),
                        json.optString("uuid"), "delete");
            }

            @Override
            public void onAnnotate(JSONObject json) {
                annotate(json);
            }

            @Override
            public void onDenotate(JSONObject json, JSONObject annJson) {
                String text = annJson.optString("description");
                doOp(String.format("Annotation '%s' deleted", text), json.optString("uuid"),
                        "denotate", text);
            }

            @Override
            public void onCopyText(JSONObject json, String text) {
                controller.copyToClipboard(text);
            }

            @Override
            public void onLabelClick(JSONObject json, String type, boolean longClick) {
                if (longClick) { // Special case - start search
                    Intent intent = new Intent(MainActivity.this, MainActivity.class);
                    intent.putExtra(App.KEY_ACCOUNT, form.getValue(App.KEY_ACCOUNT, String.class));
                    intent.putExtra(App.KEY_REPORT, form.getValue(App.KEY_REPORT, String.class));
                    String query = form.getValue(App.KEY_QUERY);
                    if ("project".equals(type)) {
                        query += " pro:" + json.optString("project");
                        intent.putExtra(App.KEY_QUERY, query.trim());
                        startActivity(intent);
                        return;
                    }
                    if ("tags".equals(type)) {
                        String tags = MainListAdapter.join(" +",
                                MainListAdapter.array2List(json.optJSONArray("tags")));
                        query += " +" + tags;
                        intent.putExtra(App.KEY_QUERY, query.trim());
                        startActivity(intent);
                        return;
                    }

                    return;
                }
                if ("project".equals(type)) {
                    add(Pair.create(App.KEY_EDIT_PROJECT, json.optString("project")));
                }
                if ("tags".equals(type)) {
                    String tags = MainListAdapter.join(" ",
                            MainListAdapter.array2List(json.optJSONArray("tags")));
                    add(Pair.create(App.KEY_EDIT_TAGS, tags));
                }
                if ("due".equals(type)) {
                    add(Pair.create(App.KEY_EDIT_DUE,
                            MainListAdapter.asDate(json.optString("due"), null)));
                }
                if ("wait".equals(type)) {
                    add(Pair.create(App.KEY_EDIT_WAIT,
                            MainListAdapter.asDate(json.optString("wait"), null)));
                }
                if ("scheduled".equals(type)) {
                    add(Pair.create(App.KEY_EDIT_SCHEDULED,
                            MainListAdapter.asDate(json.optString("scheduled"), null)));
                }
                if ("recur".equals(type)) {
                    add(Pair.create(App.KEY_EDIT_UNTIL,
                            MainListAdapter.asDate(json.optString("until"), null)),
                            Pair.create(App.KEY_EDIT_RECUR, json.optString("recur")));
                }
            }

            @Override
            public void onStartStop(JSONObject json) {
                String text = json.optString("description");
                String uuid = json.optString("uuid");
                boolean started = json.has("start");
                if (started) { // Stop
                    doOp(String.format("Task'%s' stopped", text), uuid, "stop");
                } else { // Start
                    doOp(String.format("Task '%s' started", text), uuid, "start");
                }
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });
        progressListener = MainActivity.setupProgressListener(this, progressBar);
        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_ACCOUNT);
        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_REPORT);
//        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_QUERY);
        form.add(new TextViewCharSequenceAdapter(R.id.list_filter, null), App.KEY_QUERY);
        form.load(this, savedInstanceState);
        findViewById(R.id.list_filter_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = form.getValue(App.KEY_QUERY);
//                form.setValue(App.KEY_QUERY, input);
                logger.d("Changed filter:", form.getValue(App.KEY_QUERY), input);
                reload();
            }
        });
        if (!TextUtils.isEmpty(form.getValue(App.KEY_QUERY, String.class))) {
            // Have something in query
            filterPanel.setVisibility(View.VISIBLE);
        }

        // pull-to-refresh swipeToRefresh
        mPtrFrame = findViewById(R.id.list_fragment_pull_to_refresh);
        mPtrFrame.setLastUpdateTimeRelateObject(this);
        mPtrFrame.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                sync();
                frame.refreshComplete();
            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, findViewById(R.id.list_main_list), header);
            }
        });

        // update widget
        Intent intent = new Intent(this, TaskReportWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), TaskReportWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void reload() {
        // Show/hide filter
        String query = form.getValue(App.KEY_QUERY);
        filterPanel.setVisibility(TextUtils.isEmpty(query) ? View.GONE : View.VISIBLE);
        list.load(form, updateTitleAction);
    }

    private void annotate(JSONObject json) {
        Intent dialog = new Intent(this, AnnotationDialog.class);
        dialog.putExtra(App.KEY_ACCOUNT, form.getValue(App.KEY_ACCOUNT, String.class));
        dialog.putExtra(App.KEY_EDIT_UUID, json.optString("uuid"));
        startActivityForResult(dialog, App.ANNOTATE_REQUEST);
    }

    private void showAccountMenu(View btn) {
        PopupMenu menu = new PopupMenu(this, btn);
        menu.inflate(R.menu.menu_account);
        int index = 0;
        for (Account account : controller.accounts()) {
            menu.getMenu().add(R.id.menu_account_list, index++, 0, account.name)
                    .setOnMenuItemClickListener(newAccountMenu(controller.accountID(account)));
        }
        menu.setOnMenuItemClickListener(accountMenuListener);
        menu.show();
    }

    private MenuItem.OnMenuItemClickListener newAccountMenu(final String accountName) {
        return new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent listIntent = new Intent(MainActivity.this, MainActivity.class);
                listIntent.putExtra(App.KEY_ACCOUNT, accountName);
                startActivity(listIntent);
                return true;
            }
        };
    }

    private void onNavigationMenu(MenuItem item) {
        final String account = form.getValue(App.KEY_ACCOUNT);
        if (null == ac) return;
        navigationDrawer.closeDrawers();

        switch (item.getItemId()) {
            case R.id.menu_nav_reload:
                refreshAccount(account);
                break;
            case R.id.menu_nav_run:
                startActivity(ac.intentForRunTask());
                break;
            case R.id.menu_nav_debug:
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Taskwarrior for Android debug output");
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(ac.debugLogger().file()));
                try {
                    startActivity(Intent.createChooser(emailIntent, "Share debug output..."));
                } catch (Throwable t) {
                    controller.toastMessage("Failed to share debug file", true);
                }
                break;
            case R.id.menu_nav_settings:
                Intent intent = new Intent(controller.context(), SettingsActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void refreshAccount(final String account) {
        new Tasks.ActivitySimpleTask<AccountController>(this) {

            @Override
            protected AccountController doInBackground() {
                return controller.accountController(account, true); //Re-init
            }

            @Override
            public void finish(AccountController result) {
                ac = result;
                if (null != ac) {
                    // Refreshed
                    refreshReports();
                } else {
                    MainActivity.this.finish(); // Close
                }
            }
        }.exec();
    }

    private void changeStatus(JSONObject json) {
        String status = json.optString("status");
        String uuid = json.optString("uuid");
        String description = json.optString("description");
        if ("pending".equalsIgnoreCase(status)) {
            // Mark as done
            doOp(String.format("Task '%s' marked done", description), uuid, "done");
        }
    }

    private void doOp(final String message, final String uuid, final String op, final String... ops) {
        if (ac == null) return;
        final Tasks.ActivitySimpleTask<String> task = new Tasks.ActivitySimpleTask<String>(this) {

            @Override
            protected String doInBackground() {
                if ("done".equalsIgnoreCase(op))
                    return ac.taskDone(uuid);
                if ("delete".equalsIgnoreCase(op))
                    return ac.taskDelete(uuid);
                if ("start".equalsIgnoreCase(op))
                    return ac.taskStart(uuid);
                if ("stop".equalsIgnoreCase(op))
                    return ac.taskStop(uuid);
                if ("denotate".equalsIgnoreCase(op))
                    return ac.taskDenotate(uuid, ops[0]);
                return "Not supported operation";
            }

            @Override
            public void finish(String result) {
                if (null != result) {
                    controller.toastMessage(result, true);
                } else {
                    if (null != message) { // Show success message
                        controller.toastMessage(message, false);
                    }
                    list.reload();
                }
            }
        };
        task.exec();
    }

    public static AccountController.TaskListener setupProgressListener(final Activity activity, final ProgressBar bar) {
        final Controller controller = App.controller();
        final Handler handler = new Handler(activity.getMainLooper());
        return new AccountController.TaskListener() {

            int balance = 0;

            @Override
            public void onStart() {

                if (null == bar) return;
                balance++;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (balance == 0) {
                            return;
                        }
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                bar.setVisibility(View.VISIBLE);
                            }
                        });

                    }
                }, 750);
            }

            @Override
            public void onFinish() {
                if (null == bar) return;
                if (balance > 0) {
                    balance--;
                }
                if (balance > 0) {
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onQuestion(final String question, final DataUtil.Callback<Boolean> callback) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Show dialog
                        controller.question(activity, question, new Runnable() {
                            @Override
                            public void run() {
                                callback.call(true);
                            }
                        }, new Runnable() {
                            @Override
                            public void run() {
                                callback.call(false);
                            }
                        });
                    }
                });
            }
        };
    }

    private void add(Pair<String, String>... pairs) {
        if (null == ac) return;
        Intent intent = new Intent(this, EditorActivity.class);
        showD(this, View.inflate(this, R.layout.activity_editor, null));
        ac.intentForEditor(intent, null);
        if (null != pairs) {
            Bundle data = new Bundle();
            ArrayList<String> names = new ArrayList<>();
            for (Pair<String, String> pair : pairs) { // $COMMENT
                if (!TextUtils.isEmpty(pair.second)) { // Has data
                    data.putString(pair.first, pair.second);
                    names.add(pair.first);
                }
            }
            intent.putExtra(App.KEY_EDIT_DATA, data);
            intent.putStringArrayListExtra(App.KEY_EDIT_DATA_FIELDS, names);
        }
        startActivityForResult(intent, App.EDIT_REQUEST);
    }

    private void edit(JSONObject json) {
        if (null == ac) return;
        Intent intent = new Intent(this, EditorActivity.class);
        if (ac.intentForEditor(intent, json.optString("uuid"))) { // Valid task
            startActivityForResult(intent, App.EDIT_REQUEST);
        } else {
            controller.toastMessage("Invalid task", false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        form.save(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        addButton.setEnabled(false);
        if (checkAccount()) {
            addButton.setEnabled(true);
            ac.listeners().add(progressListener, true);
            accountNameDisplay.setText(ac.name());
            accountNameID.setText(ac.id());
            refreshReports();
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onDestroy() {
        if (null != ac) {
            ac.listeners().remove(progressListener);
        }
        controller.toastListeners().remove(this);
        super.onDestroy();
    }

    private boolean checkAccount() {
        ac = controller.accountController(form);
        if (null != ac) { // Have account
            return true;
        }
        String account = controller.currentAccount();
        if (account == null) {
            // Start new account UI
            controller.addAccount(this);
            return false;
        } else {
            logger.d("Refresh account:", account);
            form.setValue(App.KEY_ACCOUNT, account);
            ac = controller.accountController(form); // Should be not null always
        }
        return true;
    }

    private void refreshReports() {
        new Tasks.ActivitySimpleTask<Map<String, String>>(this) {

            @Override
            protected Map<String, String> doInBackground() {
                return ac.taskReports();
            }

            @Override
            public void finish(Map<String, String> result) {
                // We're in UI thread
                navigation.getMenu().findItem(R.id.menu_nav_debug).setVisible(ac.debugEnabled());
                MenuItem reportsMenu = navigation.getMenu().findItem(R.id.menu_nav_reports);
                reportsMenu.getSubMenu().clear();
                for (Map.Entry<String, String> entry : result.entrySet()) { // Add reports
                    addReportMenuItem(entry.getKey(), entry.getValue(), reportsMenu.getSubMenu());
                }
                // Report mode
                String report = form.getValue(App.KEY_REPORT);
                if (null == report || !result.containsKey(report)) {
                    report = result.keySet().iterator().next(); // First item
                }
                form.setValue(App.KEY_REPORT, report);
                list.load(form, updateTitleAction);
            }

            private void addReportMenuItem(final String key, String title, SubMenu menu) {
                menu.add(title).setIcon(R.drawable.ic_action_report).setOnMenuItemClickListener(
                        new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                // Show report
                                form.setValue(App.KEY_REPORT, key);
                                form.setValue(App.KEY_QUERY, null);
                                list.load(form, updateTitleAction);
                                reload();
                                return false;
                            }
                        });
            }
        }.exec();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tb_reload:
                list.reload();
                break;
            case R.id.menu_tb_sync:
                sync();
                break;
            case R.id.menu_tb_undo:
                undo();
                break;
            case R.id.menu_tb_filter:
                showFilter();
                break;
            case R.id.menu_tb_add_shortcut:
                createShortcut();
                break;
            case R.id.menu_tb_run:
                startActivity(ac.intentForRunTask());
                break;
        }
        return true;
    }

    private void createShortcut() {
        Bundle bundle = new Bundle();
        form.save(bundle, App.KEY_ACCOUNT, App.KEY_REPORT, App.KEY_QUERY);
        String query = bundle.getString(App.KEY_QUERY, "");
        String name = bundle.getString(App.KEY_REPORT, "");
        if (!TextUtils.isEmpty(query)) { // Have add. query
            name += " " + query;
        }
        final Intent shortcutIntent = new Intent(this, MainActivity.class);
        shortcutIntent.putExtras(bundle);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        controller.input(this, "Shortcut name:", name, new DataUtil.Callback<CharSequence>() {

            @Override
            public boolean call(CharSequence value) {
                controller.createShortcut(shortcutIntent, value.toString().trim());
                return true;
            }
        }, null);
    }

    private void showFilter() {
        filterPanel.setVisibility(View.VISIBLE);
        form.getView(App.KEY_QUERY).requestFocus();
    }

    private void undo() {
        if (null == ac) return;
        new Tasks.ActivitySimpleTask<String>(this) {

            @Override
            protected String doInBackground() {
                return ac.taskUndo();
            }

            @Override
            public void finish(String result) {
                if (null != result) {
                    controller.toastMessage(result, false);
                } else {
                    list.reload();
                }
            }
        }.exec();
    }

    private void sync() {
        if (null == ac) {
            controller.toastMessage("Unable to sync", false);
            return;
        }
        new Tasks.ActivitySimpleTask<String>(this) {

            @Override
            protected String doInBackground() {
                return ac.taskSync();
            }

            @Override
            public void finish(String result) {
                if (null != result) { // Error
                    controller.toastMessage(result, false);
                } else {
                    controller.toastMessage("Sync success", false);
                    list.reload();
                }
            }
        }.exec();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (null == ac) return;
        if (RESULT_OK == resultCode && App.SETTINGS_REQUEST == requestCode) { // Settings were modified
            logger.d("Reload after finish:", requestCode, resultCode);
            refreshAccount(form.getValue(App.KEY_ACCOUNT, String.class));
        }
    }

    @Override
    public void onMessage(final String message, final boolean showLong) {
        runOnUiThread(() -> {

            int length;
            if (showLong)
                length = Toast.LENGTH_LONG;
            else
                length = Toast.LENGTH_SHORT;
            Toast.makeText(this, message, length).show();
        });
    }

    /**
     * This method will be called when a list item is removed
     *
     * @param position The position of the item within data set
     */
    public void onItemRemoved(int position) {
        Snackbar snackbar = Snackbar.make(
                findViewById(R.id.container),
                "1 item removed",
                Snackbar.LENGTH_LONG);

        snackbar.setAction("UNDO", v -> onItemUndoActionClicked());
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_action_color_done));
        snackbar.show();
    }

    /**
     * This method will be called when a list item is pinned
     *
     * @param position The position of the item within data set
     */
    public void onItemPinned(int position) {
//        final DialogFragment dialog = ItemPinnedMessageDialogFragment.newInstance(position);
//
//        getSupportFragmentManager()
//                .beginTransaction()
//                .add(dialog, FRAGMENT_TAG_ITEM_PINNED_DIALOG)
//                .commit();
    }

    /**
     * This method will be called when a list item is clicked
     *
     * @param position The position of the item within data set
     */
    public void onItemClicked(int position) {
//        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);
//        AbstractDataProvider.Data data = getDataProvider().getItem(position);
//
//        if (data.isPinned()) {
//            // unpin if tapped the pinned item
//            data.setPinned(false);
//            ((SwipeOnLongPressExampleFragment) fragment).notifyItemChanged(position);
//        }
    }

    private void onItemUndoActionClicked() {
//        int position = getDataProvider().undoLastRemoval();
//        if (position >= 0) {
//            final Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);
//            ((SwipeOnLongPressExampleFragment) fragment).notifyItemInserted(position);
//        }
    }
}
