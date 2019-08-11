package autodagger.compiler.component

import autodagger.compiler.AnnotationProcessor
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import dagger.Component
import processorworkflow.AbstractComposer
import javax.annotation.Generated
import javax.lang.model.element.Modifier

class ComponentComposer(specs: List<ComponentSpec>) : AbstractComposer<ComponentSpec>(specs) {

    override fun compose(spec: ComponentSpec): JavaFile {
        val annotationSpecBuilder = AnnotationSpec.builder(Component::class.java)

        for (typeName in spec.dependenciesTypeNames!!) {
            annotationSpecBuilder.addMember("dependencies", "\$T.class", typeName)
        }

        for (typeName in spec.modulesTypeNames!!) {
            annotationSpecBuilder.addMember("modules", "\$T.class", typeName)
        }

        val builder = TypeSpec.interfaceBuilder(spec.className.simpleName())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(
                AnnotationSpec.builder(Generated::class.java)
                    .addMember("value", "\$S", AnnotationProcessor::class.java.name)
                    .build()
            )
            .addAnnotation(annotationSpecBuilder.build())

        for (typeName in spec.superinterfacesTypeNames!!) {
            builder.addSuperinterface(typeName)
        }

        if (spec.scopeAnnotationSpec != null) {
            builder.addAnnotation(spec.scopeAnnotationSpec!!)
        }

        for (additionSpec in spec.injectorSpecs!!) {
            builder.addMethod(
                MethodSpec.methodBuilder("inject")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(additionSpec.typeName, additionSpec.name)
                    .build()
            )
        }

        for (additionSpec in spec.exposeSpecs!!) {
            val exposeBuilder = MethodSpec.methodBuilder(additionSpec.name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(additionSpec.typeName)
            if (additionSpec.qualifierAnnotationSpec != null) {
                exposeBuilder.addAnnotation(additionSpec.qualifierAnnotationSpec)
            }
            builder.addMethod(exposeBuilder.build())
        }

        if (spec.subcomponentsSpecs!!.isNotEmpty()) {
            builder.addMethods(spec.subcomponentsSpecs!!)
        }

        val typeSpec = builder.build()
        return JavaFile.builder(spec.className.packageName(), typeSpec).build()
    }
}
