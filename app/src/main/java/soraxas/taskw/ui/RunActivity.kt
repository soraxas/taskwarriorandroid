package soraxas.taskw.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.form.impl.ViewFinder.ActivityViewFinder
import org.kvj.bravo7.form.impl.bundle.ListStringBundleAdapter
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter
import org.kvj.bravo7.form.impl.widget.TransientAdapter
import org.kvj.bravo7.log.Logger
import soraxas.taskw.App
import soraxas.taskw.App.Companion.controller
import soraxas.taskw.R
import soraxas.taskw.data.AccountController
import soraxas.taskw.data.AccountController.ListAggregator
import soraxas.taskw.data.AccountController.TaskListener
import soraxas.taskw.ui.MainActivity.Companion.setupProgressListener
import soraxas.taskw.ui.RunActivity.RunAdapter.RunAdapterItem
import java.util.*

/**
 * Created by vorobyev on 12/1/15.
 */
class RunActivity : AppCompatActivity() {
    var form = FormController(ActivityViewFinder(this))
    var controller = controller()
    var logger = Logger.forInstance(this)
    private var ac: AccountController? = null
    private var adapter: RunAdapter? = null
    private var progressListener: TaskListener? = null
    private var mShareActionProvider: ShareActionProvider? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        val list = findViewById<View>(R.id.run_output) as RecyclerView
        list.layoutManager = LinearLayoutManager(this)
        setSupportActionBar(toolbar)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_ACCOUNT)
        form.add<Any, ArrayList<String>?>(TransientAdapter(ListStringBundleAdapter(), null), App.KEY_RUN_OUTPUT)
        form.add<Any, CharSequence>(TextViewCharSequenceAdapter(R.id.run_command, null), App.KEY_RUN_COMMAND)
        form.load(this, savedInstanceState)

        // Enter key trigger button press of running command
        val edittext = findViewById<EditText>(R.id.run_command)
        edittext.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER ||
                    actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                //do what you want on the press of 'done'
                run()
                // close keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(edittext.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })
        // Set enter key custom test
        edittext.setImeActionLabel("Run", KeyEvent.KEYCODE_ENTER)
        progressListener = setupProgressListener(this, findViewById<View>(R.id.progress) as ProgressBar)
        ac = controller.accountController(form)
        if (null == ac) {
            controller.toastMessage("Invalid arguments", false)
            finish()
            return
        }

        adapter = RunAdapter(form.getValue<ArrayList<String>>(App.KEY_RUN_OUTPUT))
        list.adapter = adapter
        toolbar.subtitle = ac!!.name()
    }

    private fun shareAll() {
        val text = adapter!!.allText()
        if (TextUtils.isEmpty(text)) { // Error
            controller.toastMessage("Nothing to share", false)
            return
        }
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        sendIntent.type = "text/plain"
        if (null != mShareActionProvider) {
            logger.d("Share provider set")
            mShareActionProvider!!.setShareIntent(sendIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_run, menu)

        // Locate MenuItem with ShareActionProvider
        val item = menu.findItem(R.id.menu_tb_run_share)

        // Fetch and store ShareActionProvider
        mShareActionProvider = MenuItemCompat.getActionProvider(item) as ShareActionProvider

        // Return true to display menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_run_run -> {
                run()
                return true
            }
            R.id.menu_tb_run_copy -> {
                copyAll()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun copyAll() {
        val text = adapter!!.allText()
        if (TextUtils.isEmpty(text)) { // Error
            controller.toastMessage("Nothing to copy", false)
            return
        }
        controller.copyToClipboard(text)
    }

    private fun run() {
        val input = form.getValue<String>(App.KEY_RUN_COMMAND)
        if (TextUtils.isEmpty(input)) {
            controller.toastMessage("Input is empty", false)
            return
        }
        adapter!!.clear()
        var out = ListAggregator()
        var err = ListAggregator()
        GlobalScope.launch {
            val result = ac!!.taskCustom(input, out, err)
            out.data().toMutableList().addAll(err.data())
            adapter!!.addAll(out.data().filterNotNull())
            shareAll()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (null != adapter) {
            form.setValue(App.KEY_RUN_OUTPUT, adapter!!.data)
        }
        form.save(outState)
    }

    override fun onResume() {
        super.onResume()
        ac!!.listeners().add(progressListener, true)
    }

    override fun onPause() {
        super.onPause()
        ac!!.listeners().remove(progressListener)
    }

    internal inner class RunAdapter(data: ArrayList<String>) : RecyclerView.Adapter<RunAdapterItem>() {
        var data = ArrayList<String>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunAdapterItem {
            return RunAdapterItem(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_run_output, parent, false))
        }

        override fun onBindViewHolder(holder: RunAdapterItem, position: Int) {
            holder.text.text = data[position]
        }

        override fun getItemCount(): Int {
            return data.size
        }

        fun clear() {
            notifyItemRangeRemoved(0, data.size)
            data.clear()
        }

        @Synchronized
        fun addAll(data: List<String>) {
            val from = itemCount
            this.data.addAll(data)
            notifyItemRangeInserted(from, data.size)
        }

        fun allText(): CharSequence {
            val sb = StringBuilder()
            for (line in data) { // Copy to
                if (sb.isNotEmpty()) {
                    sb.append('\n')
                }
                sb.append(line)
            }
            return sb
        }

        internal inner class RunAdapterItem(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnLongClickListener {
            val text: TextView
            override fun onLongClick(v: View): Boolean {
                controller.copyToClipboard(data[adapterPosition])
                return true
            }

            init {
                itemView.setOnLongClickListener(this)
                text = itemView.findViewById<View>(R.id.run_item_text) as TextView
            }
        }

        init {
            this.data.addAll(data)
        }
    }
}