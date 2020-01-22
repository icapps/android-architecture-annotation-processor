/**
 * Copyright (C) 2010-2016 eBusiness Information, Excilys Group
 * Copyright (C) 2016-2018 the AndroidAnnotations project
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

import java.util.HashMap

internal data class AndroidManifest constructor(val isLibraryProject: Boolean,
                                                val applicationPackage: String,
                                                val applicationClassName: String?,
                                                val componentQualifiedNames: List<String>,
                                                val metaDataQualifiedNames: Map<String, MetaDataInfo>,
                                                val permissionQualifiedNames: List<String>?,
                                                val minSdkVersion: Int,
                                                val maxSdkVersion: Int,
                                                val targetSdkVersion: Int,
                                                val isDebuggable: Boolean) {

    override fun toString(): String {
        return ("AndroidManifest [applicationPackage=" + applicationPackage + ", componentQualifiedNames=" + componentQualifiedNames + ", metaDataQualifiedNames=" + metaDataQualifiedNames
                + ", permissionQualifiedNames=" + permissionQualifiedNames + ", applicationClassName=" + applicationClassName + ", libraryProject=" + isLibraryProject + ", debugabble=" + isDebuggable
                + ", minSdkVersion=" + minSdkVersion + ", maxSdkVersion=" + maxSdkVersion + ", targetSdkVersion=" + targetSdkVersion + "]")
    }

    class MetaDataInfo(val name: String, val value: String, val resource: String) {

        override fun toString(): String {
            return "{name='$name', value='$value', resource='$resource'}"
        }

    }

    companion object {
        fun createManifest(applicationPackage: String,
                           applicationClassName: String?,
                           componentQualifiedNames: List<String>,
                           metaDataQualifiedNames: Map<String, MetaDataInfo>,
                           permissionQualifiedNames: List<String>?,
                           minSdkVersion: Int,
                           maxSdkVersion: Int,
                           targetSdkVersion: Int,
                           debugabble: Boolean): AndroidManifest {
            return AndroidManifest(
                    false,
                    applicationPackage,
                    applicationClassName,
                    componentQualifiedNames,
                    metaDataQualifiedNames,
                    permissionQualifiedNames,
                    minSdkVersion,
                    maxSdkVersion,
                    targetSdkVersion,
                    debugabble
            )
        }

        fun createLibraryManifest(applicationPackage: String,
                                  minSdkVersion: Int,
                                  maxSdkVersion: Int,
                                  targetSdkVersion: Int): AndroidManifest {
            return AndroidManifest(
                    true,
                    applicationPackage,
                    "",
                    emptyList(),
                    HashMap(),
                    emptyList(),
                    minSdkVersion,
                    maxSdkVersion,
                    targetSdkVersion,
                    false
            )
        }
    }

}