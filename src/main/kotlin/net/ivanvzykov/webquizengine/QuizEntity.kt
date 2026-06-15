package net.ivanvzykov.webquizengine

import jakarta.persistence.*
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

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

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @Column(name = "options")
    var options: List<String>? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "answers")
    var answers: List<Int>? = null

    @ManyToOne
    @JoinColumn(name = "author")
    var author: AppUser? = null
}
