package ru.quipy.service

import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestParam
import ru.quipy.logic.state.UserAggregateState
import ru.quipy.projections.*
import java.util.*

@Service
class ProjectService (
    val projectCacheRepository: ProjectCacheRepository,
    val userCacheRepository: UserAccountCacheRepository
) {

    fun getProject(projectId: UUID): ProjectEntity {
        return projectCacheRepository.findById(projectId).get()
    }

    fun getAllProjects(): MutableList<ProjectEntity> {
        return projectCacheRepository.findAll()
    }

    fun getProjectMembers(projectId: UUID): MutableSet<UUID> {
        return projectCacheRepository.findById(projectId).get().members
    }

    fun getProjectTasks(projectId: UUID): MutableMap<UUID, Task> {
        return projectCacheRepository.findById(projectId).get().tasks
    }

    fun getProjectStatuses(projectId: UUID): MutableMap<UUID, Status> {
        return projectCacheRepository.findById(projectId).get().projectStatuses
    }

    fun checkDeleteStatusDefault(statusId: UUID): Boolean {
         return statusId == UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    fun checkDeleteStatusUsage(statusId: UUID, projectId: UUID): Boolean {
        val tasks = projectCacheRepository.findById(projectId).get().tasks
        tasks.values.forEach{
            if (it.statusAssigned == statusId)
                return true
        }
        return false
    }

    fun  getProjectUserByName(projectId: UUID, name: String): MutableSet<UserAccount> {
        val foundedUsers = mutableSetOf<UserAccount>()
        val projectData = projectCacheRepository.findById(projectId).get()

        projectData.members.forEach{
            val userData = userCacheRepository.findById(it).get();
            if (userData.userName?.contains(name, true) == true) {
                foundedUsers.add(userData)
            }
        }
        return foundedUsers
    }
}