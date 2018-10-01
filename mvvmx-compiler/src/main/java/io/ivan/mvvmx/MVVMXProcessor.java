package io.ivan.mvvmx;

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
public final class MVVMXProcessor extends AbstractProcessor {

    private static final ClassName CLASSNAME_LayoutInflater = ClassName.get("android.view", "LayoutInflater");
    private static final ClassName CLASSNAME_ViewGroup = ClassName.get("android.view", "ViewGroup");
    private static final ClassName CLASSNAME_ViewModelProviders = ClassName.get("android.arch.lifecycle", "ViewModelProviders");
    private static final ClassName CLASSNAME_DataBindingUtil = ClassName.get("android.databinding", "DataBindingUtil");

    private HashMap<TypeElement, MethodSpec.Builder> builderMap = new HashMap<>();
    private HashMap<TypeElement, Name> viewModelNameMap = new HashMap<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();

        annotations.add(DataBinding.class);
        annotations.add(ViewModel.class);

        return annotations;
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message, element);
    }

    private void note(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args);
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        try {

            Set<? extends Element> viewModelElements = roundEnvironment.getElementsAnnotatedWith(ViewModel.class);
            Set<? extends Element> dataBindingElements = roundEnvironment.getElementsAnnotatedWith(DataBinding.class);

            for (Element element : viewModelElements) {
                if (!SuperficialValidation.validateElement(element)) continue;
                if (!(element instanceof VariableElement)) continue;

                Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement.getKind() != ElementKind.CLASS) continue;

                TypeElement typeElement = (TypeElement) enclosingElement;
                VariableElement variableElement = (VariableElement) element;

                // Start by verifying common generated code restrictions.
                if (isInaccessibleViaGeneratedCode(ViewModel.class, "fields", element)
                        || isBindingInWrongPackage(ViewModel.class, element)) {
                    continue;
                }

                Name viewModelName = variableElement.getSimpleName();
                viewModelNameMap.put(typeElement, viewModelName);

                MethodSpec.Builder builder = createTypeBuilder(typeElement);
                builder.addStatement("target.$L = $T.of(target).get($T.class)",
                        viewModelName, CLASSNAME_ViewModelProviders, variableElement.asType());
            }

            for (Element element : dataBindingElements) {
                if (!(element instanceof VariableElement)) continue;

                Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement.getKind() != ElementKind.CLASS) continue;

                TypeElement typeElement = (TypeElement) enclosingElement;
                VariableElement variableElement = (VariableElement) element;

                // Start by verifying common generated code restrictions.
                if (isInaccessibleViaGeneratedCode(DataBinding.class, "fields", element)
                        || isBindingInWrongPackage(DataBinding.class, element)) {
                    continue;
                }

                Name dataBindingName = variableElement.getSimpleName();
                Name viewModelName = viewModelNameMap.get(typeElement);
                int layoutId = variableElement.getAnnotation(DataBinding.class).value();
                int brId = variableElement.getAnnotation(DataBinding.class).BR();

                TypeMirror type = typeElement.asType();
                TypeMirror activityType = processingEnv.getElementUtils().getTypeElement("android.support.v4.app.FragmentActivity").asType();
                TypeMirror fragmentType = processingEnv.getElementUtils().getTypeElement("android.support.v4.app.Fragment").asType();

                MethodSpec.Builder builder = createTypeBuilder(typeElement);
                if (processingEnv.getTypeUtils().isSubtype(type, activityType)) {
                    builder.addStatement("target.$L = $T.setContentView(target, $L)", dataBindingName, CLASSNAME_DataBindingUtil, layoutId);
                } else if (processingEnv.getTypeUtils().isSubtype(type, fragmentType)) {
                    builder.addParameter(CLASSNAME_LayoutInflater, "inflater")
                            .addParameter(CLASSNAME_ViewGroup, "container")
                            .addStatement("target.$L = $T.inflate(inflater, $L, container, false)", dataBindingName, CLASSNAME_DataBindingUtil, layoutId);
                }
                if (viewModelName != null && brId > 0) {
                    builder.addStatement("target.$L.setVariable($L, target.$L)", dataBindingName, brId, viewModelName);
                }
            }

            for (Map.Entry<TypeElement, MethodSpec.Builder> entry : builderMap.entrySet()) {
                TypeElement enclosingElement = entry.getKey();
                MethodSpec.Builder builder = entry.getValue();

                String packageName = processingEnv.getElementUtils().getPackageOf(enclosingElement)
                        .getQualifiedName().toString();
                String className = enclosingElement.getQualifiedName().toString().substring(
                        packageName.length() + 1).replace('.', '$');

                ClassName bindingClassName = ClassName.get(packageName, className + "_ViewBinding");

                TypeSpec result = TypeSpec.classBuilder(bindingClassName.simpleName())
                        .addModifiers(PUBLIC, FINAL)
                        .addMethod(builder.build())
                        .build();

                JavaFile javaFile = JavaFile.builder(packageName, result)
                        .addFileComment("Generated code from Butter Knife. Do not modify!")
                        .build();

                try {
                    javaFile.writeTo(processingEnv.getFiler());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            error(null, e.toString());
        }
        return true;
    }


    private MethodSpec.Builder createTypeBuilder(TypeElement enclosingElement) {
        MethodSpec.Builder builder = builderMap.get(enclosingElement);
        if (builder == null) {
            TypeMirror elementType = enclosingElement.asType();
            if (elementType.getKind() == TypeKind.TYPEVAR) {
                TypeVariable typeVariable = (TypeVariable) elementType;
                elementType = typeVariable.getUpperBound();
            }
            builder = MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(TypeName.get(elementType), "target");
        }
        builderMap.put(enclosingElement, builder);
        return builder;
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify field or method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != ElementKind.CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(Modifier.PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

}
