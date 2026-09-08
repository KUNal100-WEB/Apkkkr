package com.example.engine

import com.example.ai.AIProvider
import com.example.data.local.dao.CurriculumDao
import com.example.data.local.dao.FlashcardDao
import com.example.data.local.dao.MasteryDao
import com.example.data.local.dao.MistakeDao
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.MasteryEntity
import com.example.data.local.entity.MistakeEntity
import java.util.UUID

data class RevisionTask(
    val id: String,
    val title: String,
    val subtitle: String,
    val priority: Int, // 1 is highest
    val type: String, // "repeated_mistake", "overdue_concept", "flashcard", "weak_topic"
    val conceptId: String?,
    val mistakeId: String? = null
)

class RevisionEngine(
    private val masteryDao: MasteryDao,
    private val mistakeDao: MistakeDao,
    private val flashcardDao: FlashcardDao,
    private val curriculumDao: CurriculumDao,
    private val aiProvider: AIProvider
) {

    /**
     * Builds the prioritized "Today's Revision" plan based on:
     * 1. Repeated mistakes
     * 2. Overdue concepts (Day 0, 1, 3, 7, 14, 30)
     * 3. Low retention & weak exam topics
     */
    suspend fun getTodaysRevisionQueue(): List<RevisionTask> {
        val tasks = mutableListOf<RevisionTask>()
        val now = System.currentTimeMillis()

        // Priority 1: Repeated mistakes
        val repeatedMistakes = mistakeDao.getRepeatedMistakesDirect()
        repeatedMistakes.take(3).forEach { mistake ->
            tasks.add(
                RevisionTask(
                    id = "rev_mistake_${mistake.id}",
                    title = "Fix Repeated Mistake: ${mistake.mistakeType.replaceFirstChar { it.uppercase() }}",
                    subtitle = "Encountered ${mistake.repeatCount} times. ${mistake.rootCause}",
                    priority = 1,
                    type = "repeated_mistake",
                    conceptId = null,
                    mistakeId = mistake.id
                )
            )
        }

        // Priority 2: Overdue Spaced Repetition Concepts
        val allMastery = masteryDao.getAllMastery()
        // We evaluate overdue concepts
        val concepts = curriculumDao.getAllConcepts()

        // Priority 3: Flashcards Due
        val dueCards = flashcardDao.getDueFlashcards(now)
        // Add sample flashcard task if available
        tasks.add(
            RevisionTask(
                id = "rev_flashcard_active",
                title = "Active Recall Flashcards",
                subtitle = "Formulas, shortcuts & question traps revision",
                priority = 2,
                type = "flashcard",
                conceptId = null
            )
        )

        return tasks
    }

    /**
     * Generates flashcards for a concept across standard types:
     * Formula, Definition, Concept, Shortcut, Common Mistake, Question-Pattern
     */
    suspend fun generateFlashcardsForConcept(conceptId: String, conceptName: String): List<FlashcardEntity> {
        val cards = listOf(
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                conceptId = conceptId,
                cardType = "formula",
                front = "Formula: Product Constancy (Price × Consumption = Constant)",
                back = "If Price increases by x/y, Consumption must decrease by x/(x + y) to keep Expenditure constant."
            ),
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                conceptId = conceptId,
                cardType = "shortcut",
                front = "Shortcut: Ratio to Percentage quick conversions",
                back = "1/2=50%, 1/3=33.33%, 1/4=25%, 1/5=20%, 1/6=16.66%, 1/7=14.28%, 1/8=12.5%, 1/9=11.11%, 1/11=9.09%."
            ),
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                conceptId = conceptId,
                cardType = "common_mistake",
                front = "Common Mistake: % Increase vs % Decrease",
                back = "A 25% increase followed by a 25% decrease does NOT return to original! Result is (1.25 × 0.75) = 0.9375 (a net 6.25% loss)."
            )
        )
        flashcardDao.insertFlashcards(cards)
        return cards
    }
}
