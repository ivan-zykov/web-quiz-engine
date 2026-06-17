package net.ivanvzykov.webquizengine.application

import net.ivanvzykov.webquizengine.persistence.AppUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AppUserDetailsService(@Autowired private val repository: AppUserRepository) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        val user = repository.findByUsername(username)
            ?: throw UsernameNotFoundException("Username $username not found")

        return AppUserAdapter(user)
    }
}