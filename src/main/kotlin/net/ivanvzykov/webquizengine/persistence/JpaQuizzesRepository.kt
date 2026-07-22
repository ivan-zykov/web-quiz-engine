package net.ivanvzykov.webquizengine.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface JpaQuizzesRepository : JpaRepository<QuizEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
            select q
            from QuizEntity q
            where q.id = :id
            """
    )
    fun findByIdForUpdate(id: Long): Optional<QuizEntity>

    @Query(
        """
        select quiz
        from QuizEntity quiz
        left join fetch quiz.options
        where quiz.id = :id
        """
    )
    fun findWithOptionsBy(id: Long): Optional<QuizEntity>
}
