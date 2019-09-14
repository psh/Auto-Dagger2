package autodagger.compiler

import autodagger.AutoInjector
import autodagger.compiler.addition.AdditionExtractor
import autodagger.compiler.binds.BindsExtractor
import autodagger.compiler.component.ComponentExtractor
import autodagger.compiler.subcomponent.SubcomponentExtractor
import autodagger.compiler.utils.notAnnotatedWith
import com.google.auto.common.MoreElements.asExecutable
import com.google.auto.common.MoreElements.asType
import com.google.auto.common.MoreTypes.asElement
import dagger.Provides
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind.*
import javax.lang.model.element.TypeElement

typealias Visitor = State.(processedAnnotation: Class<out Annotation>, element: Element) -> Unit

val BINDS_VISITOR: Visitor = { a, e ->
    if (e.kind == CLASS) {
        (e as TypeElement).interfaces?.forEach {
            val extractor = BindsExtractor(a, e, it, this)
            if (extractor.targetTypeMirrors.isNotEmpty()) {
                addBindingExtractor(extractor)
            } else {
                errors.addInvalid(e, "@AutoBinds needs a target component")
            }
        }
    }
}

val INJECT_VISITOR: Visitor = { a, e ->
    when (e.kind) {
        ANNOTATION_TYPE -> handleAnnotatedAnnotation(e, a) { addInjectorExtractor(it) }
        METHOD -> handleMethod(e, a) { addInjectorExtractor(it) }
        else -> handleOtherAddition(e, a) { addInjectorExtractor(it) }
    }
}

val EXPOSE_VISITOR: Visitor = { a, e ->
    when (e.kind) {
        ANNOTATION_TYPE -> handleAnnotatedAnnotation(e, a) { addExposeExtractor(it) }
        METHOD -> handleMethod(e, a) { addExposeExtractor(it) }
        else -> handleOtherAddition(e, a) { addExposeExtractor(it) }
    }
}

val COMPONENT_VISITOR: Visitor = { _, e ->
    when (e.kind) {
        // @AutoComponent is applied on another annotation, find out the targets of that annotation
        ANNOTATION_TYPE ->
            roundEnvironment.getElementsAnnotatedWith(asType(e)).forEach {
                addComponentExtractor(ComponentExtractor(it, e, this))
            }
        else -> addComponentExtractor(ComponentExtractor(e, e, this))
    }
}

val SUBCOMPONENT_VISITOR: Visitor = { _, e ->
    addSubcomponentExtractor(SubcomponentExtractor(e, this))
}

private fun State.handleMethod(
    e: Element,
    a: Class<out Annotation>,
    store: (AdditionExtractor) -> Unit
) {
    if (a == AutoInjector::class.java) {
        errors.addInvalid(
            e,
            "@AutoInjector cannot be applied on the method %s",
            e.simpleName.toString()
        )
    } else {
        if (e notAnnotatedWith Provides::class.java) {
            errors.addInvalid(
                e,
                "@AutoExpose can be applied on @Provides method only, %s is missing it",
                e.simpleName.toString()
            )
        } else {
            val additionElement = asElement(asExecutable(e).returnType)
            try {
                store(AdditionExtractor(a, asType(additionElement), e, this))
            } catch (_: Exception) {
                errors.addInvalid(
                    additionElement,
                    "%s must be a class",
                    additionElement.simpleName.toString()
                )
            }
        }
    }
}

private fun State.handleOtherAddition(
    e: Element,
    a: Class<out Annotation>,
    store: (AdditionExtractor) -> Unit
) {
    try {
        store(AdditionExtractor(a, asType(e), e, this))
    } catch (_: Exception) {
        errors.addInvalid(
            e,
            "%s must be a class",
            e.simpleName.toString()
        )
    }
}

private fun State.handleAnnotatedAnnotation(
    e: Element,
    a: Class<out Annotation>,
    store: (AdditionExtractor) -> Unit
) {
    roundEnvironment.getElementsAnnotatedWith(asType(e)).forEach {
        store(AdditionExtractor(a, asType(it), e, this))
    }
}

