package soraxas.taskw.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.kvj.bravo7.form.BundleAdapter
import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.form.impl.ViewFinder.ActivityViewFinder
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter
import org.kvj.bravo7.form.impl.widget.TransientAdapter
import org.kvj.bravo7.log.Logger
import org.kvj.bravo7.util.DataUtil
import soraxas.taskw.App
import soraxas.taskw.App.Companion.controller
import soraxas.taskw.R
import soraxas.taskw.common.Helpers
import soraxas.taskw.data.AccountController
import soraxas.taskw.data.AccountController.Companion.escape
import soraxas.taskw.data.AccountController.TaskListener
import soraxas.taskw.ui.EditorActivity
import soraxas.taskw.ui.MainActivity.Companion.setupProgressListener
import soraxas.taskw.utils.AutoTagsSuggestAdapter
import java.util.*

/**
 * Created by kvorobyev on 11/21/15.
 */
class EditorActivity : AppCompatActivity() {
    private val job = Job()
    private val ioiScope = CoroutineScope(Dispatchers.IO + job)

    private var toolbar: Toolbar? = null
    private var editor: Editor? = null
    private val form = FormController(ActivityViewFinder(this))
    var controller = controller()
    var logger = Logger.forInstance(this)
    private var priorities: List<String>? = null
    private var progressListener: TaskListener? = null
    private var ac: AccountController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        editor = supportFragmentManager.findFragmentById(R.id.editor_editor) as Editor?
        val progressBar = findViewById<View>(R.id.progress) as ProgressBar
        setSupportActionBar(toolbar)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_ACCOUNT)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_EDIT_UUID)
        form.add<Any, Bundle?>(TransientAdapter(object : BundleAdapter<Bundle?>() {
            override operator fun get(bundle: Bundle, name: String, def: Bundle?):
                    Bundle? {
                return bundle.getBundle(name)
            }

            override operator fun set(bundle: Bundle, name: String, value: Bundle?) {
                bundle.putBundle(name, value)
            }
        }, null).oneShot(), App.KEY_EDIT_DATA)
        form.add<Any, ArrayList<String>?>(TransientAdapter<ArrayList<String>?>(object : BundleAdapter<ArrayList<String>?>() {
            override operator fun get(bundle: Bundle, name: String, def:
            ArrayList<String>?): ArrayList<String>? {
                return bundle.getStringArrayList(name)
            }

            override operator fun set(bundle: Bundle, name: String, value:
            ArrayList<String>?) {
                bundle.putStringArrayList(name, value)
            }
        }, null).oneShot(), App.KEY_EDIT_DATA_FIELDS)
        editor!!.initForm(form)
        form.load(this, savedInstanceState)
        onSharedIntent()
        ac = controller.accountController(form)
        if (null == ac) {
            // try to load from default account first
            ac = controller.accountController(controller.currentAccount())
            if (null == ac) {
                finish()
                controller.toastMessage("Invalid arguments", false)
                return
            }
        }
        toolbar!!.subtitle = ac!!.name()
        progressListener = setupProgressListener(this, progressBar)
        ioiScope.launch {
            val result: List<String> = ac!!.taskPriority()
            editor!!.setupPriorities(result)
            priorities = result
            form.load(this@EditorActivity, savedInstanceState, App.KEY_EDIT_PRIORITY)
            editor!!.show(form)
            val formData = form.getValue<Bundle>(App.KEY_EDIT_DATA)
            val fields = form.getValue<List<String>>(App.KEY_EDIT_DATA_FIELDS)
            logger.d("Edit:", formData, fields)
            if (null != formData && null != fields) { // Have data
                for (f in fields) { // $COMMENT
                    form.setValue(f, formData.getString(f))
                }
            }
        }

        // populate project auto complete
        val autoCompProject = findViewById<View>(R.id.editor_project) as AutoCompleteTextView
        val adapter = ArrayAdapter(this, R.layout.auto_suggestion, ac!!.projects)
        autoCompProject.threshold = 0 //will start working from first character
        autoCompProject.setAdapter(adapter)

        // populate tag auto complete
        val autoCompTag = findViewById<View>(R.id.editor_tags) as AutoCompleteTextView
        val single_word_adapter = AutoTagsSuggestAdapter(this, R.layout.auto_suggestion, ac!!.tags)
        autoCompTag.threshold = 0 //will start working from first character
        autoCompTag.setAdapter(single_word_adapter)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        form.save(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        if (null != editor && !editor!!.adding(form)) { // New item mode
            menu.findItem(R.id.menu_tb_add_another).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_save -> doSave(false)
            R.id.menu_tb_add_another -> doSave(true)
            R.id.menu_tb_add_shortcut -> createShortcut()
        }
        return true
    }

    private fun createShortcut() {
        val bundle = Bundle()
        form.save(bundle)
        bundle.remove(App.KEY_EDIT_UUID) // Just in case
        val shortcutIntent = Intent(this, EditorActivity::class.java)
        shortcutIntent.putExtras(bundle)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        controller.input(this, "Shortcut name:", ac!!.name(), object : DataUtil.Callback<CharSequence?> {
            override fun call(value: CharSequence?): Boolean {
                controller.createShortcut(shortcutIntent, value.toString().trim { it <= ' ' })
                return true
            }
        }, null)
    }

    override fun onResume() {
        super.onResume()
        ac!!.listeners().add(progressListener, true)
    }

    override fun onPause() {
        super.onPause()
        ac!!.listeners().remove(progressListener)
    }

    override fun onBackPressed() {
        if (!form.changed()) { // No changes - just close
            super.onBackPressed()
            return
        }
        logger.d("Changed:", form.changes())
        controller.question(this, "There are some changes, discard?", Runnable { super@EditorActivity.onBackPressed() }, null)
    }

    private fun propertyChange(key: String, modifier: String): String {
        var value = form.getValue<String>(key)
        if (TextUtils.isEmpty(value)) {
            value = ""
        }
        return String.format("%s:%s", modifier, value)
    }

    private fun save(): String? {
        if (!form.changed()) { // No change - no save
            return "Nothing has been changed"
        }
        val description = form.getValue<String>(App.KEY_EDIT_DESCRIPTION)!!
        if (TextUtils.isEmpty(description)) { // Empty desc
            return "Description is mandatory"
        }
        val changes: MutableList<String> = ArrayList()
        for (key in form.changes()) { // Make changes
            if (App.KEY_EDIT_DESCRIPTION == key) { // Direct
                changes.add(escape(description))
            }
            if (App.KEY_EDIT_PROJECT == key) { // Direct
                changes.add(propertyChange(key, "project"))
            }
            if (App.KEY_EDIT_DUE == key) { // Direct
                changes.add(propertyChange(key, "due"))
            }
            if (App.KEY_EDIT_SCHEDULED == key) { // Direct
                changes.add(propertyChange(key, "scheduled"))
            }
            if (App.KEY_EDIT_WAIT == key) { // Direct
                changes.add(propertyChange(key, "wait"))
            }
            if (App.KEY_EDIT_UNTIL == key) { // Direct
                changes.add(propertyChange(key, "until"))
            }
            if (App.KEY_EDIT_RECUR == key) { // Direct
                changes.add(propertyChange(key, "recur"))
            }
            if (App.KEY_EDIT_PRIORITY == key) { // Direct
                changes.add(String.format("priority:%s", priorities
                        ?.get(form.getValue(App.KEY_EDIT_PRIORITY, Int::class.java)!!)))
            }
            if (App.KEY_EDIT_TAGS == key) { // Direct
                val tags: List<String> = ArrayList()
                val tagsStr = form.getValue<String>(App.KEY_EDIT_TAGS)!!
                Collections.addAll(tags.toMutableList(), *tagsStr.split(" ").toTypedArray())
                changes.add(String.format("tags:%s", Helpers.join(",", tags)))
            }
        }
        val uuid = form.getValue<String>(App.KEY_EDIT_UUID)!!
        val completed = form.getValue(App.KEY_EDIT_STATUS, Int::class.java)!! > 0
        logger.d("Saving change:", uuid, changes, completed)
        return if (TextUtils.isEmpty(uuid)) { // Add new
            if (completed) ac!!.taskLog(changes) else ac!!.taskAdd(changes)
        } else {
            ac!!.taskModify(uuid, changes)
        }
    }

    private fun doSave(addAnother: Boolean) {
        ioiScope.launch {
            val result: String? = save()
            if (!TextUtils.isEmpty(result)) { // Failed
                controller.toastMessage(result, true)
            } else {
                controller.toastMessage("Task added", false)
                this@EditorActivity.setResult(Activity.RESULT_OK)
                if (addAnother) { // Keep everything except description
                    form.setValue(App.KEY_EDIT_DESCRIPTION, "")
                    form.getView<View>(App.KEY_EDIT_DESCRIPTION).requestFocus()
                } else { // Finish activity
                    this@EditorActivity.finish()
                }
            }
        }
    }

    private fun onSharedIntent() {
        val receivedIntent = intent
        val receivedAction = receivedIntent.action
        val receivedType = receivedIntent.type
        if (receivedAction != null && receivedAction == Intent.ACTION_SEND) {

            // check mime type
            if (receivedType.startsWith("text/")) {
                val receivedText = receivedIntent
                        .getStringExtra(Intent.EXTRA_TEXT)
                if (receivedText != null) {
                    //do your stuff
                    form.setValue(App.KEY_EDIT_DESCRIPTION, receivedText)
                    form.setValue(App.KEY_ACCOUNT, controller.currentAccount())
                }
            }
        }
    }
}