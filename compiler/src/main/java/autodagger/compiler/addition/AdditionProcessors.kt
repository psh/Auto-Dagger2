package autodagger.compiler.addition

import autodagger.AutoInjector
import autodagger.compiler.Processor
import autodagger.compiler.utils.isNotPresentOn
import com.google.auto.common.MoreElements.asExecutable
import com.google.auto.common.MoreElements.asType
import com.google.auto.common.MoreTypes.asElement
import dagger.Provides
import javax.lang.model.element.ElementKind

val annotatedAdditionAnnotation: Processor = { a, e ->
    if (e.kind == ElementKind.ANNOTATION_TYPE) {
        // @AutoX is applied on another annotation, find out the targets of that annotation
        roundEnvironment.getElementsAnnotatedWith(asType(e)).forEach {
            val extractor = AdditionExtractor(a, asType(it), e, this)
            if (extractor.targetTypeMirrors.isNotEmpty()) {
                log("${a.simpleName}::Annotation", "$e")
                if (a == AutoInjector::class.java) {
                    addInjectorExtractor(extractor)
                } else {
                    addExposeExtractor(extractor)
                }
            }
        }
    }
}

val annotatedAdditionMethod: Processor = { a, e ->
    if (e.kind == ElementKind.METHOD) {
        if (a == AutoInjector::class.java) {
            errors.addInvalid(
                e,
                "@AutoInjector cannot be applied on the method %s",
                e.simpleName.toString()
            )
        } else {
            if (Provides::class.java.isNotPresentOn(e)) {
                errors.addInvalid(
                    e,
                    "@AutoExpose can be applied on @Provides method only, %s is missing it",
                    e.simpleName.toString()
                )
            } else {
                val additionElement = asElement(asExecutable(e).returnType)
                try {
                    val extractor = AdditionExtractor(a, asType(additionElement), e, this)
                    if (extractor.targetTypeMirrors.isNotEmpty()) {
                        log("${a.simpleName}::Method", "$e")
                        if (a == AutoInjector::class.java) {
                            addInjectorExtractor(extractor)
                        } else {
                            addExposeExtractor(extractor)
                        }
                    }
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
}

val addition: Processor = { a, e ->
    if (e.kind != ElementKind.ANNOTATION_TYPE && e.kind != ElementKind.METHOD) {
        try {
            val extractor = AdditionExtractor(a, asType(e), e, this)
            if (extractor.targetTypeMirrors.isNotEmpty()) {
                log(a.simpleName.toString(), "$e")
                if (a == AutoInjector::class.java) {
                    addInjectorExtractor(extractor)
                } else {
                    addExposeExtractor(extractor)
                }
            }
        } catch (_: Exception) {
            errors.addInvalid(
                e,
                "%s must be a class",
                e.simpleName.toString()
            )
        }
    }
}