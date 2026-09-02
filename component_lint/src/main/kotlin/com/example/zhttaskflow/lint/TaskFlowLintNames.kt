package com.example.zhttaskflow.lint

internal object TaskFlowLintIds {
    const val NO_TOAST_IN_FEATURE = "TaskFlowNoToastInFeature"
    const val NO_BARE_SCAFFOLD_IN_FEATURE = "TaskFlowNoBareScaffoldInFeature"
    const val LOG_UI_INTERACTION_MISSING_PAGE_ID = "TaskFlowLogUiInteractionMissingPageId"
}

internal fun isFeatureLintScope(projectName: String, packageName: String?): Boolean {
    if (projectName.removePrefix(":").startsWith("feature_")) {
        return true
    }
    return packageName?.contains(".feature.") == true
}
