package ru.quipy.service

import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestParam
import ru.quipy.api.aggregate.UserAggregate
import ru.quipy.core.EventSourcingService
import ru.quipy.logic.state.UserAggregateState
import ru.quipy.projections.UserAccount
import ru.quipy.projections.UserAccountCacheRepository

import java.util.*

@Service
class UserService (
    val userAccountCacheRepository: UserAccountCacheRepository
    ) {

    fun getAllUsers(): MutableList<UserAccount> {
        return userAccountCacheRepository.findAll()
    }

    fun getAllUsersName(): MutableSet<String> {
        val users = userAccountCacheRepository.findAll()
        val usersNameSet = mutableSetOf<String>()
        users.forEach{
            usersNameSet.add(it?.userName.toString())
        }
        return usersNameSet
    }

    fun getUserProjects(userId: UUID): MutableSet<UUID> {
        return userAccountCacheRepository.findById(userId).get().projects
    }

    fun getUser(userId: UUID): UserAccount {
        return userAccountCacheRepository.findById(userId).get()
    }

    fun checkUserName(name: String): String {
        val usersNames = this.getAllUsersName().lowerCase()
        if (usersNames.contains(name.lowercase()))
            return "User with this name already exist"
        return "This name not used"
    }

    fun findUser(name: String): MutableSet<UserAccount> {
        val usersAll = userAccountCacheRepository.findAll()
        val usersFound = mutableSetOf<UserAccount>()
        val usersIdSet = mutableSetOf<UUID>()
        usersAll.forEach{
            if (it.userName?.contains(name, true) == true)
                usersIdSet.add(it.userId)
        }

        usersIdSet.forEach{
            usersFound.add(this.getUser(it))
        }

        return usersFound
    }
}

fun MutableSet<String>.lowerCase(): List<String> = this.map { it.lowercase() }
