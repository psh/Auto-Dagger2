package autodagger.compiler.subcomponent

import autodagger.compiler.Processor

val subcomponentAnnotations: Processor = { a, e ->
    val extractor = SubcomponentExtractor(e, this)
    log(a.simpleName.toString(), "$e")
    addSubcomponentExtractor(extractor)

    if (extractor.modulesTypeMirrors.isNotEmpty()) {
        log("${a.simpleName}::Modules", "${extractor.element.asType()}: ${extractor.modulesTypeMirrors}")
        addSubcomponentModule(extractor.element.asType(), extractor.modulesTypeMirrors)
    }
}