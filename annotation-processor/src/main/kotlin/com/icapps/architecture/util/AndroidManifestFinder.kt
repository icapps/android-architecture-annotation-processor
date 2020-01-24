/**
 * Copyright (C) 2010-2016 eBusiness Information, Excilys Group
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.icapps.architecture.util

import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.ArrayList
import java.util.Properties
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.annotation.processing.ProcessingEnvironment
import javax.tools.Diagnostic
import javax.xml.parsers.DocumentBuilderFactory

internal class AndroidManifestFinder(private val environment: ProcessingEnvironment) {

    fun extractAndroidManifest(): AndroidManifest {
        return try {
            val androidManifestFile = findManifestFile()
            val projectDirectory = androidManifestFile!!.parent
            val projectProperties = File(projectDirectory, "project.properties")
            var libraryProject = false
            if (projectProperties.exists()) {
                val properties = Properties()
                try {
                    properties.load(FileInputStream(projectProperties))
                    if (properties.containsKey("android.library")) {
                        val androidLibraryProperty = properties.getProperty("android.library")
                        libraryProject = androidLibraryProperty == "true"
                    }
                } catch (ignored: IOException) { // we assume the project is not a library
                }
            }
            parse(androidManifestFile, libraryProject)
        } catch (exception: FileNotFoundException) {
            throw NoSuchElementException("Unable to find AndroidManifest.xml")
        }
    }

    @Throws(FileNotFoundException::class)
    private fun findManifestFile(): File? {
        return findManifestInKnownPaths()
    }

    @Throws(FileNotFoundException::class)
    private fun findManifestInKnownPaths(): File? {
        val holder = FileHelper.findRootProjectHolder(environment)
        return findManifestInKnownPathsStartingFromGenFolder(holder.sourcesGenerationFolder.absolutePath)
    }

    @Throws(FileNotFoundException::class)
    fun findManifestInKnownPathsStartingFromGenFolder(sourcesGenerationFolder: String): File? {
        val strategies: Iterable<AndroidManifestFinderStrategy> = listOf(
            GradleAptAndroidManifestFinderStrategy(sourcesGenerationFolder),
            GradleMergedManifestFinderStrategy(sourcesGenerationFolder),
            MavenAndroidManifestFinderStrategy(sourcesGenerationFolder)
        )
        var applyingStrategy: AndroidManifestFinderStrategy? = null
        for (strategy in strategies) {
            if (strategy.applies()) {
                applyingStrategy = strategy
                break
            }
        }

        var androidManifestFile: File? = null
        if (applyingStrategy != null) {
            androidManifestFile = applyingStrategy.findAndroidManifestFile()
        }
        if (androidManifestFile == null) {
            environment.messager.printMessage(Diagnostic.Kind.ERROR, "Failed to find manifest file")
        }
        return androidManifestFile
    }

    private abstract class AndroidManifestFinderStrategy internal constructor(sourceFolderPattern: Pattern, sourceFolder: String) {
        val matcher: Matcher = sourceFolderPattern.matcher(sourceFolder)
        fun findAndroidManifestFile(): File? {
            for (location in possibleLocations()) {
                val manifestFile = File(matcher.group(1), "$location/AndroidManifest.xml")
                if (manifestFile.exists()) {
                    return manifestFile
                }
            }
            return null
        }

        fun applies(): Boolean {
            return matcher.matches()
        }

        abstract fun possibleLocations(): Iterable<String>

    }

    private class GradleAptAndroidManifestFinderStrategy(sourceFolder: String) : AndroidManifestFinderStrategy(
        GRADLE_GEN_FOLDER, sourceFolder
    ) {
        companion object {
            val GRADLE_GEN_FOLDER: Pattern = Pattern.compile("^(.*?)build[\\\\/]generated[\\\\/]source[\\\\/]apt(.*)$")
        }

        override fun possibleLocations(): Iterable<String> {
            val gradleVariant = matcher.group(2)
            return listOf(
                "build/intermediates/manifests/full$gradleVariant",
                "build/intermediates/bundles$gradleVariant"
            )
        }
    }

    private class GradleMergedManifestFinderStrategy(sourceFolder: String) : AndroidManifestFinderStrategy(
        GRADLE_GEN_FOLDER, sourceFolder
    ) {
        companion object {
            val GRADLE_GEN_FOLDER: Pattern = Pattern.compile("^(.*?)build[\\\\/]generated[\\\\/]source[\\\\/]kapt(.*)$")
        }

        override fun possibleLocations(): Iterable<String> {
            val gradleVariant = matcher.group(2)
            return listOf(
                "build/intermediates/merged_manifests/$gradleVariant",
                "build/intermediates/library_manifest/$gradleVariant"
            )
        }
    }

    private class MavenAndroidManifestFinderStrategy(sourceFolder: String) : AndroidManifestFinderStrategy(
        MAVEN_GEN_FOLDER, sourceFolder
    ) {
        companion object {
            val MAVEN_GEN_FOLDER: Pattern = Pattern.compile("^(.*?)target[\\\\/]generated-sources.*$")
        }

        override fun possibleLocations(): Iterable<String> {
            return listOf("target", "src/main", "")
        }
    }

    private fun parse(androidManifestFile: File?, libraryProject: Boolean): AndroidManifest {
        val docBuilderFactory = DocumentBuilderFactory.newInstance()
        val doc = try {
            val docBuilder = docBuilderFactory.newDocumentBuilder()
            docBuilder.parse(androidManifestFile)
        } catch (e: Exception) {
            throw IllegalStateException("Could not parse the AndroidManifest.xml file at path {}$androidManifestFile", e)
        }
        val documentElement = doc.documentElement
        documentElement.normalize()
        val applicationPackage = documentElement.getAttribute("package")
        var minSdkVersion = -1
        var maxSdkVersion = -1
        var targetSdkVersion = -1
        val sdkNodes = documentElement.getElementsByTagName("uses-sdk")
        if (sdkNodes.length > 0) {
            val sdkNode = sdkNodes.item(0)
            minSdkVersion = extractAttributeIntValue(sdkNode, "android:minSdkVersion")
            maxSdkVersion = extractAttributeIntValue(sdkNode, "android:maxSdkVersion")
            targetSdkVersion = extractAttributeIntValue(sdkNode, "android:targetSdkVersion")
        }
        if (libraryProject) {
            return AndroidManifest.createLibraryManifest(
                applicationPackage,
                minSdkVersion,
                maxSdkVersion,
                targetSdkVersion
            )
        }
        val applicationNodes = documentElement.getElementsByTagName("application")
        var applicationClassQualifiedName: String? = null
        var applicationDebuggableMode = false
        if (applicationNodes.length > 0) {
            val applicationNode = applicationNodes.item(0)
            val nameAttribute = applicationNode.attributes.getNamedItem("android:name")
            applicationClassQualifiedName = manifestNameToValidQualifiedName(applicationPackage, nameAttribute)
            if (applicationClassQualifiedName == null) {
                if (nameAttribute != null) {
                    environment.messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "The class application declared in the AndroidManifest.xml cannot be found in the compile path: [${nameAttribute.nodeValue}]"
                    )
                }
            }
            val debuggableAttribute =
                applicationNode.attributes.getNamedItem("android:debuggable")
            if (debuggableAttribute != null) {
                applicationDebuggableMode = debuggableAttribute.nodeValue.equals("true", ignoreCase = true)
            }
        }
        val activityNodes = documentElement.getElementsByTagName("activity")
        val activityQualifiedNames = extractComponentNames(applicationPackage, activityNodes)
        val serviceNodes = documentElement.getElementsByTagName("service")
        val serviceQualifiedNames = extractComponentNames(applicationPackage, serviceNodes)
        val receiverNodes = documentElement.getElementsByTagName("receiver")
        val receiverQualifiedNames = extractComponentNames(applicationPackage, receiverNodes)
        val providerNodes = documentElement.getElementsByTagName("provider")
        val providerQualifiedNames = extractComponentNames(applicationPackage, providerNodes)
        val componentQualifiedNames: MutableList<String> = ArrayList()
        componentQualifiedNames.addAll(activityQualifiedNames)
        componentQualifiedNames.addAll(serviceQualifiedNames)
        componentQualifiedNames.addAll(receiverQualifiedNames)
        componentQualifiedNames.addAll(providerQualifiedNames)

        val usesPermissionNodes = documentElement.getElementsByTagName("uses-permission")
        val permissionQualifiedNames = extractUsesPermissionNames(usesPermissionNodes)
        return AndroidManifest.createManifest(
            applicationPackage,
            applicationClassQualifiedName,
            componentQualifiedNames,
            emptyMap(),
            permissionQualifiedNames,
            minSdkVersion,
            maxSdkVersion,
            targetSdkVersion,
            applicationDebuggableMode
        )
    }

    private fun extractAttributeIntValue(node: Node, attribute: String): Int {
        try {
            val attributes = node.attributes
            if (attributes.length > 0) {
                val attributeNode = attributes.getNamedItem(attribute)
                if (attributeNode != null) {
                    return attributeNode.nodeValue.toInt()
                }
            }
        } catch (ignored: NumberFormatException) { // we assume the manifest is well-formed
        }
        return -1
    }

    private fun extractComponentNames(applicationPackage: String, componentNodes: NodeList): List<String> {
        val componentQualifiedNames: MutableList<String> = ArrayList()
        for (i in 0 until componentNodes.length) {
            val activityNode = componentNodes.item(i)
            val nameAttribute = activityNode.attributes.getNamedItem("android:name")
            val qualifiedName = manifestNameToValidQualifiedName(applicationPackage, nameAttribute)
            if (qualifiedName != null) {
                componentQualifiedNames.add(qualifiedName)
            } else {
                if (nameAttribute != null) {
                    environment.messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "A class activity declared in the AndroidManifest.xml cannot be found in the compile path: [${nameAttribute.nodeValue}]"
                    )
                } else {
                    environment.messager.printMessage(
                        Diagnostic.Kind.WARNING,
                        "The {} activity node in the AndroidManifest.xml has no android:name attribute: $i"
                    )
                }
            }
        }
        return componentQualifiedNames
    }

    private fun manifestNameToValidQualifiedName(applicationPackage: String, nameAttribute: Node?): String? {
        return if (nameAttribute != null) {
            val activityName = nameAttribute.nodeValue
            if (activityName.startsWith(applicationPackage)) {
                returnClassIfExistsOrNull(activityName)
            } else {
                if (activityName.startsWith(".")) {
                    returnClassIfExistsOrNull(applicationPackage + activityName)
                } else {
                    if (classOrModelClassExists(activityName)) {
                        activityName
                    } else {
                        returnClassIfExistsOrNull("$applicationPackage.$activityName")
                    }
                }
            }
        } else {
            null
        }
    }

    private fun classOrModelClassExists(className: String): Boolean {
        val elementUtils = environment.elementUtils
        return elementUtils.getTypeElement(className) != null
    }

    private fun returnClassIfExistsOrNull(className: String): String? {
        return if (classOrModelClassExists(className)) {
            className
        } else {
            null
        }
    }

    private fun extractUsesPermissionNames(usesPermissionNodes: NodeList): List<String>? {
        val usesPermissionQualifiedNames: MutableList<String> = ArrayList()
        for (i in 0 until usesPermissionNodes.length) {
            val usesPermissionNode = usesPermissionNodes.item(i)
            val nameAttribute = usesPermissionNode.attributes.getNamedItem("android:name") ?: return null
            usesPermissionQualifiedNames.add(nameAttribute.nodeValue)
        }
        return usesPermissionQualifiedNames
    }

}