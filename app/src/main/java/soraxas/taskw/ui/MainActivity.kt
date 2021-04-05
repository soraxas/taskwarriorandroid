package soraxas.taskw.ui

import `in`.srain.cube.views.ptr.PtrClassicFrameLayout
import `in`.srain.cube.views.ptr.PtrDefaultHandler
import `in`.srain.cube.views.ptr.PtrFrameLayout
import `in`.srain.cube.views.ptr.PtrHandler
import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.*
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.form.impl.ViewFinder.ActivityViewFinder
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter
import org.kvj.bravo7.form.impl.widget.TransientAdapter
import org.kvj.bravo7.log.Logger
import org.kvj.bravo7.util.DataUtil
import soraxas.taskw.App
import soraxas.taskw.R
import soraxas.taskw.common.Helpers
import soraxas.taskw.data.AccountController
import soraxas.taskw.data.AccountController.TaskListener
import soraxas.taskw.data.Controller
import java.util.*
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), Controller.ToastMessageListener, CoroutineScope {
    private lateinit var mJob: Job
    override val coroutineContext: CoroutineContext
        get() = mJob + Dispatchers.Main


    private val form = FormController(ActivityViewFinder(this))
    var logger = Logger.forInstance(this)!!
    var controller: Controller = App.controller()
    private var ac: AccountController? = null
    private lateinit var toolbar: Toolbar
    private lateinit var navigationDrawer: DrawerLayout
    private val accountMenuListener = PopupMenu.OnMenuItemClickListener { item ->
        when (item.itemId) {
            R.id.menu_account_add -> controller.addAccount(this@MainActivity)
            R.id.menu_account_set_def -> {
                if (null == ac) return@OnMenuItemClickListener false
                controller.setDefault(ac?.id())
            }
        }
        navigationDrawer!!.closeDrawers()
        true
    }
    private lateinit var navigation: NavigationView
    private lateinit var filterPanel: ViewGroup
    private lateinit var mPtrFrame: PtrClassicFrameLayout
    private lateinit var list: MainList
    private val updateTitleAction = Runnable {
        launch {
            toolbar.subtitle = list.mAdapter.info!!.description
        }
    }
    private var addButton: FloatingActionButton? = null
    private var progressBar: ProgressBar? = null
    private var progressListener: TaskListener? = null
    private var accountNameDisplay: TextView? = null
    private var accountNameID: TextView? = null
    private var header: ViewGroup? = null

    private fun show_undo_msg(message: String?) {
        if (message == null)
            return;
        val parentLayout = findViewById<View>(R.id.coordinator_layout)
        Snackbar.make(parentLayout, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("UNDO") { undo() }
                .setActionTextColor(resources.getColor(android.R.color.holo_red_light))
                .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mJob = Job()
        controller.toastListeners().add(this)
        setContentView(R.layout.activity_list)
        toolbar = findViewById(R.id.toolbar)
        navigationDrawer = findViewById<View>(R.id.list_navigation_drawer) as DrawerLayout
        navigation = findViewById<View>(R.id.list_navigation) as NavigationView
        header = navigation.inflateHeaderView(R.layout.item_nav_header) as ViewGroup
        navigation.setNavigationItemSelectedListener { item: MenuItem ->
            onNavigationMenu(item)
            true
        }


//        // register the extended floating action Button
//        final ExtendedFloatingActionButton extendedFloatingActionButton = findViewById(R.id.extFloatingActionButton);
//        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
//            @Override
//            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                // the delay of the extension of the FAB is set for 12 items
//                if (scrollY > oldScrollY + 12 && extendedFloatingActionButton.isExtended()) {
//                    extendedFloatingActionButton.shrink();
//                }
//
//                // the delay of the extension of the FAB is set for 12 items
//                if (scrollY < oldScrollY - 12 && !extendedFloatingActionButton.isExtended()) {
//                    extendedFloatingActionButton.extend();
//                }
//
//                // if the nestedScrollView is at the first item of the list then the
//                // extended floating action should be in extended state
//                if (scrollY == 0) {
//                    extendedFloatingActionButton.extend();
//                }
//            }
//        });
        list = supportFragmentManager.findFragmentById(R.id.list_list_fragment) as
                MainList
        addButton = findViewById<View>(R.id.list_add_btn) as FloatingActionButton
        progressBar = findViewById<View>(R.id.progress) as ProgressBar
        accountNameDisplay = header!!.findViewById<View>(R.id.list_nav_account_name) as TextView
        accountNameID = header!!.findViewById<View>(R.id.list_nav_account_id) as TextView
        filterPanel = findViewById<View>(R.id.list_filter_block) as ViewGroup
        setSupportActionBar(toolbar)
        toolbar!!.setNavigationOnClickListener {
            if (navigationDrawer!!.isDrawerOpen(Gravity.LEFT)) {
                navigationDrawer!!.closeDrawers()
            } else {
                navigationDrawer!!.openDrawer(Gravity.LEFT)
            }
        }
        header!!.findViewById<View>(R.id.list_nav_menu_btn).setOnClickListener { v: View -> showAccountMenu(v) }
        list.listener(object : SwipeListAdapter.ItemListener {
            override fun onEdit(json: JSONObject?) {
                edit(json) // Start editor
            }

            override fun onStatus(json: JSONObject?) {
                changeStatus(json)
            }

            override fun onDelete(json: JSONObject?) {
                doOp(String.format("Task '%s' deleted", json!!.optString("description")),
                        json.optString("uuid"), "delete")
            }

            override fun onAnnotate(json: JSONObject?) {
                annotate(json)
            }

            override fun onDenotate(json: JSONObject?, annJson: JSONObject?) {
                val text = annJson!!.optString("description")
                doOp(String.format("Annotation '%s' deleted", text), json!!.optString("uuid"),
                        "denotate", text)
            }

            override fun onCopyText(json: JSONObject?, text: String?) {
                controller.copyToClipboard(text)
            }

            override fun onLabelClick(json: JSONObject?, type: String?, longClick: Boolean) {
                if (longClick) { // Special case - start search
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    intent.putExtra(App.KEY_ACCOUNT, form.getValue(App.KEY_ACCOUNT, String::class.java))
                    intent.putExtra(App.KEY_REPORT, form.getValue(App.KEY_REPORT, String::class.java))
                    var query = form.getValue<String>(App.KEY_QUERY)
                    if (("project" == type)) {
                        query += " pro:" + json!!.optString("project")
                        intent.putExtra(App.KEY_QUERY, query.trim { it <= ' ' })
                        startActivity(intent)
                        return
                    }
                    if (("tags" == type)) {
                        val tags = Helpers.join(" +",
                                Helpers.array2List(json!!.optJSONArray("tags")))
                        query += " +$tags"
                        intent.putExtra(App.KEY_QUERY, query.trim { it <= ' ' })
                        startActivity(intent)
                        return
                    }
                    return
                }
                if (("project" == type)) {
                    add(Pair.create(App.KEY_EDIT_PROJECT, json!!.optString("project")))
                }
                if (("tags" == type)) {
                    val tags = Helpers.join(" ",
                            Helpers.array2List(json!!.optJSONArray("tags")))
                    add(Pair.create(App.KEY_EDIT_TAGS, tags))
                }
                if (("due" == type)) {
                    add(Pair.create(App.KEY_EDIT_DUE,
                            Helpers.asDate(json!!.optString("due"), null)))
                }
                if (("wait" == type)) {
                    add(Pair.create(App.KEY_EDIT_WAIT,
                            Helpers.asDate(json!!.optString("wait"), null)))
                }
                if (("scheduled" == type)) {
                    add(Pair.create(App.KEY_EDIT_SCHEDULED,
                            Helpers.asDate(json!!.optString("scheduled"), null)))
                }
                if (("recur" == type)) {
                    add(Pair.create(App.KEY_EDIT_UNTIL,
                            Helpers.asDate(json!!.optString("until"), null)),
                            Pair.create(App.KEY_EDIT_RECUR, json.optString("recur")))
                }
            }

            override fun onStartStop(json: JSONObject?) {
                val text = json!!.optString("description")
                val uuid = json.optString("uuid")
                val started = json.has("start")
                if (started) { // Stop
                    doOp(String.format("Task'%s' stopped", text), uuid, "stop")
                } else { // Start
                    doOp(String.format("Task '%s' started", text), uuid, "start")
                }
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            list.mRecyclerView.setOnScrollChangeListener { v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
                // the delay of the extension of the FAB is set for 12 items
                val numItemForFAB = 4
                logger.d(v)
                logger.d(scrollX, scrollY, oldScrollX, oldScrollY)
                if (scrollY > oldScrollY + numItemForFAB && addButton!!.isShown) {
                    addButton!!.hide()
                }

                // the delay of the extension of the FAB is set for 12 items
                if (scrollY < oldScrollY - numItemForFAB && !addButton!!.isShown) {
                    addButton!!.show()
                }

                // if the nestedScrollView is at the first item of the list then the
                // extended floating action should be in extended state
                if (scrollY == 0) {
                    addButton!!.show()
                }
            }
        }
        //        findViewById(R.id.list_fragment_pull_to_refresh).setOnScrollChangeListener(new OnScrollChangeListener() {
//            @Override
//            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                // the delay of the extension of the FAB is set for 12 items
//                if (scrollY > oldScrollY + 12 && extendedFloatingActionButton.isExtended()) {
//                    extendedFloatingActionButton.shrink();
//                }
//
//                // the delay of the extension of the FAB is set for 12 items
//                if (scrollY < oldScrollY - 12 && !extendedFloatingActionButton.isExtended()) {
//                    extendedFloatingActionButton.extend();
//                }
//
//                // if the nestedScrollView is at the first item of the list then the
//                // extended floating action should be in extended state
//                if (scrollY == 0) {
//                    extendedFloatingActionButton.extend();
//                }
//            }
//        }
//
//
//                                                                                   }
//        );
//        final ExtendedFloatingActionButton extendedFloatingActionButton = findViewById(R.id.extFloatingActionButton);
//        list.set
//        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
//            @Override
//            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                // the delay of the extension of the FAB is set for 12 items
//                if (scrollY > oldScrollY + 12 && extendedFloatingActionButton.isExtended()) {
//                    extendedFloatingActionButton.shrink();
//                }
//
//                // the delay of the extension of the FAB is set for 12 items
//                if (scrollY < oldScrollY - 12 && !extendedFloatingActionButton.isExtended()) {
//                    extendedFloatingActionButton.extend();
//                }
//
//                // if the nestedScrollView is at the first item of the list then the
//                // extended floating action should be in extended state
//                if (scrollY == 0) {
//                    extendedFloatingActionButton.extend();
//                }
//            }
//        });
        addButton!!.setOnClickListener(View.OnClickListener { add() })
        progressListener = setupProgressListener(this, progressBar)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_ACCOUNT)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_REPORT)
        //        form.add(new TransientAdapter<>(new StringBundleAdapter(), null), App.KEY_QUERY);
        form.add<Any, CharSequence>(TextViewCharSequenceAdapter(R.id.list_filter, null), App.KEY_QUERY)
        form.load(this, savedInstanceState)
        findViewById<View>(R.id.list_filter_btn).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val input = form.getValue<String>(App.KEY_QUERY)
                //                form.setValue(App.KEY_QUERY, input);
                logger.d("Changed filter:", form.getValue(App.KEY_QUERY), input)
                reload()
            }
        })
        if (!TextUtils.isEmpty(form.getValue(App.KEY_QUERY, String::class.java))) {
            // Have something in query
            filterPanel.visibility = View.VISIBLE
        }

        // pull-to-refresh swipeToRefresh
        mPtrFrame = findViewById<PtrClassicFrameLayout>(R.id.list_fragment_pull_to_refresh)
        mPtrFrame.setLastUpdateTimeRelateObject(this)
        mPtrFrame.setPtrHandler(object : PtrHandler {
            override fun onRefreshBegin(frame: PtrFrameLayout) {
                sync()
                frame.refreshComplete()
            }

            override fun checkCanDoRefresh(frame: PtrFrameLayout, content: View, header: View): Boolean {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, findViewById<View>(R.id.list_main_list), header)
            }
        })

        // update widget
        val intent = Intent(this, TaskReportWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        val ids = AppWidgetManager.getInstance(application)
                .getAppWidgetIds(ComponentName(application, TaskReportWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

    private fun reload() {
        // Show/hide filter
        val query = form.getValue<String>(App.KEY_QUERY)
        filterPanel.visibility = if (TextUtils.isEmpty(query)) View.GONE else View.VISIBLE
        list.load(form, updateTitleAction)
    }

    private fun annotate(json: JSONObject?) {
        val dialog = Intent(this, AnnotationDialog::class.java)
        dialog.putExtra(App.KEY_ACCOUNT, form.getValue(App.KEY_ACCOUNT, String::class.java))
        dialog.putExtra(App.KEY_EDIT_UUID, json!!.optString("uuid"))
        startActivityForResult(dialog, App.ANNOTATE_REQUEST)
    }

    private fun showAccountMenu(btn: View) {
        val menu = PopupMenu(this, btn)
        menu.inflate(R.menu.menu_account)
        var index = 0
        for (account in controller.accounts()) {
            menu.menu.add(R.id.menu_account_list, index++, 0, account.name)
                    .setOnMenuItemClickListener(newAccountMenu(controller.accountID(account)))
        }
        menu.setOnMenuItemClickListener(accountMenuListener)
        menu.show()
    }

    private fun newAccountMenu(accountName: String): MenuItem.OnMenuItemClickListener {
        return MenuItem.OnMenuItemClickListener {
            val listIntent = Intent(this@MainActivity, MainActivity::class.java)
            listIntent.putExtra(App.KEY_ACCOUNT, accountName)
            startActivity(listIntent)
            true
        }
    }

    private fun onNavigationMenu(item: MenuItem) {
        val account = form.getValue<String>(App.KEY_ACCOUNT)
        if (null == ac) return
        navigationDrawer!!.closeDrawers()
        when (item.itemId) {
            R.id.menu_nav_reload -> refreshAccount(account)
            R.id.menu_nav_run -> startActivity(ac!!.intentForRunTask())
            R.id.menu_nav_debug -> {
                val emailIntent = Intent(Intent.ACTION_SEND)
                emailIntent.type = "text/plain"
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Taskwarrior for Android debug output")
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(ac!!
                        .debugLogger()!!.file()))
                try {
                    startActivity(Intent.createChooser(emailIntent, "Share debug output..."))
                } catch (t: Throwable) {
                    controller.toastMessage("Failed to share debug file", true)
                }
            }
            R.id.menu_nav_settings -> {
                val intent = Intent(controller.context(), SettingsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun refreshAccount(account: String) {
        launch(Dispatchers.Default) {
            ac = controller.accountController(account, true)
            if (null != ac) {
                // Refreshed
                refreshReports()
            } else {
                this@MainActivity.finish() // Close
            }
        }
    }

    private fun changeStatus(json: JSONObject?) {
        val status = json!!.optString("status")
        val uuid = json.optString("uuid")
        val description = json.optString("description")
        if ("pending".equals(status, ignoreCase = true)) {
            // Mark as done
            doOp(String.format("Task '%s' marked done", description), uuid, "done")
        }
    }

    private fun doOp(message: String?, uuid: String, op: String, vararg ops: String) {
        ac?.let {
            launch(Dispatchers.Default) {
                val result: String? =
                        when {
                            "done".equals(op, ignoreCase = true) -> {
                                it.taskDone(uuid)
                            }
                            "delete".equals(op, ignoreCase = true) -> {
                                it.taskDelete(uuid)
                            }
                            "start".equals(op, ignoreCase = true) -> {
                                it.taskStart(uuid)
                            }
                            "stop".equals(op, ignoreCase = true) -> {
                                it.taskStop(uuid)
                            }
                            "denotate".equals(op, ignoreCase = true) -> {
                                it.taskDenotate(uuid, ops[0])
                            }
                            else -> {
                                "Not supported operation"
                            }
                        }
                if (null != result) {
                    controller.toastMessage(result, true)
                } else {
                    show_undo_msg(message)
                    list.reload()
                }
            }
        }
//        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
//
//        Snackbar.make(layout, "Subscription Deleted", Snackbar.LENGTH_LONG)
//                .setAction("Undo",  a -> {
////                            activeSubs.add(position-1, tmp)
////                            adapter!!.notifyDataSetChanged()
//                });
    }

    private fun add(vararg pairs: Pair<String, String>) {
        if (null == ac) return
        val intent = Intent(this, EditorActivity::class.java)
        showD(this, View.inflate(this, R.layout.activity_editor, null))
        ac!!.intentForEditor(intent, null)
        if (null != pairs) {
            val data = Bundle()
            val names = ArrayList<String>()
            for (pair in pairs) { // $COMMENT
                if (!TextUtils.isEmpty(pair.second)) { // Has data
                    data.putString(pair.first, pair.second)
                    names.add(pair.first)
                }
            }
            intent.putExtra(App.KEY_EDIT_DATA, data)
            intent.putStringArrayListExtra(App.KEY_EDIT_DATA_FIELDS, names)
        }
        startActivityForResult(intent, App.EDIT_REQUEST)
    }

    private fun edit(json: JSONObject?) {
        if (null == ac) return
        val intent = Intent(this, EditorActivity::class.java)
        if (ac!!.intentForEditor(intent, json!!.optString("uuid"))) { // Valid task
            startActivityForResult(intent, App.EDIT_REQUEST)
        } else {
            controller.toastMessage("Invalid task", false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        form.save(outState)
    }

    override fun onResume() {
        super.onResume()
        addButton!!.isEnabled = false
        if (checkAccount()) {
            addButton!!.isEnabled = true
            ac!!.listeners().add(progressListener, true)
            accountNameDisplay!!.text = ac!!.name()
            accountNameID!!.text = ac!!.id()
            refreshReports()
        }
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onDestroy() {
        if (null != ac) {
            ac!!.listeners().remove(progressListener)
        }
        controller.toastListeners().remove(this)
        mJob = Job()
        super.onDestroy()
    }

    private fun checkAccount(): Boolean {
        ac = controller.accountController(form)
        if (null != ac) { // Have account
            return true
        }
        val account = controller.currentAccount()
        ac = if (account == null) {
            // Start new account UI
            controller.addAccount(this)
            return false
        } else {
            logger.d("Refresh account:", account)
            form.setValue(App.KEY_ACCOUNT, account)
            controller.accountController(form) // Should be not null always
        }
        return true
    }

    private fun refreshReports() {
        launch() {
            var result: Map<String, String?> = ac!!.taskReports()
            // We're in UI thread
            navigation.menu.findItem(R.id.menu_nav_debug).isVisible = ac!!
                    .debugEnabled()
            val reportsMenu = navigation.menu.findItem(R.id.menu_nav_reports)
            reportsMenu.subMenu.clear()
            for ((key, value) in result) { // Add reports
                reportsMenu.subMenu.add(value).setIcon(R.drawable.ic_action_report)
                        .setOnMenuItemClickListener { // Show report
                            form.setValue(App.KEY_REPORT, key)
                            form.setValue(App.KEY_QUERY, null)
                            list.load(form, updateTitleAction)
                            reload()
                            false
                        }
            }
            // Report mode
            var report = form.getValue<String>(App.KEY_REPORT)
            if (null == report || !result.containsKey(report)) {
                report = result.keys.iterator().next() // First item
            }
            form.setValue(App.KEY_REPORT, report)
            list.load(form, updateTitleAction)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_reload -> list.reload()
            R.id.menu_tb_sync -> sync()
            R.id.menu_tb_undo -> undo()
            R.id.menu_tb_filter -> showFilter()
            R.id.menu_tb_add_shortcut -> createShortcut()
            R.id.menu_tb_run -> startActivity(ac!!.intentForRunTask())
        }
        return true
    }

    private fun createShortcut() {
        val bundle = Bundle()
        form.save(bundle, App.KEY_ACCOUNT, App.KEY_REPORT, App.KEY_QUERY)
        val query = bundle.getString(App.KEY_QUERY, "")
        var name = bundle.getString(App.KEY_REPORT, "")
        if (!TextUtils.isEmpty(query)) { // Have add. query
            name += " $query"
        }
        val shortcutIntent = Intent(this, MainActivity::class.java)
        shortcutIntent.putExtras(bundle)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        controller.input(this, "Shortcut name:", name,
                { value: CharSequence -> controller.createShortcut(shortcutIntent, value.toString().trim { it <= ' ' }) }
                , null)
    }

    private fun showFilter() {
        filterPanel.visibility = View.VISIBLE
        form.getView<View>(App.KEY_QUERY).requestFocus()
    }

    private fun undo() {
        if (null == ac) return
        launch(Dispatchers.Default) {
            val result: String? = ac!!.taskUndo()
            if (null != result) {
                controller.toastMessage(result, false)
            } else {
                list.reload()
            }
        }
    }

    private fun sync() {
        if (null == ac) {
            controller.toastMessage("Unable to sync", false)
            return
        }
        launch(Dispatchers.Default) {
            var result: String? = ac!!.taskSync()
            if (null != result) { // Error
                controller.toastMessage(result, false)
            } else {
                controller.toastMessage("Sync success", false)
                list.reload()
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (null == ac) return
        if (Activity.RESULT_OK == resultCode && App.SETTINGS_REQUEST == requestCode) { // Settings were modified
            logger.d("Reload after finish:", requestCode, resultCode)
            refreshAccount(form.getValue(App.KEY_ACCOUNT, String::class.java))
        }
    }

    override fun onMessage(message: String, showLong: Boolean) {
        runOnUiThread {
            val length: Int
            if (showLong) length = Toast.LENGTH_LONG else length = Toast.LENGTH_SHORT
            Toast.makeText(this, message, length).show()
        }
    }

    /**
     * This method will be called when a list item is removed
     *
     * @param position The position of the item within data set
     */
    fun onItemRemoved(position: Int) {
        val snackbar = Snackbar.make(
                findViewById(R.id.container),
                "1 item removed",
                Snackbar.LENGTH_LONG)
        snackbar.setAction("UNDO") { v: View? -> onItemUndoActionClicked() }
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_action_color_done))
        snackbar.show()
    }

    /**
     * This method will be called when a list item is pinned
     *
     * @param position The position of the item within data set
     */
    fun onItemPinned(position: Int) {
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
    fun onItemClicked(position: Int) {
//        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);
//        AbstractDataProvider.Data data = getDataProvider().getItem(position);
//
//        if (data.isPinned()) {
//            // unpin if tapped the pinned item
//            data.setPinned(false);
//            ((SwipeOnLongPressExampleFragment) fragment).notifyItemChanged(position);
//        }
    }

    private fun onItemUndoActionClicked() {
//        int position = getDataProvider().undoLastRemoval();
//        if (position >= 0) {
//            final Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VIEW);
//            ((SwipeOnLongPressExampleFragment) fragment).notifyItemInserted(position);
//        }
    }

    companion object {
        @JvmStatic
        fun setupProgressListener(activity: Activity, bar: ProgressBar?): TaskListener {
            val controller: Controller = App.controller()
            val handler = Handler(activity.mainLooper)
            return object : TaskListener {
                var balance = 0
                override fun onStart() {
                    if (null == bar) return
                    balance++
                    handler.postDelayed({
                        if (balance == 0) {
                            return@postDelayed
                        }
                        activity.runOnUiThread({ bar.visibility = View.VISIBLE })
                    }, 750)
                }

                override fun onFinish() {
                    if (null == bar) return
                    if (balance > 0) {
                        balance--
                    }
                    if (balance > 0) {
                        return
                    }
                    activity.runOnUiThread { bar.visibility = View.GONE }
                }

                override fun onQuestion(question: String?, callback: DataUtil
                .Callback<Boolean?>?) {
                    activity.runOnUiThread {
                        // Show dialog
                        controller.question(activity, question,
                                { callback?.call(true) },
                                { callback?.call(false) })
                    }
                }
            }
        }
    }
}