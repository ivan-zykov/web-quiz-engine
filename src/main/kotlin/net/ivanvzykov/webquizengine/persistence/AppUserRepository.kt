package net.ivanvzykov.webquizengine.persistence

import org.springframework.data.repository.CrudRepository

interface AppUserRepository : CrudRepository<AppUserEntity, Int> {
    fun findByUsername(username: String): AppUserEntity?
}
