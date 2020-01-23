package com.icapps.daggerlint

import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.icapps.daggerlint.detectors.InjectedButNotAnnotatedDetector

class IssueRegistry : com.android.tools.lint.client.api.IssueRegistry() {

    override val api: Int = CURRENT_API

    override val issues: List<Issue> = listOf(InjectedButNotAnnotatedDetector.ISSUE)

}