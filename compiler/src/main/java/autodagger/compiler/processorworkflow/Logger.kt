package autodagger.compiler.processorworkflow

object Logger {
    private var TAG: String? = null
    private var enabled: Boolean = false

    fun init(tag: String, enabled: Boolean) {
        TAG = tag
        Logger.enabled = enabled
    }

    fun d(message: String, vararg format: Any) {
        if (TAG == null) {
            throw IllegalStateException("Must call Logger.init() before using logger")
        }

        if (!enabled) {
            return
        }

        println("$TAG - ${String.format(message, *format)}")
    }
}
