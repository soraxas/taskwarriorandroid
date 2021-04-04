package soraxas.taskw.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kvj.bravo7.log.Logger
import soraxas.taskw.App
import soraxas.taskw.App.Companion.controller

class SyncIntentReceiver : BroadcastReceiver() {
    var controller = controller()
    var logger = Logger.forInstance(this)
    override fun onReceive(context: Context, intent: Intent) {
        // Lock and run sync
        val lock = controller.lock()
        lock.acquire(10 * 60 * 1000L /*10 minutes*/)
        logger.d("Sync from receiver", intent.data)
        GlobalScope.launch {
            var account = intent.getStringExtra(App.KEY_ACCOUNT)
            if (TextUtils.isEmpty(account)) {
                account = controller.currentAccount()
            }
            val s: String? = controller.accountController(account).taskSync()
            logger.d("Sync from receiver done:", s)
            controller.toastMessage(s, false)
            lock.release()
        }
    }
}