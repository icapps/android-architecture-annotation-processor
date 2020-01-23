package com.icapps.daggerlint.detectors.util

import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.icapps.daggerlint.Constants
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass

object Helper {
    fun findParents(node: PsiClass, first: Boolean): List<PsiClass> {
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

    fun createImportFix(context: JavaContext, importFQDN: String): ImportHelper {
        val imports = context.uastFile?.imports

        var importStatement = "import $importFQDN"
        var importLocation: Location? = null
        var importBefore = false
        if (imports != null && imports.isNotEmpty()) {
            if (imports.any { it.asSourceString() == importStatement }) return ImportHelper(needsFQDNAnnotation = false, fix = null)

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
            return ImportHelper(needsFQDNAnnotation = true, fix = null)
        if (importBefore)
            importStatement += '\n'

        val builder = LintFix.create()
            .replace()
            .with(importStatement)
            .range(importLocation)
            .reformat(true)

        if (importBefore)
            builder.beginning()
        else
            builder.end()

        return ImportHelper(needsFQDNAnnotation = false, fix = builder.build())
    }

    fun createAnnotationFix(context: JavaContext, node: UClass, annotationName: String): LintFix {
        val comments = node.comments
        var beginning = true
        var toInsert = "@$annotationName"
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

        val annotationFixBuilder = LintFix.create()
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

    data class ImportHelper(val needsFQDNAnnotation: Boolean, val fix: LintFix?)
}