package net.ivanvzykov.webquizengine

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock {
        val clockInCetZone = Clock.system(ZoneId.of("Europe/Paris"))
        checkNotNull(clockInCetZone) { "Failed to instantiate clock for CET" }
        return clockInCetZone
    }
}
