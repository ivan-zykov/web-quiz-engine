package net.ivanvzykov.webquizengine.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface AppUserRepository : JpaRepository<AppUserEntity, Int> {
    fun findByUsername(username: String): AppUserEntity?
}
