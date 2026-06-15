package net.ivanvzykov.webquizengine

import jakarta.persistence.*
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
    var user: AppUser? = null

    @Column(name = "completedAt")
    var completedAt: LocalDateTime? = null
}
