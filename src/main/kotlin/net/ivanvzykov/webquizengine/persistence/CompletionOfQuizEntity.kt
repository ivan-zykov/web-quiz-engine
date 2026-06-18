package net.ivanvzykov.webquizengine.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "completionsOfQuizzes")
class CompletionOfQuizEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @ManyToOne
    @JoinColumn(name = "quizId")
    var quiz: QuizEntity? = null

    @ManyToOne
    @JoinColumn(name = "userCompleted")
    var user: AppUserEntity? = null

    @Column(name = "completedAt")
    var completedAt: LocalDateTime? = null
}