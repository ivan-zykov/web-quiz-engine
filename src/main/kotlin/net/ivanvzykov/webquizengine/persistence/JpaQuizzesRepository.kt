package net.ivanvzykov.webquizengine.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface JpaQuizzesRepository : JpaRepository<QuizEntity, Long>
