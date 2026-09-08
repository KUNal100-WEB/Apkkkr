package com.example.engine

import com.example.ai.AIProvider
import com.example.data.local.entity.ConceptEntity
import com.example.data.local.entity.QuestionEntity

data class ConceptBridgeData(
    val oldConceptName: String,
    val newConceptName: String,
    val relationshipExplanation: String,
    val tinyExample: String,
    val microQuestion: String,
    val microOptions: List<String>,
    val correctMicroAnswer: String,
    val bridgeFollowup: String
)

data class SolverStep(
    val stepIndex: Int,
    val stepTitle: String,
    val aiGuidance: String,
    val promptForStudent: String,
    val expectedAnswer: String,
    val completed: Boolean = false
)

data class HintLevel(
    val level: Int,
    val title: String,
    val content: String
)

class TutorEngine(
    private val aiProvider: AIProvider
) {

    /**
     * Generates a reusable Concept Bridge when transitioning from oldConcept to newConcept.
     */
    suspend fun generateConceptBridge(
        oldConcept: ConceptEntity,
        newConcept: ConceptEntity,
        language: String = "hinglish"
    ): ConceptBridgeData {
        val prompt = buildString {
            appendLine("Create an educational Concept Bridge for a competitive exam aspirant.")
            appendLine("Previous Concept: ${oldConcept.name} (${oldConcept.description})")
            appendLine("New Target Concept: ${newConcept.name} (${newConcept.description})")
            appendLine("Language: $language")
            appendLine("Bridge Requirements:")
            appendLine("1. Explain how ${oldConcept.name} directly connects and transitions into ${newConcept.name}.")
            appendLine("2. Provide a 1-sentence intuitive connection.")
            appendLine("3. Provide a tiny 1-line real world or numerical example.")
            appendLine("4. Formulate ONE simple micro-question with 4 choices to verify understanding.")
            appendLine("5. Provide the exact answer and a warm follow-up bridge line.")
        }

        val schema = """
        {
          "relationshipExplanation": "Clear connection between both concepts",
          "tinyExample": "Simple illustrative example",
          "microQuestion": "Micro verification question",
          "microOptions": ["A", "B", "C", "D"],
          "correctMicroAnswer": "A",
          "bridgeFollowup": "Great! Now let's see how this transforms into standard exam questions."
        }
        """.trimIndent()

        val result = aiProvider.generateStructuredOutput(
            prompt = prompt,
            schemaDescription = schema,
            parser = { raw ->
                val json = org.json.JSONObject(raw)
                val optArray = json.getJSONArray("microOptions")
                val opts = mutableListOf<String>()
                for (i in 0 until optArray.length()) opts.add(optArray.getString(i))

                ConceptBridgeData(
                    oldConceptName = oldConcept.name,
                    newConceptName = newConcept.name,
                    relationshipExplanation = json.getString("relationshipExplanation"),
                    tinyExample = json.getString("tinyExample"),
                    microQuestion = json.getString("microQuestion"),
                    microOptions = opts,
                    correctMicroAnswer = json.getString("correctMicroAnswer"),
                    bridgeFollowup = json.getString("bridgeFollowup")
                )
            },
            systemInstruction = "You are an empathetic, world-class Socratic exam tutor."
        )

        return result.getOrElse {
            // High-quality deterministic fallback bridge
            ConceptBridgeData(
                oldConceptName = oldConcept.name,
                newConceptName = newConcept.name,
                relationshipExplanation = "${oldConcept.name} aur ${newConcept.name} dono comparisons aur proportions ke hi roop hain. Agar ek basic relationship samajh aa gayi, toh doosra naturally flow hota hai.",
                tinyExample = "Jaise 1/4 ek fraction hai, 1 : 4 ek ratio hai, aur (1/4) × 100 = 25% wahi same quantity percentage mein hai!",
                microQuestion = "Agar ek ratio 1 : 2 hai, toh ye percentage mein kitna hoga?",
                microOptions = listOf("25%", "50%", "75%", "100%"),
                correctMicroAnswer = "50%",
                bridgeFollowup = "Shabash! Dekha? Sirf notation badla hai, maths wahi simple hai. Ab aage badhte hain!"
            )
        }
    }

    /**
     * Deconstructs a question into interactive "Solve With Me" steps.
     * AI does NOT dump the full solution at once.
     */
    suspend fun generateSolveWithMeSteps(
        question: QuestionEntity,
        language: String = "hinglish"
    ): List<SolverStep> {
        val prompt = buildString {
            appendLine("Break this exam problem down into 3 to 4 sequential Socratic steps for 'Solve With Me' mode:")
            appendLine("Question: ${question.questionText}")
            appendLine("Options: ${question.optionsJson}")
            appendLine("Solution: ${question.solution}")
            appendLine("Correct Answer: ${question.answer}")
            appendLine("Language: $language")
            appendLine("RULE: Never dump the full answer upfront. Each step must ask student for one specific mini-target (e.g. identify givens, calculate intermediate value, final answer).")
        }

        val schema = """
        [
          {
            "stepIndex": 1,
            "stepTitle": "Given & Target",
            "aiGuidance": "Pehle identify karo kya given hai aur kya nikalna hai.",
            "promptForStudent": "What is the initial quantity or base value?",
            "expectedAnswer": "..."
          }
        ]
        """.trimIndent()

        val result = aiProvider.generateStructuredOutput(
            prompt = prompt,
            schemaDescription = schema,
            parser = { raw ->
                val arr = org.json.JSONArray(raw)
                val list = mutableListOf<SolverStep>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    list.add(
                        SolverStep(
                            stepIndex = item.getInt("stepIndex"),
                            stepTitle = item.getString("stepTitle"),
                            aiGuidance = item.getString("aiGuidance"),
                            promptForStudent = item.getString("promptForStudent"),
                            expectedAnswer = item.getString("expectedAnswer")
                        )
                    )
                }
                list
            }
        )

        return result.getOrElse {
            listOf(
                SolverStep(
                    stepIndex = 1,
                    stepTitle = "Step 1: Identify Givens & Target",
                    aiGuidance = "Pehle question ko todte hain: Problem mein hume kya information di gayi hai aur goal kya hai?",
                    promptForStudent = "What is the key base value or given condition in this problem?",
                    expectedAnswer = "Base Value / Condition"
                ),
                SolverStep(
                    stepIndex = 2,
                    stepTitle = "Step 2: Choose Concept & Formula",
                    aiGuidance = "Ab hume sahi formula ya shortcut relationship apply karni hai.",
                    promptForStudent = "Which mathematical relationship connects the given terms?",
                    expectedAnswer = "Formula / Ratio Relation"
                ),
                SolverStep(
                    stepIndex = 3,
                    stepTitle = "Step 3: Execute & Conclude",
                    aiGuidance = "Ab final calculation execute karte hain!",
                    promptForStudent = "Based on our steps, what is the final calculated answer matching the options?",
                    expectedAnswer = question.answer
                )
            )
        }
    }

    /**
     * Builds the 6-Level Hint Ladder for any question:
     * H1: Direction
     * H2: Concept / Formula
     * H3: First Operation
     * H4: Guided Step
     * H5: Partial Solution
     * H6: Full Solution
     */
    fun getHintLadder(question: QuestionEntity): List<HintLevel> {
        return listOf(
            HintLevel(
                level = 1,
                title = "Hint 1: General Direction",
                content = "Focus on the given constraints. Notice whether the quantity increases or decreases relative to the original base value."
            ),
            HintLevel(
                level = 2,
                title = "Hint 2: Core Concept / Formula",
                content = "Recall the standard formula for this family: Product constancy or percentage change: % change = (Change / Base) × 100."
            ),
            HintLevel(
                level = 3,
                title = "Hint 3: First Operation",
                content = "Assume a convenient starting number (like 100 or x) or convert the given percentage into a fractional ratio (e.g. 25% = 1/4)."
            ),
            HintLevel(
                level = 4,
                title = "Hint 4: Guided Step",
                content = "If the term increases by 1/4 (factor 5/4), its counterpart must decrease to (4/5) to keep the product constant. What is 1 - 4/5?"
            ),
            HintLevel(
                level = 5,
                title = "Hint 5: Partial Solution",
                content = "The required reduction is 1/5 of the new value. Calculate (1/5) × 100."
            ),
            HintLevel(
                level = 6,
                title = "Hint 6: Complete Solution",
                content = question.solution
            )
        )
    }

    /**
     * Responds to Socratic 'Why' queries or natural voice inputs:
     * e.g. "Bhai ye step samajh nahi aaya" -> explains root causality, not just formula repetition.
     */
    suspend fun explainWhy(
        questionText: String,
        solutionContext: String,
        studentQuery: String,
        language: String = "hinglish"
    ): String {
        val prompt = buildString {
            appendLine("The student is asking a 'Why' / confusion question regarding this problem:")
            appendLine("Question: $questionText")
            appendLine("Current Solution context: $solutionContext")
            appendLine("Student says: \"$studentQuery\"")
            appendLine("Language: $language")
            appendLine("TUTOR INSTRUCTIONS:")
            appendLine("- Explain the underlying intuition and WHY this step works, NOT just 'because the formula says so'.")
            appendLine("- Use everyday physical reasoning or simple number analogies.")
            appendLine("- Keep it encouraging, concise (under 4 sentences), and crystal clear.")
        }

        val result = aiProvider.generateText(prompt, systemInstruction = "You are a warm, intuitive teacher explaining the deep 'Why' behind competitive exam tricks.")
        return result.getOrElse {
            "Is step ka main logic ye hai ki jab hum ek quantity ko badhate hain, toh total product ko same rakhne ke liye doosri quantity ko usi proportion mein kam karna padta hai. Think of it like a seesaw: agar price upar gaya, toh consumption naturally neeche aayega!"
        }
    }
}
