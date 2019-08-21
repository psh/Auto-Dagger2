package autodagger.compiler.processorworkflow

import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

class Errors {
    val list = mutableListOf<Error>()

    fun addInvalid(element: Element, reason: String, vararg format: String?) {
        list.add(
            Error(
                element,
                String.format("Invalid value: %s", String.format(reason, *format))
            )
        )
    }

    fun hasErrors(): Boolean = list.isNotEmpty()

    class Error(val element: Element, val text: String)

    class ElementErrors(val parent: Errors, private val element: Element) {
        fun addInvalid(reason: String, vararg format: String?) {
            parent.addInvalid(element, reason, *format)
        }
    }
}

fun Messager.deliver(errors: Errors) = errors.list.forEach {
    printMessage(Diagnostic.Kind.ERROR, it.text, it.element)
}

