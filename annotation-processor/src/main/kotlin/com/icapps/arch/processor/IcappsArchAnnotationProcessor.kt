package com.icapps.arch.processor

import com.google.auto.service.AutoService
import com.icapps.arch.annotation.AndroidInjected
import com.icapps.arch.annotation.GenerateViewModelInjector
import com.icapps.arch.processor.IcappsArchAnnotationProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.icapps.arch.util.AndroidManifestFinder
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.PrintWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class IcappsArchAnnotationProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        private const val ANDROID_INJECTED_MODULE_CLASS_NAME = "GeneratedAndroidInjectedModule"
        private const val VIEWMODEL_MODULE_CLASS_NAME = "GeneratedViewModelModule"
    }

    private lateinit var processingEnvironment: ProcessingEnvironment
    private lateinit var appPackage: String

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(
        AndroidInjected::class.java.canonicalName,
        GenerateViewModelInjector::class.java.canonicalName
    )

    override fun init(processingEnvironment: ProcessingEnvironment) {
        this.processingEnvironment = processingEnvironment

        val manifest = AndroidManifestFinder(processingEnvironment).extractAndroidManifest()
        appPackage = "${manifest.applicationPackage}.generated"
    }

    override fun process(elements: MutableSet<out TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        if (elements.isEmpty() || roundEnvironment.processingOver()) return false

        val activities = ElementFilter.typesIn(roundEnvironment.getElementsAnnotatedWith(AndroidInjected::class.java))
        val viewmodels = ElementFilter.typesIn(roundEnvironment.getElementsAnnotatedWith(GenerateViewModelInjector::class.java))
        generateAndroidInjectedModule(activities, elements)
        generateViewmodelBindingModule(viewmodels, elements)

        return true
    }

    // Generates the following:
    // @Module
    // public interface AndroidInjectedModule {
    //   @ContributesAndroidInjector
    //   xxxActivity bindXxActivity()
    // }
    private fun generateAndroidInjectedModule(activities: Set<TypeElement>, elements: MutableSet<out TypeElement>) {
        val ifaceBuilder = TypeSpec.interfaceBuilder(ANDROID_INJECTED_MODULE_CLASS_NAME)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("dagger", "Module"))

        activities.forEach { activity ->
            val funSpec = MethodSpec.methodBuilder("bind${activity.simpleName}")
                .addAnnotation(ClassName.get("dagger.android", "ContributesAndroidInjector"))
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(TypeName.get(activity.asType()))
                .build()
            ifaceBuilder.addMethod(funSpec)
        }

        val fileBuilder = JavaFile.builder(appPackage, ifaceBuilder.build())
        val fileContents = fileBuilder.build().toString()
        writeSourceFile(
            fileContents,
            ANDROID_INJECTED_MODULE_CLASS_NAME, elements
        )
    }

    // Generates the following:
    // @Module
    // public interface AndroidInjectedModule {
    //    @Binds
    //    @IntoMap
    //    @ViewModelKey(ExampleViewModel::class)
    //    internal abstract fun bindExampleViewModel(exampleViewModel: ExampleViewModel): ViewModel
    // }
    private fun generateViewmodelBindingModule(viewmodels: Set<TypeElement>, elements: MutableSet<out TypeElement>) {
        val ifaceBuilder = TypeSpec.interfaceBuilder(VIEWMODEL_MODULE_CLASS_NAME)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("dagger", "Module"))

        viewmodels.forEach { viewmodel ->
            val viewModelKeyAnnotation =
                AnnotationSpec.builder(ClassName.get("com.icapps.architecture.di", "ViewModelKey"))
                    .addMember("value", "\$T.class", TypeName.get(viewmodel.asType()))
                    .build()

            val funSpec = MethodSpec.methodBuilder("bind${viewmodel.simpleName}")
                .addAnnotation(ClassName.get("dagger", "Binds"))
                .addAnnotation(ClassName.get("dagger.multibindings", "IntoMap"))
                .addAnnotation(viewModelKeyAnnotation)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.get(viewmodel.asType()), viewmodel.simpleName.toString())
                .returns(ClassName.get("androidx.lifecycle", "ViewModel"))
                .build()
            ifaceBuilder.addMethod(funSpec)
        }

        val fileBuilder = JavaFile.builder(appPackage, ifaceBuilder.build())
        val fileContents = fileBuilder.build().toString()
        writeSourceFile(
            fileContents,
            VIEWMODEL_MODULE_CLASS_NAME, elements
        )
    }

    private fun writeSourceFile(fileContents: String, className: String, elements: MutableSet<out TypeElement>) {
        val generatedSourcesRoot: String = processingEnvironment.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
        if (generatedSourcesRoot.isEmpty()) {
            processingEnvironment.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to find generated files directory"
            )
            return
        }

        val fileObject = processingEnvironment.filer.createSourceFile("$appPackage.$className", *elements.toTypedArray())
        PrintWriter(fileObject.openWriter()).use { out ->
            out.print(fileContents)
            out.close()
        }
    }

}