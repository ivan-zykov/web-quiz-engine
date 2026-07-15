package net.ivanvzykov.webquizengine.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class AppUserEntity(
    @Id
    @GeneratedValue
    @Column(name = "id")
    var id: Int = 0,

    @Column(name = "username", unique = true)
    var username: String = "",

    @Column(name = "password")
    var password: String = "",
)