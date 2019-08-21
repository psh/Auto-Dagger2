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
        val annotationSpecBuilder = AnnotationSpec.builder(Subcomponent::class.java)

        for (typeName in modulesTypeNames!!) {
            annotationSpecBuilder.addMember("modules", "\$T.class", typeName)
        }

        val builder = TypeSpec.interfaceBuilder(className.simpleName())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(Generated::class.java)
                    .addMember("value", "\$S", AutoDaggerAnnotationProcessor::class.java.name)
                    .build()
            )
            .addAnnotation(annotationSpecBuilder.build())

        for (typeName in superinterfacesTypeNames!!) {
            builder.addSuperinterface(typeName)
        }

        if (scopeAnnotationSpec != null) {
            builder.addAnnotation(scopeAnnotationSpec!!)
        }

        for (additionSpec in injectorSpecs!!) {
            builder.addMethod(
                MethodSpec.methodBuilder("inject")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(additionSpec.typeName, additionSpec.name)
                    .build()
            )
        }

        for (additionSpec in exposeSpecs!!) {
            val exposeBuilder = MethodSpec.methodBuilder(additionSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(additionSpec.typeName)
            if (additionSpec.qualifierAnnotationSpec != null) {
                exposeBuilder.addAnnotation(additionSpec.qualifierAnnotationSpec)
            }
            builder.addMethod(exposeBuilder.build())
        }

        if (subcomponentsSpecs!!.isNotEmpty()) {
            builder.addMethods(subcomponentsSpecs!!)
        }

        try {
            JavaFile.builder(className.packageName(), builder.build())
                .build()
                .writeTo(filer)
        } catch (e: Exception) {
        }
    }
}