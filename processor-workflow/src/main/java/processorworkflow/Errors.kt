package processorworkflow

import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

class Errors {
    private val list = mutableListOf<Error>()

    fun addInvalid(element: Element, reason: String, vararg format: String?) {
        list.add(Error(element, String.format("Invalid value: %s", String.format(reason, *format))))
    }

    fun addMissing(element: Element, reason: String, vararg format: String?) {
        list.add(Error(element, String.format("Missing value: %s", String.format(reason, *format))))
    }

    fun deliver(messager: Messager) = list.forEach {
        messager.printMessage(Diagnostic.Kind.ERROR, it.text, it.element)
    }

    fun getFor(element: Element): ElementErrors = ElementErrors(this, element)

    fun hasErrors(): Boolean = list.isNotEmpty()

    internal class Error(val element: Element, val text: String)

    class ElementErrors(val parent: Errors, private val element: Element) {
        fun addInvalid(reason: String, vararg format: String?) {
            parent.addInvalid(element, reason, *format)
        }

        fun addMissing(reason: String, vararg format: String?) {
            parent.addMissing(element, reason, *format)
        }
    }
}
