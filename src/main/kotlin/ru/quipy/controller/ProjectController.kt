package ru.quipy.controller

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.quipy.api.aggregate.ProjectAggregate
import ru.quipy.api.aggregate.UserAggregate
import ru.quipy.api.event.*
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.commands.*
import ru.quipy.logic.state.ProjectAggregateState
import ru.quipy.logic.state.StatusEntity
import ru.quipy.logic.state.TaskEntity
import ru.quipy.logic.state.UserAggregateState
import ru.quipy.projections.ProjectEntity
import ru.quipy.projections.Status
import ru.quipy.projections.Task
import ru.quipy.projections.UserAccount
import ru.quipy.service.ProjectService
import java.util.*

@RestController
@RequestMapping("/projects")
class ProjectController(
    val projectEsService: EventSourcingService<UUID, ProjectAggregate, ProjectAggregateState>,
    val userEsService: EventSourcingService<UUID, UserAggregate, UserAggregateState>,
    val projectService: ProjectService
) {

    @PostMapping("/{projectTitle}")
    fun createProject(@PathVariable projectTitle: String, @RequestParam creatorId: UUID) : ProjectCreatedEvent {
        val projectId = UUID.randomUUID()
        val project = projectEsService.create { it.create(projectId, projectTitle, creatorId) }
        userEsService.update(creatorId) {
            it.addProject(projectId)
        }
        return project
    }

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID) : ProjectEntity {
        return projectService.getProject(projectId)
    }

    @PostMapping("/{projectId}/tasks/{taskName}")
    fun createTask(@PathVariable projectId: UUID, @PathVariable taskName: String) : ProjectTaskCreatedEvent {
        return projectEsService.update(projectId) {
            it.addTask(taskName)
        }
    }
    @PostMapping("/{projectId}/tasks/status")
    fun changeTaskStatus(@PathVariable projectId: UUID, @RequestParam taskId: UUID, @RequestParam statusId: UUID) : ProjectTaskStatusChangedEvent {
        return projectEsService.update(projectId) {
            it.projectTaskStatusChange(statusId, taskId)
        }
    }

    @PostMapping("/{projectId}/tasks/assign")
    fun assignMemberToTask(@PathVariable projectId: UUID, @RequestParam taskId: UUID, @RequestParam memberId: UUID) : ProjectTaskMemberAssignedEvent {
        return projectEsService.update(projectId) {
            it.projectTaskMemberAssign(memberId, taskId)
        }
    }

    @PostMapping("/{projectId}/tasks/title")
    fun changeTaskTitle(@PathVariable projectId: UUID, @RequestParam taskId: UUID, @RequestParam title: String) : ProjectTaskTitleChangedEvent {
        return projectEsService.update(projectId) {
            it.projectTaskTitleChange(title, taskId)
        }
    }

    @PostMapping("/{projectId}/members")
    fun addMember(@PathVariable projectId: UUID, @RequestParam memberId: UUID) : ProjectMemberAddedEvent {
        return projectEsService.update(projectId) {
            it.projectMemberAdd(memberId)
        }
    }

    @PostMapping("/{projectId}/title")
    fun changeTitle(@PathVariable projectId: UUID, @RequestParam title: String) : ProjectTitleChangedEvent {
        return projectEsService.update(projectId) {
            it.projectTitleChange(title);
        }
    }

    @DeleteMapping("/{projectId}/status")
    fun deleteStatus(@PathVariable projectId: UUID, @RequestParam statusId: UUID) : ProjectStatusDeletedEvent? {
        if (projectService.checkDeleteStatusDefault(statusId)) {
            return null
        }

        if (projectService.checkDeleteStatusUsage(statusId, projectId)) {
            return null
        }

        return projectEsService.update(projectId) {
            it.projectStatusDelete(statusId)
        }
    }

    @PostMapping("/{projectId}/status")
    fun addStatus(@PathVariable projectId: UUID, @RequestParam statusTitle: String, @RequestParam color: String) : ProjectStatusAddedEvent {
        return projectEsService.update(projectId) {
            it.projectStatusCreate(statusTitle, color)
        }
    }

    @GetMapping("/{projectId}/members")
    fun getProjectMembers(@PathVariable projectId: UUID): MutableSet<UUID> {
        return projectService.getProjectMembers(projectId)
    }

    @GetMapping("/{projectId}/tasks")
    fun getProjectTasks(@PathVariable projectId: UUID): MutableMap<UUID, Task> {
        return projectService.getProjectTasks(projectId)
    }

    @GetMapping("/{projectId}/statuses")
    fun getProjectStatuses(@PathVariable projectId: UUID): MutableMap<UUID, Status> {
        return projectService.getProjectStatuses(projectId)
    }

    @GetMapping("/{projectId}/found_member")
    fun getProjectUserByName(@PathVariable projectId: UUID, @RequestParam name: String): MutableSet<UserAccount> {
        return projectService.getProjectUserByName(projectId, name)
    }


    @GetMapping("/all")
    fun getAllProjects(): MutableList<ProjectEntity> {
        return projectService.getAllProjects()
    }
}