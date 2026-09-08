package com.example.data.model

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिंदी (Hindi)"),
    HINGLISH("hinglish", "Hinglish (Mix)")
}

enum class ExamType(
    val id: String,
    val title: String,
    val category: String,
    val totalTimeMinutes: Int,
    val totalQuestions: Int,
    val marksPerCorrect: Double,
    val negativeMark: Double,
    val description: String
) {
    SSC_CGL(
        id = "ssc_cgl",
        title = "SSC CGL (Tier-1)",
        category = "Staff Selection Commission",
        totalTimeMinutes = 60,
        totalQuestions = 100,
        marksPerCorrect = 2.0,
        negativeMark = 0.50,
        description = "Quantitative Aptitude, General Intelligence & Reasoning, English Comprehension, General Awareness."
    ),
    SSC_CHSL(
        id = "ssc_chsl",
        title = "SSC CHSL (Tier-1)",
        category = "Staff Selection Commission",
        totalTimeMinutes = 60,
        totalQuestions = 100,
        marksPerCorrect = 2.0,
        negativeMark = 0.50,
        description = "Combined Higher Secondary Level: Quantitative, Reasoning, English, General Awareness."
    ),
    RAILWAY_NTPC(
        id = "railway_ntpc",
        title = "Railway RRB NTPC",
        category = "Railway Recruitment Board",
        totalTimeMinutes = 90,
        totalQuestions = 100,
        marksPerCorrect = 1.0,
        negativeMark = 0.33,
        description = "General Awareness, Mathematics, General Intelligence and Reasoning."
    ),
    BANKING_PO(
        id = "banking_po",
        title = "Banking IBPS / SBI PO",
        category = "Banking & Financial Exams",
        totalTimeMinutes = 60,
        totalQuestions = 100,
        marksPerCorrect = 1.0,
        negativeMark = 0.25,
        description = "Quantitative Aptitude, Reasoning Ability, English Language with sectional timings."
    )
}

enum class DifficultyLevel(val level: Int, val title: String, val description: String) {
    INTUITION(1, "1. Intuition", "Extremely simple zero-assumptions visual/conceptual intuition"),
    FOUNDATION(2, "2. Foundation", "Core definitions, simple numbers, and direct relationships"),
    BASIC(3, "3. Basic", "Standard single-step textbook problem"),
    BASIC_VARIATION(4, "4. Basic Variation", "Slight wording shift or reversed givens"),
    MODERATE(5, "5. Moderate", "Multi-step reasoning with standard competitive framing"),
    HARD(6, "6. Hard", "Multi-concept synthesis, non-obvious traps"),
    ADVANCED(7, "7. Advanced", "Unfamiliar framing, high cognitive demand, elimination methods"),
    TIMED_EXAM_CHALLENGE(8, "8. Timed Exam Challenge", "Speed-critical real competitive tier problem")
}

enum class QuestionFamily(val title: String) {
    DIRECT("Direct Application"),
    REVERSE("Reverse Problem"),
    MISSING_VALUE("Missing Value"),
    COMPARISON("Comparison"),
    STATEMENT("Statement / Conceptual"),
    DATA_INTERPRETATION("Data Interpretation / Graph"),
    MULTI_STEP("Multi-Step Reasoning"),
    MULTI_CONCEPT("Multi-Concept Synthesis"),
    UNFAMILIAR_FRAMING("Unfamiliar Framing"),
    TRAP("Trap / Distractor"),
    ELIMINATION("Option Elimination"),
    APPROXIMATION("Approximation / Fast Calc"),
    TRANSFER("Transfer Across Domains"),
    ERROR_REPAIR("Error Repair"),
    TIMED("Speed / Timed Sprint")
}

enum class MistakeCategory(
    val code: String,
    val title: String,
    val description: String
) {
    CONCEPTUAL("conceptual", "Conceptual Gap", "Misunderstood underlying scientific/mathematical principle"),
    FORMULA("formula", "Formula Recall Error", "Applied incorrect or incomplete formula"),
    ARITHMETIC("arithmetic", "Calculation / Arithmetic Slip", "Basic computation, addition, multiplication, or division mistake"),
    READING("reading", "Misread Question", "Missed a critical word like 'not', 'except', 'ratio of', etc."),
    UNIT("unit", "Unit Conversion Error", "Mismatched units (e.g. km/h vs m/s, minutes vs hours)"),
    SIGN("sign", "Sign / Polarity Error", "Inverted positive/negative sign in algebra or profit/loss"),
    LOGICAL("logical", "Logical Fallacy", "Invalid deduction or unwarranted step"),
    ASSUMPTION("assumption", "False Assumption", "Assumed unstated conditions (e.g. assuming integer or equal parts)"),
    STEP_ORDER("step_order", "Step-Order Error", "Violated BODMAS / PEMDAS or sequential step requirements"),
    CARELESS("careless", "Careless Slip", "Marked wrong option or slipped on simple copy of number"),
    TIME_PRESSURE("time_pressure", "Time-Pressure Rushing", "Rushed through question due to running timer"),
    MISINTERPRETATION("misinterpretation", "Misinterpretation", "Interpreted the context or target incorrectly"),
    PATTERN_RECOGNITION("pattern_recognition", "Pattern Recognition Failure", "Failed to spot recognizable algebraic or reasoning pattern")
}

enum class PracticeMode(val title: String, val subtitle: String) {
    LEARN("Learn & Build", "Intuition to foundation with Socratic guidance"),
    GUIDED("Guided Practice", "Solve step-by-step with AI tutor checkpoints"),
    INDEPENDENT("Independent Practice", "Test your standalone problem solving"),
    ACCURACY("Accuracy Focus", "No timer, strict penalization of careless slips"),
    SPEED("Speed Drill", "Fast-paced target time with quick shortcuts"),
    MIXED("Mixed Concepts", "Randomized multi-topic competitive barrage"),
    WEAKNESS("Weakness Repair", "Targeted questions addressing diagnosed errors"),
    REVISION("Spaced Revision", "Retain older concepts scheduled for recall"),
    PYQ("Verified PYQs", "Actual previous years exam questions with citations"),
    MOCK("Full Mock Simulation", "Timed section rules, palette, and negative marking")
}

enum class ConceptStatus {
    LOCKED,
    PREREQUISITE_GAP,
    LEARNING,
    PRACTICING,
    WEAK,
    REVISION_DUE,
    MASTERED
}

enum class SourceType(val displayName: String) {
    VERIFIED_PYQ("Verified PYQ"),
    USER_UPLOAD("User Upload / Scan"),
    AI_GENERATED("AI Generated (Validated)"),
    AI_EXPLANATION("AI Tutor Explanation"),
    UNCERTAIN("Uncertain OCR / Verify")
}
