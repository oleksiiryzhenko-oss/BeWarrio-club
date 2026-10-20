package com.katadojo.app.data.model

enum class ExperienceLevel(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced")
}

enum class SkillLevel(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced"), ALL_LEVELS("All Levels");

    fun includes(level: ExperienceLevel): Boolean = when (this) {
        ALL_LEVELS -> true
        BEGINNER -> level == ExperienceLevel.BEGINNER
        INTERMEDIATE -> level == ExperienceLevel.INTERMEDIATE
        ADVANCED -> level == ExperienceLevel.ADVANCED
    }
}

enum class KarateStyle(val label: String) {
    SHOTOKAN("Shotokan"), KYOKUSHIN("Kyokushin"), GOJU_RYU("Goju-ryu"), WADO_RYU("Wado-ryu"),
    SHITO_RYU("Shito-ryu"), MIXED("Mixed styles")
}

/** Broad groups used by the schedule filters. */
enum class TrainingCategory { KATA, KUMITE, TECHNIQUE, CONDITIONING, OPEN }

enum class TrainingType(val label: String, val category: TrainingCategory) {
    FUNDAMENTALS("Fundamentals", TrainingCategory.TECHNIQUE),
    KATA("Kata", TrainingCategory.KATA),
    KUMITE("Kumite", TrainingCategory.KUMITE),
    COMPETITION("Competition", TrainingCategory.KUMITE),
    FOOTWORK("Footwork & Stance", TrainingCategory.TECHNIQUE),
    CONDITIONING("Conditioning", TrainingCategory.CONDITIONING),
    OPEN_DOJO("Open Dojo", TrainingCategory.OPEN),
    SEMINAR("Seminar", TrainingCategory.KATA)
}

/** Filters of the weekly schedule: training categories plus skill levels. */
enum class ScheduleFilter(val label: String) {
    ALL("All"), KATA("Kata"), KUMITE("Kumite"), TECHNIQUE("Technique"), CONDITIONING("Conditioning"),
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced");

    fun matches(training: KarateTraining): Boolean = when (this) {
        ALL -> true
        KATA -> training.type.category == TrainingCategory.KATA
        KUMITE -> training.type.category == TrainingCategory.KUMITE
        TECHNIQUE -> training.type.category == TrainingCategory.TECHNIQUE
        CONDITIONING -> training.type.category == TrainingCategory.CONDITIONING
        BEGINNER -> training.level.includes(ExperienceLevel.BEGINNER)
        INTERMEDIATE -> training.level.includes(ExperienceLevel.INTERMEDIATE)
        ADVANCED -> training.level.includes(ExperienceLevel.ADVANCED)
    }
}

/** The belt a user actually holds in their dojo (profile information only). */
enum class Belt(val label: String, val color: Long) {
    WHITE("White", 0xFFF5F1EA), YELLOW("Yellow", 0xFFE8C547), ORANGE("Orange", 0xFFE08A3C),
    GREEN("Green", 0xFF4E9A5B), BLUE("Blue", 0xFF3E6FB0), PURPLE("Purple", 0xFF7B5AA6),
    BROWN("Brown", 0xFF7A5234), BLACK("Black", 0xFF101010)
}

enum class WorkoutType(val label: String) {
    TECHNIQUE("Technique"), KATA("Kata"), KUMITE("Kumite"), CONDITIONING("Conditioning"),
    FOOTWORK("Footwork"), FLEXIBILITY("Flexibility"), MIXED("Mixed")
}

/** Result the user gives a kata section in Kata Coach. */
enum class SectionStatus(val label: String, val rating: Int) {
    COMPLETED("Completed", 10), NEEDS_PRACTICE("Needs Practice", 6), DIFFICULT("Difficult", 3)
}

enum class PerceivedDifficulty(val label: String) { EASY("Easy"), MODERATE("Moderate"), DIFFICULT("Difficult") }

enum class KataDifficulty(val label: String) { BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced") }

inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    if (name == null) null else enumValues<T>().firstOrNull { it.name == name }

/** Workout type suggested when starting a workout from a dojo training. */
val TrainingType.workoutType: WorkoutType
    get() = when (this) {
        TrainingType.KATA, TrainingType.SEMINAR -> WorkoutType.KATA
        TrainingType.KUMITE, TrainingType.COMPETITION -> WorkoutType.KUMITE
        TrainingType.FOOTWORK -> WorkoutType.FOOTWORK
        TrainingType.CONDITIONING -> WorkoutType.CONDITIONING
        TrainingType.FUNDAMENTALS -> WorkoutType.TECHNIQUE
        TrainingType.OPEN_DOJO -> WorkoutType.MIXED
    }
