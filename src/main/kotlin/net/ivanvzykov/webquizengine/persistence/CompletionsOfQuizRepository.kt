package net.ivanvzykov.webquizengine.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CompletionsOfQuizRepository : JpaRepository<CompletionOfQuizEntity, Long> {
    fun findByQuiz(quiz: QuizEntity, pageable: Pageable): Page<CompletionOfQuizEntity>
    fun findByUserOrderByCompletedAtDescIdAsc(user: AppUserEntity, pageable: Pageable): Page<CompletionOfQuizEntity>

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true,
    )
    @Query(
        """
            delete
            from CompletionOfQuizEntity completion
            where completion.quiz = :quiz
        """
    )
    fun deleteInBulkByQuiz(quiz: QuizEntity)
}
