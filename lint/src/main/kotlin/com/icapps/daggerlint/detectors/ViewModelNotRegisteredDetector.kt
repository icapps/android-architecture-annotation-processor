package com.icapps.daggerlint.detectors

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.icapps.daggerlint.Constants
import com.icapps.daggerlint.Constants.JAVAX_INJECT_CLASSNAME
import com.icapps.daggerlint.detectors.util.Helper
import com.intellij.lang.jvm.JvmModifier
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

class ViewModelNotRegisteredDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.Companion.create(
            "ViewModelNotRegistered",
            "Dagger generated lint",
            "This checks for view models which are injected but not registered with @GenerateViewModelInjector",
            category = Category.CORRECTNESS,
            priority = 10,
            severity = Severity.ERROR,
            suppressAnnotations = null,
            enabledByDefault = true,
            moreInfo = null,
            implementation = Implementation(
                ViewModelNotRegisteredDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UClass::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                //If we have the annotation, no need to do anything
                if (node.findAnnotation(Constants.GENERATE_VIEW_MODEL_CLASSNAME) != null) {
                    return
                } else if (node.hasModifier(JvmModifier.ABSTRACT) || node.isInterface) {
                    return
                }
                if (!isInjectConstructed(node)) {
                    return
                }
                val allParents = Helper.findParents(node, first = true)
                if (allParents.any { it.qualifiedName == Constants.ANDROID_VIEW_MODEL_CLASSNAME }) {
                    checkClass(context, node)
                }
            }
        }
    }

    private fun isInjectConstructed(node: UClass): Boolean {
        return node.constructors.size == 1 && node.constructors[0].hasAnnotation(JAVAX_INJECT_CLASSNAME)
    }

    private fun checkClass(context: JavaContext, node: UClass) {
        context.report(
            ISSUE, node, context.getNameLocation(node), "Missing @${Constants.GENERATE_VIEW_MODEL_CLASSNAME_SHORT} annotation", createFix(context, node)
        )
    }

    private fun createFix(context: JavaContext, node: UClass): LintFix {
        val importHelper = Helper.createImportFix(context, Constants.GENERATE_VIEW_MODEL_CLASSNAME)
        val annotationFix =
            Helper.createAnnotationFix(context, node, if (importHelper.needsFQDNAnnotation) Constants.GENERATE_VIEW_MODEL_CLASSNAME else Constants.GENERATE_VIEW_MODEL_CLASSNAME_SHORT)

        if (importHelper.fix == null) return annotationFix

        return fix().name(
            "Add annotation",
            true
        ).composite(importHelper.fix, annotationFix)
    }

}