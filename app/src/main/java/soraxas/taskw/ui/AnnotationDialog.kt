package soraxas.taskw.ui

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kvj.bravo7.form.FormController
import org.kvj.bravo7.form.impl.ViewFinder.ActivityViewFinder
import org.kvj.bravo7.form.impl.bundle.StringBundleAdapter
import org.kvj.bravo7.form.impl.widget.TextViewCharSequenceAdapter
import org.kvj.bravo7.form.impl.widget.TransientAdapter
import soraxas.taskw.App
import soraxas.taskw.App.Companion.controller
import soraxas.taskw.R

/**
 * Created by kvorobyev on 11/25/15.
 */
class AnnotationDialog : AppCompatActivity() {
    var controller = controller()
    var form = FormController(ActivityViewFinder(this))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_annotation)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_ACCOUNT)
        form.add<Any, String?>(TransientAdapter(StringBundleAdapter(), null), App.KEY_EDIT_UUID)
        form.add<Any, CharSequence>(TextViewCharSequenceAdapter(R.id.ann_text, ""), App.KEY_EDIT_TEXT)
        form.load(this, savedInstanceState)
        findViewById<View>(R.id.ann_cancel_btn).setOnClickListener { v: View? -> doFinish() }
        findViewById<View>(R.id.ann_ok_btn).setOnClickListener { v: View? -> doSave() }
    }

    private fun doSave() {
        val text = form.getValue<String>(App.KEY_EDIT_TEXT)
        if (TextUtils.isEmpty(text)) { // Nothing to save
            controller.toastMessage("Input is mandatory", false)
            return
        }
        val ac = controller.accountController(form.getValue(App.KEY_ACCOUNT, String::class.java))
        GlobalScope.launch {
            val uuid = form.getValue<String>(App.KEY_EDIT_UUID)
            val result: String? = ac.taskAnnotate(uuid, text)
            if (null != result) { // Error
                controller.toastMessage(result, false)
            } else {
                setResult(Activity.RESULT_OK)
                this@AnnotationDialog.finish()
            }
        }
    }

    private fun doFinish() {
        if (form.changed()) { // Ask for confirmation
            controller.question(this@AnnotationDialog, "There are some changes, discard?", Runnable { finish() }, null)
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        doFinish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        form.save(outState)
    }
}