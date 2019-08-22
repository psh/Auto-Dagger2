package autodagger.compiler.component

import autodagger.compiler.AutoDaggerAnnotationProcessor
import autodagger.compiler.addition.AdditionSpec
import com.squareup.javapoet.*
import dagger.Component
import javax.annotation.Generated
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

data class ComponentSpec(
    val className: ClassName,
    var targetTypeName: TypeName? = null,
    var scopeAnnotationSpec: AnnotationSpec? = null,
    var injectorSpecs: List<AdditionSpec>? = null,
    var exposeSpecs: List<AdditionSpec>? = null,
    var dependenciesTypeNames: List<TypeName>? = null,
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
                AnnotationSpec.builder(Component::class.java).apply {
                    dependenciesTypeNames?.forEach {
                        addMember("dependencies", "\$T.class", it)
                    }

                    modulesTypeNames?.forEach {
                        addMember("modules", "\$T.class", it)
                    }
                }.build()
            ).apply {
                superinterfacesTypeNames?.forEach { addSuperinterface(it) }

                scopeAnnotationSpec?.let { addAnnotation(it) }

                injectorSpecs?.forEach { additionSpec ->
                    addMethod(
                        MethodSpec.methodBuilder("inject")
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameter(additionSpec.typeName, additionSpec.name)
                            .build()
                    )
                }

                exposeSpecs?.forEach { additionSpec ->
                    val exposeBuilder = MethodSpec.methodBuilder(additionSpec.name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(additionSpec.typeName)
                    additionSpec.qualifierAnnotationSpec?.let {
                        exposeBuilder.addAnnotation(it)
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
