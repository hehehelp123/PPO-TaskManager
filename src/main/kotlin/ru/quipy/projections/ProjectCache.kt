package ru.quipy.projections

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import ru.quipy.api.aggregate.ProjectAggregate
import ru.quipy.api.aggregate.UserAggregate
import ru.quipy.api.event.*
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.commands.addProject
import ru.quipy.logic.state.StatusEntity
import ru.quipy.logic.state.TaskEntity
import ru.quipy.logic.state.UserAggregateState
import ru.quipy.streams.AggregateSubscriptionsManager
import java.util.*
import javax.annotation.PostConstruct

@Component
class ProjectCache (
    private val projectCacheRepository: ProjectCacheRepository,
) {

    val logger: Logger = LoggerFactory.getLogger(ProjectCache::class.java)

    @Autowired
    lateinit var subscriptionsManager: AggregateSubscriptionsManager

    @PostConstruct
    fun init() {
        subscriptionsManager.createSubscriber(ProjectAggregate::class, "project::event-cache") {
            `when`(ProjectCreatedEvent::class) { event ->
                val project = ProjectEntity(event.projectId, event.title, event.creatorId)
                val status = Status(UUID.fromString("00000000-0000-0000-0000-000000000000"), "CREATED", "default")
                project.members.add(event.creatorId)
                project.projectStatuses[UUID.fromString("00000000-0000-0000-0000-000000000000")] = status
                projectCacheRepository.save(project)
                logger.info("Project created: {} by user {}", event.title, event.creatorId)
            }

            `when`(ProjectTaskCreatedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                val task = Task(event.taskId, event.taskName, UUID.fromString("00000000-0000-0000-0000-000000000000"))
                project.tasks[event.taskId] = task
                projectCacheRepository.save(project)
                logger.info("Task created: {}", event.taskName)
            }

            `when`(ProjectStatusAddedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                val status = Status(event.statusId, event.statusName, event.statusColor)
                project.projectStatuses[event.statusId] = status
                projectCacheRepository.save(project)
                logger.info("Status created: {} with color {}", event.statusName, event.statusColor)
            }

            `when`(ProjectTaskStatusChangedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                val task = project.tasks[event.taskId]
                task?.statusAssigned = event.statusId
                projectCacheRepository.save(project)
                logger.info("Status {} assigned to task {}: ", event.statusId, event.taskId)
            }

            `when`(ProjectMemberAddedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                project.members.add(event.memberId)
                projectCacheRepository.save(project)
                logger.info("User {} added to project {}: ", event.memberId, event.projectId)
            }

            `when`(ProjectTitleChangedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                project.projectTitle = event.title
                projectCacheRepository.save(project)
                logger.info("Project {} changed title to {}: ", event.projectId, event.title)
            }

            `when`(ProjectStatusDeletedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                project.projectStatuses.keys.remove(event.statusId)
                projectCacheRepository.save(project)
                logger.info("Status {} deleted from project {}: ", event.statusId, event.projectId)
            }

            `when`(ProjectTaskMemberAssignedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                val task = project.tasks[event.taskId]
                task?.membersAssigned?.add(event.memberId)
                projectCacheRepository.save(project)
                logger.info("User {} assigned to task {}: ", event.memberId, event.taskId)
            }

            `when`(ProjectTaskTitleChangedEvent::class) { event ->
                val project = projectCacheRepository.findById(event.projectId).get()
                val task = project.tasks[event.taskId]
                task?.title = event.title
                projectCacheRepository.save(project)
                logger.info("Task {} changed title to {}: ", event.taskId, event.title)
            }
        }
    }
}

@Document("project-event-cache")
data class ProjectEntity(
    @Id
    val projectId: UUID,
    var projectTitle: String,
    val creatorId: UUID,
    var members: MutableSet<UUID> = mutableSetOf(),
    var tasks: MutableMap<UUID, Task> = mutableMapOf(),
    var projectStatuses: MutableMap<UUID, Status> = mutableMapOf()
)

data class Task(
    @Id
    val id: UUID,
    var title: String,
    var statusAssigned: UUID,
    val membersAssigned: MutableSet<UUID> = mutableSetOf()
)

data class Status(
    @Id
    val id: UUID,
    val name: String,
    val color: String
)


@Repository
interface ProjectCacheRepository: MongoRepository<ProjectEntity, UUID>
