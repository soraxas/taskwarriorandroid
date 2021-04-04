package soraxas.taskw


import android.app.Application
import soraxas.taskw.data.Controller

class App : Application() {


    override fun onCreate() {
        instance = this
        super.onCreate()
        controller = create()
        init()
        controller.init()
    }

    fun create(): Controller {
        return Controller(this, "Taskwarrior")
    }

    fun init() {
        val controller: Controller = controller()
        for (acc in controller.accounts()) {
            controller.accountController(controller.accountID(acc)) // This will schedule sync
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: App

        @JvmStatic
        lateinit var controller: Controller

        @JvmStatic
        fun controller(): Controller {
            return controller
        }

        const val ACCOUNT_TYPE = "kvj.task.account"
        const val KEY_ACCOUNT = "account"
        const val KEY_REPORT = "report"
        const val KEY_QUERY = "query"
        const val KEY_EDIT_UUID = "editor_uuid"
        const val KEY_EDIT_DESCRIPTION = "editor_description"
        const val KEY_EDIT_PROJECT = "editor_project"
        const val KEY_EDIT_TAGS = "editor_tags"
        const val KEY_EDIT_DUE = "editor_due"
        const val KEY_EDIT_WAIT = "editor_wait"
        const val KEY_EDIT_SCHEDULED = "editor_scheduled"
        const val KEY_EDIT_RECUR = "editor_recur"
        const val KEY_EDIT_UNTIL = "editor_until"
        const val KEY_EDIT_PRIORITY = "editor_priority"
        const val EDIT_REQUEST = 1
        const val SYNC_REQUEST = 2
        const val KEY_EDIT_TEXT = "editor_text"
        const val ANNOTATE_REQUEST = 3
        const val SETTINGS_REQUEST = 4
        const val KEY_EDIT_STATUS = "editor_status"
        const val ACCOUNT_FOLDER = "folder"
        const val KEY_TEXT_INPUT = "text_editor_input"
        const val KEY_TEXT_TARGET = "text_editor_target"
        const val KEY_QUERY_INPUT = "query_input"
        const val KEY_RUN_COMMAND = "run_command"
        const val KEY_RUN_OUTPUT = "run_output"
        const val KEY_EDIT_DATA = "editor_data"
        const val KEY_EDIT_DATA_FIELDS = "editor_data_fields"

        @JvmField
        val BUILTIN_REPORTS = arrayOf(
                "burndown.daily",
                "burndown.monthly",
                "burndown.weekly",
                "calendar",
                "colors",
                "export",
                "ghistory.annual",
                "ghistory.monthly",
                "history.annual",
                "history.monthly",
                "information",
                "summary",
                "timesheet",
                "projects")
        const val LOG_FILE = "taskw.log.txt"
    }
}