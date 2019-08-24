package autodagger.compiler.component

import autodagger.compiler.Processor
import com.google.auto.common.MoreElements.asType
import javax.lang.model.element.ElementKind

val componentAnnotations: Processor = { a, e ->
    if (e.kind == ElementKind.ANNOTATION_TYPE) {
        log(a.simpleName.toString() + "::Annotation", "Found on $e")
        // @AutoComponent is applied on another annotation, find out the targets of that annotation
        roundEnvironment.getElementsAnnotatedWith(asType(e)).forEach {
            log(a.simpleName.toString() + "::Annotation", "   Looking into $it")
            addComponentExtractor(ComponentExtractor(it, e, this))
        }
    } else {
        log(a.simpleName.toString(), "Found $e")
        addComponentExtractor(ComponentExtractor(e, e, this))
    }
}