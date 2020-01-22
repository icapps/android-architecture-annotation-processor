/**
 * Copyright (C) 2010-2016 eBusiness Information, Excilys Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.icapps.arch.util

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.annotation.processing.ProcessingEnvironment
import javax.tools.FileObject
import javax.tools.StandardLocation

object FileHelper {
    @Throws(FileNotFoundException::class)
    fun findRootProject(processingEnv: ProcessingEnvironment): File {
        val rootProjectHolder =
                findRootProjectHolder(processingEnv)
        return rootProjectHolder.projectRoot
    }

    /**
     * We use a dirty trick to find the AndroidManifest.xml file, since it's not
     * available in the classpath. The idea is quite simple : create a fake class
     * file, retrieve its URI, and start going up in parent folders to find the
     * AndroidManifest.xml file. Any better solution will be appreciated.
     */
    fun findRootProjectHolder(processingEnv: ProcessingEnvironment): FileHolder {
        val filer = processingEnv.filer
        val dummySourceFile: FileObject
        dummySourceFile = try {
            filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "dummy" + System.currentTimeMillis())
        } catch (ignored: IOException) {
            throw FileNotFoundException()
        }
        var dummySourceFilePath = dummySourceFile.toUri().toString()
        if (dummySourceFilePath.startsWith("file:")) {
            if (!dummySourceFilePath.startsWith("file://")) {
                dummySourceFilePath = "file://" + dummySourceFilePath.substring("file:".length)
            }
        } else {
            dummySourceFilePath = "file://$dummySourceFilePath"
        }
        val cleanURI: URI
        cleanURI = try {
            URI(dummySourceFilePath)
        } catch (e: URISyntaxException) {
            throw FileNotFoundException()
        }
        val dummyFile = File(cleanURI)
        val sourcesGenerationFolder = dummyFile.parentFile
        val projectRoot = sourcesGenerationFolder.parentFile
        return FileHolder(dummySourceFilePath, sourcesGenerationFolder, projectRoot)
    }

    @Throws(FileNotFoundException::class)
    fun resolveOutputDirectory(processingEnv: ProcessingEnvironment): File {
        val rootProject = findRootProject(processingEnv)
        // Target folder - Maven
        val targetFolder = File(rootProject, "target")
        if (targetFolder.isDirectory && targetFolder.canWrite()) {
            return targetFolder
        }
        // Build folder - Gradle
        val buildFolder = File(rootProject, "build")
        if (buildFolder.isDirectory && buildFolder.canWrite()) {
            return buildFolder
        }
        // Bin folder - Eclipse
        val binFolder = File(rootProject, "bin")
        return if (binFolder.isDirectory && binFolder.canWrite()) {
            binFolder
        } else rootProject
        // Fallback to projet root folder
    }

    class FileHolder(
        var dummySourceFilePath: String,
        var sourcesGenerationFolder: File,
        var projectRoot: File
    )
}