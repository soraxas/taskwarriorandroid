package soraxas.taskw.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.form.impl.ViewFinder.ActivityViewFinder
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter
import org.kvj.bravo7.form.impl.widget.TransientAdapter
import org.kvj.bravo7.log.Logger
import soraxas.taskw.App
import soraxas.taskw.App.Companion.controller
import soraxas.taskw.R
import java.io.InputStream

/**
 * Created by vorobyev on 11/30/15.
 */
class TextEditor : AppCompatActivity() {
    var form = FormController(ActivityViewFinder(this))
    private lateinit var toolbar: Toolbar
    var controller = controller()
    var logger = Logger.forInstance(this)!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_editor)
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        form.add<Any, CharSequence>(TextViewCharSequenceAdapter(R.id.text_editor_input, null).trackOrigin(true), App.KEY_TEXT_INPUT)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_TEXT_TARGET)
        form.load(this, savedInstanceState)
        //        logger.d("On create:", form.getValue(App.KEY_TEXT_INPUT), form.getValue(App.KEY_TEXT_TARGET));
        if (null == form.getValue(App.KEY_TEXT_TARGET)) {
            // No data - load from Uri
            loadText(intent)
        } else {
            updateToolbar()
        }
    }

    private fun updateToolbar() {
        toolbar.subtitle = form.getValue(App.KEY_TEXT_TARGET, String::class.java)
    }

    private fun loadText(intent: Intent) {
        lateinit var file: InputStream
        // Load
        lateinit var _uri: android.net.Uri
        try {
            _uri = intent.data!!
            if ("content" == _uri.scheme || "file" == _uri.scheme) {
                file = contentResolver.openInputStream(_uri)
                if (file == null)
                    throw Exception("Error in opening input stream")
            }
        } catch (e: Exception) {
            logger.e(e, "Error getting file:", intent.data, intent.data.path)
            // Invalid file
            controller.toastMessage("Invalid file provided", true)
            finish()
            return
        }
        val finalFile: InputStream = file
        GlobalScope.launch {
            val result: String? = controller.readFile(finalFile)
            logger.d("File loaded:", _uri, true)
            if (null == result) {
                controller.toastMessage("File IO error", true)
                this@TextEditor.finish()
            } else {
                form.setValue(App.KEY_TEXT_TARGET, _uri.toString(), true)
                form.setValue(App.KEY_TEXT_INPUT, result.trim { it <= ' ' }, true)
                updateToolbar()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        form.save(outState)
    }

    override fun onBackPressed() {
        if (!form.changed()) {
            super.onBackPressed() // Close
            return
        }
        logger.d("Changes:", form.changes())
        controller.question(this, "There are some changes, discard?", Runnable { finish() }, null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_text_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_tb_save -> save()
        }
        return true
    }

    private fun save() {
        if (!form.changed()) {
            finish() //
        }
        val fileName = form.getValue<String>(App.KEY_TEXT_TARGET)
        val text = form.getValue<String>(App.KEY_TEXT_INPUT)

        GlobalScope.launch {
            var saveMe = text!!
            if (!saveMe.isEmpty() && !saveMe.endsWith("\n")) { // Auto-add new line
                saveMe += "\n"
            }
            var result: Boolean = controller.saveFile(fileName, saveMe)
            if (!result) {
                // Failure
                controller.toastMessage("File write failure", true)
            } else {
                controller.toastMessage("File saved", false)
                setResult(Activity.RESULT_OK)
                this@TextEditor.finish()
            }
        }
    }
}