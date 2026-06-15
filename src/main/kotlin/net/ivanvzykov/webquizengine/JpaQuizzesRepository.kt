package net.ivanvzykov.webquizengine

import org.springframework.data.jpa.repository.JpaRepository

interface JpaQuizzesRepository : JpaRepository<QuizEntity, Long>
