package autodagger.compiler.processorworkflow

import com.squareup.javapoet.JavaFile

abstract class AbstractComposer<T_Model>(private val specs: List<T_Model>) {
    fun compose(): List<JavaFile> = specs.map { compose(it) }
    protected abstract fun compose(spec: T_Model): JavaFile
}
