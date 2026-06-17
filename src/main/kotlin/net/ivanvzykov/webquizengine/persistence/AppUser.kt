package net.ivanvzykov.webquizengine.persistence

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class AppUser(
    @Id @GeneratedValue
    var id: Int = 0,
    var username: String = "",
    var password: String = "",
)