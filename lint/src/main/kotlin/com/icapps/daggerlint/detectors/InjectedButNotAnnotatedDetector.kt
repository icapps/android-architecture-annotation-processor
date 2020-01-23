package com.icapps.daggerlint.detectors

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.icapps.daggerlint.Constants
import com.icapps.daggerlint.Constants.JAVAX_INJECT_CLASSNAME
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

class InjectedButNotAnnotatedDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.Companion.create(
            "InjectedButNotAnnotated",
            "Dagger generated lint",
            "This checks for superclasses of android activities that contain injected code and are not annotated with @AndroidInjected",
            category = Category.CORRECTNESS,
            priority = 10,
            severity = Severity.ERROR,
            suppressAnnotations = null,
            enabledByDefault = true,
            moreInfo = null,
            implementation = Implementation(
                InjectedButNotAnnotatedDetector::class.java,
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
                if (node.findAnnotation(Constants.GENERATE_INJECTION_CLASSNAME) != null) {
                    return
                } else if (node.hasModifier(JvmModifier.ABSTRACT) || node.isInterface) {
                    return
                }
                val allParents = findParents(node, first = true)
                if (allParents.any { it.qualifiedName in Constants.ALL_ANDROID_INJECTED }) {
                    checkClass(context, node, allParents)
                }
            }
        }
    }

    private fun checkClass(context: JavaContext, node: UClass, parents: List<PsiClass>) {
        val allClassesToCheck = parents.toMutableList().also { it.add(0, node) }
        if (allClassesToCheck.any { hasInjected(it) }) {
            context.report(
                ISSUE, node, context.getNameLocation(node), "Missing @AndroidInjected annotation", createFix(context, node)
            )
        }
    }

    private fun createFix(context: JavaContext, node: UClass): LintFix {

        val importHelper = createImportFix(context)
        val annotationFix = createAnnotationFix(context, node, fqDn = (importHelper == null))

        if (importHelper == null) return annotationFix

        return fix().name(
            "Add annotation and import",
            true
        ).composite(importHelper, annotationFix)
    }

    private fun hasInjected(psiClass: PsiClass): Boolean {
        return psiClass.allFields.any { it.hasAnnotation(JAVAX_INJECT_CLASSNAME) }
    }

    private fun findParents(node: PsiClass, first: Boolean): List<PsiClass> {
        if (node.qualifiedName == Constants.JAVA_OBJECT_CLASSNAME)
            return emptyList()

        val nodeParents = node.supers
        if (nodeParents.isEmpty())
            return emptyList()

        val parents = mutableListOf<PsiClass>()
        if (!first)
            parents.add(node)

        nodeParents.forEach { parents.addAll(findParents(it, first = false)) }
        return parents.distinctBy { it.qualifiedName }
    }

    private fun createAnnotationFix(context: JavaContext, node: UClass, fqDn: Boolean): LintFix {
        val comments = node.comments
        var beginning = true
        var toInsert = if (fqDn) "@${Constants.GENERATE_INJECTION_CLASSNAME}" else "@${Constants.GENERATE_INJECTION_CLASSNAME_SHORT}"
        val useRange = if (comments.isNotEmpty()) {
            val psi = comments.last().sourcePsi
            if (psi != null) {
                beginning = false
                context.getLocation(psi)
            } else
                context.getLocation(node as PsiElement)
        } else {
            context.getLocation(node as PsiElement)
        }
        if (beginning) {
            toInsert += "\n"
        }

        val annotationFixBuilder = fix()
            .name(
                "Add annotation",
                true
            ).replace()
            .with(toInsert)
            .range(useRange)
            .shortenNames().reformat(true)

        if (beginning) {
            annotationFixBuilder.beginning()
        } else {
            annotationFixBuilder.end()
        }

        return annotationFixBuilder.build()
    }

    private fun createImportFix(context: JavaContext): LintFix? {
        val imports = context.uastFile?.imports

        var importStatement = "import ${Constants.GENERATE_INJECTION_CLASSNAME}"
        var importLocation: Location? = null
        var importBefore = false
        if (imports != null && imports.isNotEmpty()) {
            //Find position to insert to
            val index = imports.indexOfFirst { it.asSourceString() > importStatement }
            if (index == -1) {
                importLocation = context.getLocation(imports.last())
            } else if (index == 0) {
                importLocation = context.getLocation(imports.first())
                importBefore = true
            } else {
                importLocation = context.getLocation(imports[index])
                importBefore = true
            }
        }
        if (importLocation == null)
            return null
        if (importBefore)
            importStatement += '\n'

        val builder = fix()
            .replace()
            .with(importStatement)
            .range(importLocation)
            .reformat(true)

        if (importBefore)
            builder.beginning()
        else
            builder.end()

        return builder.build()
    }

}