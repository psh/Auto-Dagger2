package autodagger.compiler;

import autodagger.AutoComponent;
import autodagger.AutoSubcomponent;
import autodagger.compiler.utils.AutoComponentClassNameUtil;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.*;
import processorworkflow.AbstractComposer;
import processorworkflow.AbstractProcessing;
import processorworkflow.Errors;
import processorworkflow.ProcessingBuilder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author Lukasz Piliszczuk - lukasz.pili@gmail.com
 */
public class ComponentProcessing extends AbstractProcessing<ComponentSpec, State> {

    /**
     * Build all extractors first, then build all builders, because
     * we want to gather all targets first
     */
    private final Set<ComponentExtractor> extractors;

    public ComponentProcessing(Elements elements, Types types, Errors errors, State state) {
        super(elements, types, errors, state);
        extractors = new HashSet<>();
    }

    @Override
    public Set<Class<? extends Annotation>> supportedAnnotations() {
        Set set = ImmutableSet.of(AutoComponent.class);
        return set;
    }

    @Override
    protected void processElements(Set<? extends Element> annotationElements) {
        super.processElements(annotationElements);
        if (errors.hasErrors()) {
            return;
        }

        processExtractors();
    }

    @Override
    public boolean processElement(Element element, Errors.ElementErrors elementErrors) {
        if (ElementKind.ANNOTATION_TYPE.equals(element.getKind())) {
            // @AutoComponent is applied on another annotation, find out the targets of that annotation
            Set<? extends Element> targetElements = roundEnvironment.getElementsAnnotatedWith(MoreElements.asType(element));
            for (Element targetElement : targetElements) {
                process(targetElement, element);
                if (errors.hasErrors()) {
                    return false;
                }
            }
            return true;
        }

        process(element, element);

        if (errors.hasErrors()) {
            return false;
        }

        return true;
    }

    private void process(Element targetElement, Element element) {
        ComponentExtractor extractor = new ComponentExtractor(targetElement, element, types, elements, errors);
        if (errors.hasErrors()) {
            return;
        }

        extractors.add(extractor);
    }

    private void processExtractors() {
        for (ComponentExtractor extractor : extractors) {
            ComponentSpec spec = new Builder(extractor, errors).build();
            if (errors.hasErrors()) {
                return;
            }

            specs.add(spec);
        }
    }


    @Override
    public AbstractComposer<ComponentSpec> createComposer() {
        return new ComponentComposer(specs);
    }

    private class Builder extends ProcessingBuilder<ComponentExtractor, ComponentSpec> {

        public Builder(ComponentExtractor extractor, Errors errors) {
            super(extractor, errors);
        }

        @Override
        protected ComponentSpec build() {
            ComponentSpec componentSpec = new ComponentSpec(AutoComponentClassNameUtil.getComponentClassName(extractor.getComponentElement()));
            componentSpec.setTargetTypeName(TypeName.get(extractor.getTargetTypeMirror()));

            if (extractor.getScopeAnnotationTypeMirror() != null) {
                componentSpec.setScopeAnnotationSpec(AnnotationSpec.get(extractor.getScopeAnnotationTypeMirror()));
            }

            // injectors
            componentSpec.setInjectorSpecs(ProcessingUtil.getAdditions(extractor.getTargetTypeMirror(), state.getInjectorExtractors()));

            // exposed
            componentSpec.setExposeSpecs(ProcessingUtil.getAdditions(extractor.getTargetTypeMirror(), state.getExposeExtractors()));

            // dependencies
            componentSpec.setDependenciesTypeNames(getDependencies());

            // superinterfaces
            componentSpec.setSuperinterfacesTypeNames(getSuperinterfaces(extractor.getSuperinterfacesTypeMirrors()));

            // modules
            componentSpec.setModulesTypeNames(ProcessingUtil.getTypeNames(extractor.getModulesTypeMirrors()));

            // subcomponents
            componentSpec.setSubcomponentsSpecs(getSubcomponents());

            return componentSpec;
        }

        private List<MethodSpec> getSubcomponents() {
            if (extractor.getSubcomponentsTypeMirrors().isEmpty()) {
                return Collections.emptyList();
            }

            List<MethodSpec> methodSpecs = new ArrayList<>(extractor.getSubcomponentsTypeMirrors().size());
            for (TypeMirror typeMirror : extractor.getSubcomponentsTypeMirrors()) {
                Element e = MoreTypes.asElement(typeMirror);
                TypeName typeName;
                String name;
                if (MoreElements.isAnnotationPresent(e, AutoSubcomponent.class)) {
                    ClassName cls = AutoComponentClassNameUtil.getComponentClassName(e);
                    typeName = cls;
                    name = cls.simpleName();
                } else {
                    typeName = TypeName.get(typeMirror);
                    name = e.getSimpleName().toString();
                }

                List<TypeMirror> modules = state.getSubcomponentModules(typeMirror);
                List<ParameterSpec> parameterSpecs;
                if(modules != null) {
                    parameterSpecs = new ArrayList<>(modules.size());
                    int count = 0;
                    for (TypeMirror moduleTypeMirror : modules) {
                        parameterSpecs.add(ParameterSpec.builder(TypeName.get(moduleTypeMirror), String.format("module%d", ++count)).build());
                    }
                } else {
                    parameterSpecs = new ArrayList<>(0);
                }

                methodSpecs.add(MethodSpec.methodBuilder("plus" + name)
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameters(parameterSpecs)
                        .returns(typeName)
                        .build());
            }

            return methodSpecs;
        }

        private List<TypeName> getDependencies() {
            List<TypeName> typeNames = new ArrayList<>();
            if (extractor.getDependenciesTypeMirrors() == null) {
                return typeNames;
            }

            mainLoop:
            for (TypeMirror typeMirror : extractor.getDependenciesTypeMirrors()) {
                // check if dependency type mirror references an @AutoComponent target
                // if so, build the TypeName that matches the target component
                for (ComponentExtractor componentExtractor : extractors) {
                    if (componentExtractor == extractor) {
                        // ignore self
                        continue;
                    }

                    if (ProcessingUtil.areTypesEqual(componentExtractor.getTargetTypeMirror(), typeMirror)) {
                        typeNames.add(AutoComponentClassNameUtil.getComponentClassName(componentExtractor.getComponentElement()));
                        continue mainLoop;
                    }
                }

                typeNames.add(TypeName.get(typeMirror));
            }

            return typeNames;
        }

        private List<TypeName> getSuperinterfaces(List<TypeMirror> superinterfaces) {
            List<TypeName> typeNames = new ArrayList<>();
            if (superinterfaces == null) {
                return typeNames;
            }

            mainLoop:
            for (TypeMirror typeMirror : superinterfaces) {
                // check if dependency type mirror references an @AutoComponent target
                // if so, build the TypeName that matches the target component
                for (ComponentExtractor componentExtractor : extractors) {
                    if (componentExtractor == extractor) {
                        // ignore self
                        continue;
                    }

                    if (ProcessingUtil.areTypesEqual(componentExtractor.getTargetTypeMirror(), typeMirror)) {
                        typeNames.add(AutoComponentClassNameUtil.getComponentClassName(componentExtractor.getComponentElement()));
                        continue mainLoop;
                    }
                }

                typeNames.add(TypeName.get(typeMirror));
            }

            return typeNames;
        }
    }
}
