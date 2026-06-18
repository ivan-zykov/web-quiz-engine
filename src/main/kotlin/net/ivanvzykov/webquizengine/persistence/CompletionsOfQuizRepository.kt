package net.ivanvzykov.webquizengine.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CompletionsOfQuizRepository : JpaRepository<CompletionOfQuizEntity, Long> {
    fun findByQuiz(quiz: QuizEntity): List<CompletionOfQuizEntity>
    fun findByQuiz(quiz: QuizEntity, pageable: Pageable): Page<CompletionOfQuizEntity>
    fun findByUserOrderByCompletedAtDescIdAsc(user: AppUserEntity, pageable: Pageable): Page<CompletionOfQuizEntity>
}
