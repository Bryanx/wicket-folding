package com.github.bryanx.wicketfolding.services

import com.intellij.openapi.project.Project
import com.github.bryanx.wicketfolding.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
