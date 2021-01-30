package com.github.igrrik.kmmfrozeninspection.services

import com.github.igrrik.kmmfrozeninspection.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
