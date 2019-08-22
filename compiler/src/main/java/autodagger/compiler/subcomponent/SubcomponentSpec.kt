package autodagger.compiler.subcomponent

import autodagger.compiler.AutoDaggerAnnotationProcessor
import autodagger.compiler.addition.AdditionSpec
import com.squareup.javapoet.*
import dagger.Subcomponent
import javax.annotation.Generated
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

data class SubcomponentSpec(
    val className: ClassName,
    var scopeAnnotationSpec: AnnotationSpec? = null,
    var injectorSpecs: List<AdditionSpec>? = null,
    var exposeSpecs: List<AdditionSpec>? = null,
    var modulesTypeNames: List<TypeName>? = null,
    var superinterfacesTypeNames: List<TypeName>? = null,
    var subcomponentsSpecs: List<MethodSpec>? = null
) {
    fun writeTo(filer: Filer) {
        val builder = TypeSpec.interfaceBuilder(className.simpleName())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(Generated::class.java)
                    .addMember("value", "\$S", AutoDaggerAnnotationProcessor::class.java.name)
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(Subcomponent::class.java).apply {
                    modulesTypeNames?.forEach { typeName ->
                        addMember("modules", "\$T.class", typeName)
                    }
                }.build()
            )
            .apply {
                superinterfacesTypeNames?.forEach { addSuperinterface(it) }

                scopeAnnotationSpec?.let { addAnnotation(it) }

                injectorSpecs?.forEach {
                    addMethod(
                        MethodSpec.methodBuilder("inject")
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameter(it.typeName, it.name)
                            .build()
                    )
                }

                exposeSpecs?.forEach {
                    val exposeBuilder = MethodSpec.methodBuilder(it.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(it.typeName)
                    if (it.qualifierAnnotationSpec != null) {
                        exposeBuilder.addAnnotation(it.qualifierAnnotationSpec)
                    }
                    addMethod(exposeBuilder.build())
                }

                if (subcomponentsSpecs!!.isNotEmpty()) addMethods(subcomponentsSpecs!!)
            }

        try {
            JavaFile.builder(className.packageName(), builder.build())
                .build()
                .writeTo(filer)
        } catch (e: Exception) {
        }
    }
}