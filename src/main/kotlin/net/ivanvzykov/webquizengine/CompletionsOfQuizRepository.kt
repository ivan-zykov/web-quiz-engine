package net.ivanvzykov.webquizengine

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CompletionsOfQuizRepository : JpaRepository<CompletionOfQuizEntity, Long> {
    fun findByQuiz(quiz: QuizEntity): List<CompletionOfQuizEntity>
    fun findByQuiz(quiz: QuizEntity, pageable: Pageable): Page<CompletionOfQuizEntity>
    fun findByUserOrderByCompletedAtDescIdAsc(user: AppUser, pageable: Pageable): Page<CompletionOfQuizEntity>
}
