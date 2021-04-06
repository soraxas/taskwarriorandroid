package soraxas.taskw.data

import android.app.PendingIntent
import android.content.Intent
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.Uri
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kvj.bravo7.log.Logger
import org.kvj.bravo7.log.Logger.LoggerLevel
import org.kvj.bravo7.util.Compat
import org.kvj.bravo7.util.DataUtil
import org.kvj.bravo7.util.Listeners
import org.kvj.bravo7.util.Listeners.ListenerEmitter
import soraxas.taskw.App
import soraxas.taskw.R
import soraxas.taskw.common.Helpers
import soraxas.taskw.sync.SSLHelper
import soraxas.taskw.ui.MainActivity
import soraxas.taskw.ui.RunActivity
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class AccountController(private val controller: Controller, private val id: String, private val accountName: String) {

    private val job = Job()
    private val accountControllerScope = CoroutineScope(Dispatchers.Main + job)

    private var acceptThread: Thread? = null
    private val notificationTypes: MutableSet<NotificationType> = HashSet()
    var fileLogger: FileLogger? = null
    fun taskrc(): File {
        return File(tasksFolder, TASKRC)
    }

    fun taskAnnotate(uuid: String, text: String): String? {
        val err = StringAggregator()
        if (!callTask(outConsumer, err, uuid, "annotate", escape(text))) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskDenotate(uuid: String?, text: String): String? {
        val err = StringAggregator()
        if (!callTask(outConsumer, err, uuid!!, "denotate", escape(text))) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskUndo(): String? {
        val err = StringAggregator()
        if (!callTask(outConsumer, err, "undo")) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun name(): String {
        return accountName
    }

    fun createQueryInfo(query: String): ReportInfo {
        val info = ReportInfo()
        info.description = query
        info.sort["urgency"] = false
        info.sort["description"] = false
        info.query = query
        if (!query.toLowerCase().contains("status:")) info.query += " status:pending"
        info.priorities = taskPriority()
        for (f in defaultFields) {
            info.fields[f] = ""
        }
        return info
    }

    fun taskCustom(command: String, out: StreamConsumer, err: StreamConsumer): Int {
        val params: MutableList<String> = ArrayList()
        if (!TextUtils.isEmpty(socketName)) { // Have socket opened - add key
            params.add("rc.taskd.socket=$socketName")
        }
        Collections.addAll(params, *command.split(" ").toTypedArray())
        val result = callTask(out, err, false, *params.toTypedArray())
        err.eat("")
        err.eat(String.format("Exit code: %d", result))
        return result
    }

    fun id(): String {
        return id
    }

    fun debugEnabled(): Boolean {
        return fileLogger != null
    }

    fun debugLogger(): FileLogger? {
        return fileLogger
    }

    interface TaskListener {
        fun onStart()
        fun onFinish()
        fun onQuestion(question: String?, callback: DataUtil.Callback<Boolean?>?)
    }

    private val taskListeners: Listeners<TaskListener> = object : Listeners<TaskListener>() {
        protected override fun onAdd(listener: TaskListener) {
            super.onAdd(listener)
            if (active) { // Run onStart
                listener.onStart()
            }
        }
    }
    private var active = false
    private val socketName: String
    var logger: Logger = Logger.forInstance(this)!!
    private val syncSocket: LocalServerSocket?
    private val tasksFolder: File?

    interface StreamConsumer {
        fun eat(line: String?)
    }

    private inner class ToLogConsumer(private val level: LoggerLevel, private val prefix: String) : StreamConsumer {
        override fun eat(line: String?) {
            logger.log(level, prefix, line)
        }

    }

    private val errConsumer: StreamConsumer = ToLogConsumer(LoggerLevel.Warning, "ERR:")
    private val outConsumer: StreamConsumer = ToLogConsumer(LoggerLevel.Info, "STD:")
    private fun initLogger() {
        fileLogger = null
        val conf = taskSettings(androidConf("debug"))
        if ("y".equals(conf["android.debug"], ignoreCase = true)) { // Enabled
            fileLogger = FileLogger(tasksFolder)
            debug("Profile:", accountName, id, fileLogger!!.logFile(tasksFolder))
        }
    }

    private fun debug(vararg params: Any) {
        if (null != fileLogger) { // Enabled
            fileLogger!!.log(*params)
        }
    }

    private fun loadNotificationTypes() {
        accountControllerScope.launch {
            val config = taskSettings(androidConf("sync.notification"))
            val s: String = when {
                config.isEmpty() -> "all"
                else -> config.values.iterator().next()
            }
            notificationTypes.clear()
            if ("all" == s) { // All types
                notificationTypes.add(NotificationType.Sync)
                notificationTypes.add(NotificationType.Success)
                notificationTypes.add(NotificationType.Error)
            } else {
                for (type in s.split(",").toTypedArray()) { // Search type
                    for (nt in NotificationType.values()) { // Check name
                        if (nt.name.equals(type.trim { it <= ' ' }, ignoreCase = true)) { // Found
                            notificationTypes.add(nt)
                            break
                        }
                    }
                }
            }
        }
    }

    private inner class StringAggregator : StreamConsumer {
        var builder = StringBuilder()
        override fun eat(line: String?) {
            if (builder.isNotEmpty()) {
                builder.append('\n')
            }
            builder.append(line)
        }

        fun text(): String {
            return builder.toString()
        }
    }

    class ListAggregator : StreamConsumer {
        var data: MutableList<String?> = ArrayList()
        override fun eat(line: String?) {
            data.add(line)
        }

        fun data(): List<String?> {
            return data
        }
    }

    enum class TimerType(val type: String) {
        Periodical("periodical"), AfterError("onerror"), AfterChange("onchange");

    }

    enum class NotificationType(val _name: String) {
        Sync("sync"), Success("success"), Error("error");

    }

    fun stop() {
        controller.cancelAlarm(syncIntent("alarm"))
        if (null != syncSocket) {
            try {
                syncSocket.close()
            } catch (e: Exception) {
                logger.w(e, "Failed to close socket")
            }
        }
    }

    fun scheduleSync(type: TimerType) {
        accountControllerScope.launch {
            val config = taskSettings(androidConf(String.format("sync.%s", type.type)))
            val minutes: Double;
            minutes = if (config.isEmpty()) {
                0.0
            } else {
                try {
                    config.values.iterator().next().toDouble()
                } catch (e: Exception) {
                    logger.w("Failed to parse:", e.message, config)
                    0.0
                }
            }
            if (minutes <= 0) {
                logger.d("Ignore schedule - not configured", type)
            } else {
                val c = Calendar.getInstance()
                c.add(Calendar.SECOND, (minutes * 60.0).toInt())
                controller.scheduleAlarm(c.time, syncIntent("alarm"))
                logger.d("Scheduled:", c.time, type)
            }
        }
    }

    private fun androidConf(format: String): String {
        return String.format("android.%s", format)
    }

    private fun toggleSyncNotification(n: NotificationCompat.Builder, type: NotificationType): Boolean {
        return if (notificationTypes.contains(type)) { // Have to show
            val intent = Intent(controller.context(), MainActivity::class.java)
            intent.putExtra(App.KEY_ACCOUNT, id)
            n.setContentIntent(PendingIntent.getActivity(controller.context(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
            controller.notify(Controller.NotificationType.Sync, accountName, n)
            true
        } else {
            controller.cancel(Controller.NotificationType.Sync, accountName)
            false
        }
    }

    fun taskSync(): String? {
        var n = controller.newNotification(accountName)
        n.setOngoing(true)
        n.setContentText("Sync is in progress")
        n.setTicker("Sync is in progress")
        n.priority = NotificationCompat.PRIORITY_DEFAULT
        toggleSyncNotification(n, NotificationType.Sync)
        val err = StringAggregator()
        val out = StringAggregator()
        val result = callTask(out, err, "rc.taskd.socket=$socketName", "sync")
        debug("Sync result:", result)
        logger.d("Sync result:", result, "ERR:", err.text(), "OUT:", out.text())
        n = controller.newNotification(accountName)
        n.setOngoing(false)
        return if (result) { // Success
            n.setContentText("Sync complete")
            n.priority = NotificationCompat.PRIORITY_MIN
            n.addAction(R.drawable.ic_action_sync, "Sync again", syncIntent("notification"))
            toggleSyncNotification(n, NotificationType.Success)
            scheduleSync(TimerType.Periodical)
            null
        } else {
            val error = err.text()
            debug("Sync error output:", error)
            n.setContentText("Sync failed")
            n.setTicker("Sync failed")
            n.setSubText(error)
            n.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            n.priority = NotificationCompat.PRIORITY_DEFAULT
            n.addAction(R.drawable.ic_action_sync, "Retry now", syncIntent("notification"))
            toggleSyncNotification(n, NotificationType.Error)
            scheduleSync(TimerType.AfterError)
            error
        }
    }

    var linePatthern = Pattern.compile("^([A-Za-z0-9\\._]+)\\s+(\\S.*)$")
    private fun taskSetting(name: String): String? {
        return taskSettings(name)[name]
    }

    private fun taskSettings(vararg names: String): Map<String, String> {
        val result: MutableMap<String, String> = LinkedHashMap()
        callTask(object : StreamConsumer {
            override fun eat(line: String?) {
                val m = linePatthern.matcher(line)
                if (m.find()) {
                    val keyName = m.group(1).trim { it <= ' ' }
                    val keyValue = m.group(2).trim { it <= ' ' }
                    for (name in names) {
                        if (name.equals(keyName, ignoreCase = true)) {
                            result[name] = keyValue
                            break
                        }
                    }
                }
            }
        }, errConsumer, "rc.defaultwidth=1000", "show")
        return result
    }

    private abstract inner class PatternLineConsumer : StreamConsumer {
        override fun eat(line: String?) {
            val m = linePatthern.matcher(line)
            if (m.find()) {
                val keyName = m.group(1).trim { it <= ' ' }
                val keyValue = m.group(2).trim { it <= ' ' }
                eat(keyName, keyValue)
            }
        }

        abstract fun eat(key: String, value: String)
    }

    fun taskReports(): Map<String, String> {
        val onlyThose: MutableList<String> = ArrayList() // Save names of pre-configured reports here
        val settings = taskSettings(androidConf("reports"),
                androidConf("report.default"))
        val list = settings[androidConf("reports")]
        var defaultReport = settings[androidConf("report.default")]
        if (TextUtils.isEmpty(defaultReport)) {
            defaultReport = "next"
        }
        if (!TextUtils.isEmpty(list)) {
            onlyThose.addAll(split2(list, ","))
        }
        val keys: MutableList<String> = ArrayList()
        val values: MutableList<String> = ArrayList()
        callTask(object : PatternLineConsumer() {
            override fun eat(key: String, value: String) {
                if (controller.BUILTIN_REPORTS.contains(key)) { // Skip builtin reports
                    return
                }
                if (!onlyThose.isEmpty() && !onlyThose.contains(key)) {
                    return  // Skip not selected
                }
                keys.add(key)
                values.add(value)
            }
        }, errConsumer, "reports")
        if (onlyThose.isEmpty() && !keys.isEmpty()) { // All reports - remove last
            keys.removeAt(keys.size - 1)
            values.removeAt(values.size - 1)
        }
        val result = LinkedHashMap<String, String>()
        if (keys.contains(defaultReport)) {
            // Move default to the top
            val index = keys.indexOf(defaultReport)
            keys.add(0, keys[index])
            keys.removeAt(index + 1)
            values.add(0, values[index])
            values.removeAt(index + 1)
        }
        for (i in keys.indices) {
            result[keys[i]] = values[i]
        }
        if (result.isEmpty()) { // Invalid configuration
            result["next"] = "[next] Fail-safe report"
        }
        //        logger.d("Reports after sort:", keys, values, defaultReport, result);
        return result
    }

    fun taskReportInfo(name: String?, query: String): ReportInfo {
        val info = ReportInfo()
        callTask(object : PatternLineConsumer() {
            override fun eat(key: String, value: String) {
                if (key.endsWith(".columns")) {
                    val parts = value.split(",").toTypedArray()
                    for (p in parts) {
                        var name = p
                        var type = ""
                        if (p.contains(".")) {
                            name = p.substring(0, p.indexOf("."))
                            type = p.substring(p.indexOf(".") + 1)
                        }
                        info.fields[name] = type
                    }
                }
                if (key.endsWith(".sort")) {
                    val parts = value.split(",").toTypedArray()
                    for (p in parts) {
                        var _p = p
                        if (_p.endsWith("/")) _p = _p.substring(0, _p.length - 1)
                        info.sort[_p.substring(0, _p.length - 1)] = _p[_p.length - 1] ==
                                '+'
                    }
                }
                if (key.endsWith(".filter")) {
                    var q = value
                    if (!TextUtils.isEmpty(query)) { // Add query
                        q += " $query"
                    }
                    info.query = q
                }
                if (key.endsWith(".description")) {
                    info.description = value
                }
            }
        }, errConsumer, "show", String.format("report.%s.", name))
        info.priorities = taskPriority()
        if (!info.sort.containsKey("description")) {
            info.sort["description"] = true
        }
        return info
    }

    fun taskPriority(): List<String> {
        // Get all priorities
        val result: MutableList<String> = ArrayList()
        callTask(object : PatternLineConsumer() {
            override fun eat(key: String, value: String) {
                result.addAll(split2(value, ","))
                logger.d("Parsed priority:", value, result)
            }
        }, errConsumer, "show", "uda.priority.values")
        return result
    }

    private fun readStream(stream: InputStream, outputStream: OutputStream?,
                           consumer: StreamConsumer?): Thread? {
        val reader: Reader
        reader = try {
            InputStreamReader(stream, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            logger.e("Error opening stream")
            return null
        }
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    val line = CharArrayWriter()
                    var ch = -1
                    while (reader.read().also { ch = it } >= 0) {
                        if (ch == '\n'.toInt()) {
                            // New line
                            if (null != consumer) {
                                consumer.eat(line.toString())
                                line.reset()
                            }
                            continue
                        }
                        line.write(ch)
                        if (null != outputStream && line.size() > CONFIRM_YN.length) {
                            if (line.toString().substring(line.size() - CONFIRM_YN.length) ==
                                    CONFIRM_YN) {
                                // Ask for confirmation
                                val question = line.toString().substring(0, line.size()
                                        - CONFIRM_YN
                                        .length).trim { it <= ' ' }
                                listeners().emit(object : ListenerEmitter<TaskListener> {
                                    override fun emit(listener: TaskListener): Boolean {
                                        listener.onQuestion(question, object : DataUtil.Callback<Boolean?> {
                                            override
                                            fun call(value: Boolean?): Boolean {
                                                try {
                                                    outputStream.write(String.format
                                                    ("%s\n", if (value!!) "yes" else
                                                        "no").toByteArray(charset("utf-8")))
                                                } catch (e: IOException) {
                                                    e.printStackTrace()
                                                }
                                                return true
                                            }
                                        })
                                        return true // Only one call
                                    }
                                })
                            }
                        }
                    }
                    if (line.size() > 0) {
                        // Last line
                        consumer?.eat(line.toString())
                    }
                } catch (e: Exception) {
                    logger.e(e, "Error reading stream")
                } finally {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                    }
                }
            }
        }
        thread.start()
        return thread
    }

    private fun initTasksFolder(): File? {
        val folder = File(controller.context().getExternalFilesDir(null), id)
        return if (!folder.exists() || !folder.isDirectory) {
            null
        } else folder
    }

    @Synchronized
    private fun callTask(out: StreamConsumer, err: StreamConsumer, api: Boolean, vararg arguments: String): Int {
        active = true
        taskListeners.emit(object : ListenerEmitter<TaskListener> {
            override fun emit(listener: TaskListener): Boolean {
                listener.onStart()
                return true
            }
        })
        return try {
            if (null == controller.executable) {
                debug("Error in binary call: executable not found")
                throw RuntimeException("Invalid executable")
            }
            if (null == tasksFolder) {
                debug("Error in binary call: invalid profile folder")
                throw RuntimeException("Invalid folder")
            }
            val args: MutableList<String> = ArrayList()
            args.add(controller.executable)
            args.add("rc.color=off")
            if (api) {
                args.add("rc.confirmation=off")
                args.add("rc.verbose=nothing")
            } else {
                args.add("rc.verbose=none")
            }
            Collections.addAll(args, *arguments)
            val pb = ProcessBuilder(args)
            pb.directory(tasksFolder)
            pb.environment()["TASKRC"] = File(tasksFolder, TASKRC).absolutePath
            pb.environment()["TASKDATA"] = File(tasksFolder, DATA_FOLDER).absolutePath
            val p = pb.start()
            logger.d("Calling now:", tasksFolder, args)
            //            debug("Execute:", args);
            val outThread = readStream(p.inputStream, p.outputStream, out)
            val errThread = readStream(p.errorStream, null, err)
            val exitCode = p.waitFor()
            logger.d("Exit code:", exitCode, args)
            if (exitCode != 0) {
                controller.toastMessage("task err code: $exitCode, args: $args", true)
            }
            //            debug("Execute result:", exitCode);
            outThread?.join()
            errThread?.join()
            exitCode
        } catch (e: Exception) {
            logger.e(e, "Failed to execute task")
            err.eat(e.message)
            debug("Execute failure:")
            debug(e)
            255
        } finally {
            taskListeners.emit(object : ListenerEmitter<TaskListener> {
                override fun emit(listener: TaskListener): Boolean {
                    listener.onFinish()
                    return true
                }
            })
            active = false
        }
    }

    private fun callTask(out: StreamConsumer, err: StreamConsumer, vararg arguments: String): Boolean {
        val result = callTask(out, err, true, *arguments)
        return result == 0
    }

    private fun fileFromConfig(path: String?): File? {
        if (TextUtils.isEmpty(path)) { // Invalid path
            return null
        }
        return if (path!!.startsWith("/")) { // Absolute
            File(path)
        } else File(tasksFolder, path)
        // Relative
    }

    private inner class LocalSocketRunner(name: String, config: Map<String, String>) {
        private val port: Int
        private val host: String
        private val factory: SSLSocketFactory
        val socket: LocalServerSocket

        @Throws(IOException::class)
        fun accept() {
            val conn = socket.accept()
            logger.d("New incoming connection")
            LocalSocketThread(conn).start()
        }

        private inner class LocalSocketThread(private val socket: LocalSocket) : Thread() {
            @Throws(IOException::class)
            private fun recvSend(from: InputStream, to: OutputStream): Long {
                val head = ByteArray(4) // Read it first
                from.read(head)
                to.write(head)
                to.flush()
                val size = ByteBuffer.wrap(head, 0, 4).order(ByteOrder.BIG_ENDIAN).int.toLong()
                var bytes: Long = 4
                val buffer = ByteArray(1024)
                logger.d("Will transfer:", size)
                while (bytes < size) {
                    val recv = from.read(buffer)
                    //                logger.d("Actually get:", recv);
                    if (recv == -1) {
                        return bytes
                    }
                    to.write(buffer, 0, recv)
                    to.flush()
                    bytes += recv.toLong()
                }
                logger.d("Transfer done", bytes, size)
                return bytes
            }

            override fun run() {
                var remoteSocket: SSLSocket? = null
                debug("Communication taskw<->android started")
                try {
                    remoteSocket = factory.createSocket(host, port) as SSLSocket
                    val finalRemoteSocket = remoteSocket
                    Compat.levelAware(16, { finalRemoteSocket!!.enabledProtocols = arrayOf("TLSv1", "TLSv1.1", "TLSv1.2") }) { finalRemoteSocket!!.enabledProtocols = arrayOf("TLSv1") }
                    debug("Ready to establish TLS connection to:", host, port)
                    val localInput = socket.inputStream
                    val localOutput = socket.outputStream
                    val remoteInput = remoteSocket!!.inputStream
                    val remoteOutput = remoteSocket.outputStream
                    debug("Connected to taskd server")
                    logger.d("Connected, will read first piece", remoteSocket.session.cipherSuite)
                    val bread = recvSend(localInput, remoteOutput)
                    val bwrite = recvSend(remoteInput, localOutput)
                    logger.d("Sync success")
                    debug("Transfer complete. Bytes sent:", bread, "Bytes received:", bwrite)
                } catch (e: Exception) {
                    logger.e(e, "Failed to transfer data")
                    debug("Transfer failure")
                    debug(e)
                } finally {
                    if (null != remoteSocket) {
                        try {
                            remoteSocket.close()
                        } catch (e: IOException) {
                        }
                    }
                    try {
                        socket.close()
                    } catch (e: IOException) {
                    }
                }
            }

        }

        init {
            val trustType = SSLHelper.parseTrustType(config["taskd.trust"])
            val _host = config["taskd.server"]
            val lastColon = _host!!.lastIndexOf(":")
            port = _host.substring(lastColon + 1).toInt()
            host = _host.substring(0, lastColon)
            debug("Host and port:", host, port)
            if (null != fileLogger) { // Can't just call debug, because of use of fileLogger
                debug("CA file:",
                        fileLogger!!.logFile(fileFromConfig(config["taskd.ca"])))
                debug("Certificate file:",
                        fileLogger!!.logFile(fileFromConfig(config["taskd.certificate"])))
                debug("Key file:",
                        fileLogger!!.logFile(fileFromConfig(config["taskd.key"])))
            }
            factory = SSLHelper.tlsSocket(
                    controller.openFile(fileFromConfig(config["taskd.ca"])),
                    controller.openFile(fileFromConfig(config["taskd.certificate"])),
                    controller.openFile(fileFromConfig(config["taskd.key"])),
                    trustType)
            debug("Credentials loaded")
            logger.d("Connecting to:", host, port)
            socket = LocalServerSocket(name)
        }
    }

    private fun openLocalSocket(name: String): LocalServerSocket? {
        try {
            val config = taskSettings("taskd.ca", "taskd.certificate", "taskd.key", "taskd.server", "taskd.trust")
            logger.d("Will run with config:", config)
            debug("taskd.* config:", config)
            if (!config.containsKey("taskd.server")) {
                // Not configured
                logger.d("Sync not configured - give up")
                controller.toastMessage("Sync disabled: no taskd.server value", true)
                debug("taskd.server is empty: sync disabled")
                return null
            }
            val runner: LocalSocketRunner
            runner = try {
                LocalSocketRunner(name, config)
            } catch (e: Exception) {
                logger.e(e, "Error opening socket")
                debug(e)
                controller.toastMessage("Sync disabled: certificate load failure", true)
                return null
            }
            acceptThread = object : Thread() {
                override fun run() {
                    while (true) {
                        try {
//                            debug("Incoming connection: task binary -> android");
                            runner.accept()
                        } catch (e: IOException) {
                            debug("Socket accept failed")
                            debug(e)
                            logger.w(e, "Accept failed")
                            return
                        }
                    }
                }
            }
            acceptThread!!.start()
//            controller.toastMessage("Sync configured", false)
            return runner.socket // Close me later on stop
        } catch (e: Exception) {
            logger.e(e, "Failed to open local socket")
        }
        return null
    }

    fun taskList(query: String?): List<JSONObject> {
        var query = query
        query = if (TextUtils.isEmpty(query)) {
            "status:pending"
        } else {
            String.format("(%s)", query)
        }
        val context = taskSetting("context")
        logger.d("taskList context:", context)
        if (context != null)
            debug("List query:", query, "context:", context)
        if (!TextUtils.isEmpty(context)) { // Have context configured
            val cQuery = taskSetting(String.format("context.%s", context))
            if (!TextUtils.isEmpty(cQuery)) { // Prepend context
                debug("Context query:", cQuery!!)
                query = String.format("(%s) %s", cQuery, query)
            }
            logger.d("Context query:", cQuery, query)
        }
        val result: MutableList<JSONObject> = ArrayList()
        val params: MutableList<String> = ArrayList()
        params.add("rc.json.array=off")
        params.add("export")
        params.add(escape(query))
        callTask(object : StreamConsumer {
            override fun eat(line: String?) {
                if (!TextUtils.isEmpty(line)) {
                    try {
                        result.add(JSONObject(line))
                    } catch (e: Exception) {
                        logger.e(e, "Not JSON object:", line)
                    }
                }
            }
        }, errConsumer, *params.toTypedArray())
        logger.d("List for:", query, result.size, context)
        return result
    }

    val projects: List<String?>
        get() {
            val result: MutableList<String?> = ArrayList()
            val params: MutableList<String> = ArrayList()
            params.add("_projects")
            callTask(object : StreamConsumer {
                override fun eat(line: String?) {
                    if (!TextUtils.isEmpty(line)) {
                        try {
                            result.add(line)
                        } catch (e: Exception) {
                            logger.e(e, "Not String object:", line)
                        }
                    }
                }
            }, errConsumer, *params.toTypedArray())
            logger.d("Project list: ", result)
            return result
        }

    val tags: List<String>
        get() = getTags(false)

    fun getTags(include_buildin_tags: Boolean): List<String> {
        val virtualTags = setOf("ACTIVE", "ANNOTATED", "BLOCKED", "BLOCKING",
                "CHILD", "COMPLETED", "DELETED", "DUE", "DUETODAY", "MONTH", "ORPHAN", "OVERDUE",
                "PARENT", "PENDING", "READY", "SCHEDULED", "TAGGED", "TODAY", "TOMORROW", "UDA",
                "UNBLOCKED", "UNTIL", "WAITING", "WEEK", "YEAR", "YESTERDAY"
        )
        val specialTags = setOf("next", "nocal", "nocolor", "nonag")
        val result: MutableList<String> = ArrayList()
        val params: MutableList<String> = ArrayList()
        params.add("_tags")
        callTask(object : StreamConsumer {
            override fun eat(line: String?) {
                line?.let {
                    if (include_buildin_tags || it.toUpperCase(Locale.ROOT) !in virtualTags &&
                            it.toLowerCase(Locale.ROOT) !in specialTags) {
                        result.add(it)
                    }
                }
            }
        }, errConsumer, *params.toTypedArray())
        logger.d("Tag list: ", result)
        return result
    }

    fun intentForRunTask(): Intent {
        val intent = Intent(controller.context(), RunActivity::class.java)
        intent.putExtra(App.KEY_ACCOUNT, id)
        return intent
    }

    fun intentForEditor(intent: Intent, uuid: String?): Boolean {
        intent.putExtra(App.KEY_ACCOUNT, id)
        val priorities = taskPriority()
        if (TextUtils.isEmpty(uuid)) { // Done - new item
            intent.putExtra(App.KEY_EDIT_PRIORITY, priorities.indexOf(""))
            return true
        }
        val jsons = taskList(uuid)
        if (jsons.isEmpty()) { // Failed
            return false
        }
        val json = jsons[0]
        var priorityIndex = priorities.indexOf(json.optString("priority", ""))
        if (-1 == priorityIndex) {
            priorityIndex = priorities.indexOf("")
        }
        intent.putExtra(App.KEY_EDIT_PRIORITY, priorityIndex)
        intent.putExtra(App.KEY_EDIT_UUID, json.optString("uuid"))
        intent.putExtra(App.KEY_EDIT_DESCRIPTION, json.optString("description"))
        intent.putExtra(App.KEY_EDIT_PROJECT, json.optString("project"))
        val tags = json.optJSONArray("tags")
        if (null != tags) {
            intent.putExtra(App.KEY_EDIT_TAGS, Helpers.join(" ",
                    Helpers.array2List(tags)))
        }
        val formattedISO: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(controller.context())
        intent.putExtra(App.KEY_EDIT_DUE, Helpers.asDate(json.optString("due"),
                formattedISO, sharedPref))
        intent.putExtra(App.KEY_EDIT_WAIT, Helpers.asDate(json.optString("wait"),
                formattedISO, sharedPref))
        intent.putExtra(App.KEY_EDIT_SCHEDULED, Helpers.asDate(json.optString(
                "scheduled"),
                formattedISO, sharedPref))
        intent.putExtra(App.KEY_EDIT_UNTIL, Helpers.asDate(json.optString("until"),
                formattedISO, sharedPref))
        intent.putExtra(App.KEY_EDIT_RECUR, json.optString("recur"))
        return true
    }

    fun taskDone(uuid: String?): String? {
        val err = StringAggregator()
        if (!callTask(outConsumer, err, uuid!!, "done")) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskDelete(uuid: String?): String? {
        val err = StringAggregator()
        if (!callTask(outConsumer, err, uuid!!, "delete")) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskStart(uuid: String?): String? {
        val err = StringAggregator()
        if (!callTask(outConsumer, err, uuid!!, "start")) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskStop(uuid: String?): String? {
        val err = StringAggregator()
        if (!callTask(outConsumer, err, uuid!!, "stop")) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskLog(changes: List<String>): String? {
        val params: MutableList<String> = ArrayList()
        params.add("log")
        for (change in changes) { // Copy non-empty
            if (!TextUtils.isEmpty(change)) {
                params.add(change)
            }
        }
        val err = StringAggregator()
        if (!callTask(outConsumer, err, *params.toTypedArray())) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskAdd(changes: List<String>): String? {
        val params: MutableList<String> = ArrayList()
        params.add("add")
        for (change in changes) { // Copy non-empty
            if (!TextUtils.isEmpty(change)) {
                params.add(change)
            }
        }
        val err = StringAggregator()
        if (!callTask(outConsumer, err, *params.toTypedArray())) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun taskModify(uuid: String, changes: List<String>): String? {
        val params: MutableList<String> = ArrayList()
        params.add(uuid)
        params.add("modify")
        for (change in changes) { // Copy non-empty
            if (!TextUtils.isEmpty(change)) {
                params.add(change)
            }
        }
        val err = StringAggregator()
        if (!callTask(outConsumer, err, *params.toTypedArray())) { // Failure
            return err.text()
        }
        scheduleSync(TimerType.AfterChange)
        return null // Success
    }

    fun listeners(): Listeners<TaskListener> {
        return taskListeners
    }

    fun syncIntent(type: String?): PendingIntent {
        val intent = Intent(controller.context(), SyncIntentReceiver::class.java)
        intent.putExtra(App.KEY_ACCOUNT, id)
        intent.data = Uri.fromParts("tw", type, id)
        return PendingIntent.getBroadcast(controller.context(), App.SYNC_REQUEST, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    companion object {
        private const val CONFIRM_YN = " (yes/no) "
        private val defaultFields = arrayOf("description", "urgency", "priority", "due", "wait", "scheduled", "recur", "until", "project", "tags")
        const val TASKRC = ".taskrc.android"
        const val DATA_FOLDER = "data"
        fun split2(src: String?, sep: String): List<String> {
            val result: MutableList<String> = ArrayList()
            var start = 0
            while (true) {
                val index = src!!.indexOf(sep, start)
                if (index == -1) {
                    result.add(src.substring(start).trim { it <= ' ' })
                    return result
                }
                result.add(src.substring(start, index).trim { it <= ' ' })
                start = index + sep.length
            }
        }

        @JvmStatic
        fun escape(query: String): String {
            return query.replace(" ", "\\ ") //.replace("(", "\\(").replace(")", "\\)");
        }
    }

    init {
        tasksFolder = initTasksFolder()
        socketName = UUID.randomUUID().toString().toLowerCase()
        initLogger()
        syncSocket = openLocalSocket(socketName)
        scheduleSync(TimerType.Periodical) // Schedule on start
        loadNotificationTypes()
    }
}