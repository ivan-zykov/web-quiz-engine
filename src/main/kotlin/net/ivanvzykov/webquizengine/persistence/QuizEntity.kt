package net.ivanvzykov.webquizengine.persistence

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "quizzes")
class QuizEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @Column(name = "title")
    var title: String? = null

    @Column(name = "text")
    var text: String? = null

    @ElementCollection
    @CollectionTable(
        name = "quiz_options",
        joinColumns = [JoinColumn(name = "quiz_id")]
    )
    @Column(name = "option")
    var options: List<String>? = null

    @ElementCollection
    @CollectionTable(
        name = "quiz_answers",
        joinColumns = [JoinColumn(name = "quiz_id")]
    )
    @Column(name = "answer")
    var answers: List<Int>? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    var author: AppUserEntity? = null
}
