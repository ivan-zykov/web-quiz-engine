package net.ivanvzykov.webquizengine

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    @Order(1)
    fun apiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/api/**")
            authorizeHttpRequests {
                authorize(HttpMethod.POST, "/api/register", permitAll)
                authorize("/api/**", authenticated)
            }
            httpBasic { }
            csrf { disable() }
        }

        return http.build()
    }

    @Bean
    @Order(2)
    fun h2ConsoleFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher(PathRequest.toH2Console())
            authorizeHttpRequests {
                authorize(anyRequest, permitAll)
            }
            csrf { disable() }
            headers {
                frameOptions {
                    sameOrigin = true
                }
            }
        }

        return http.build()
    }

    @Bean
    fun otherSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize(HttpMethod.POST, "/actuator/shutdown", permitAll) // Required by course task
                authorize(anyRequest, denyAll)
            }
            httpBasic { }
            csrf { disable() }
        }

        return http.build()
    }
}
