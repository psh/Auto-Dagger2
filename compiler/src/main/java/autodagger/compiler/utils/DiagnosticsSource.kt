package autodagger.compiler.utils

import javax.lang.model.element.Element

interface DiagnosticsSource {
    val element: Element

    fun toDiagnostics(): MutableMap<String, String?>
}