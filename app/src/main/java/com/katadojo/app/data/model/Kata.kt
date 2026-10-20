package com.katadojo.app.data.model

data class KataSection(val index: Int, val title: String, val cue: String) {
    val number: Int get() = index + 1
}

data class Kata(
    val id: String,
    val name: String,
    val difficulty: KataDifficulty,
    val durationText: String,
    val description: String,
    val styleNote: String,
    val sections: List<KataSection>
) {
    val sectionCount: Int get() = sections.size
}

/**
 * Built-in kata library. Entries are practice guides split into training sections; names,
 * sequences and availability differ between karate styles and organizations.
 */
object KataLibrary {
    const val STYLE_DISCLAIMER =
        "Kata names, sequences and availability differ between karate styles. Follow your instructor's version."

    private fun sections(vararg items: Pair<String, String>) =
        items.mapIndexed { i, (title, cue) -> KataSection(i, title, cue) }

    val all: List<Kata> = listOf(
        Kata(
            "taikyoku_shodan", "Taikyoku Shodan", KataDifficulty.BEGINNER, "1-2 min",
            "A foundational kata built on front stance, lower block and lunge punch along a simple floor pattern.",
            "Common in Shotokan and Kyokushin", sections(
                "Opening & first turn" to "Bow, ready stance, then turn left with a clean lower block.",
                "First line" to "Step forward with steady front stances and full hip rotation on each punch.",
                "Turns & middle line" to "Keep your head level through each turn; do not rush the block.",
                "Final line & closing" to "Finish with control, return to ready stance and hold your focus."
            )
        ),
        Kata(
            "heian_shodan", "Heian Shodan", KataDifficulty.BEGINNER, "2-3 min",
            "The first Heian kata introduces direction changes, blocks and stepping punches in an H-shaped pattern.",
            "Shotokan; known as Pinan Nidan in some styles", sections(
                "Opening Stance & First Sequence" to "Start from ready stance. Turn with a strong lower block and keep hips square.",
                "Second sequence" to "Drive forward in front stance; each punch lands as the stance settles.",
                "Direction change" to "Pivot on the ball of the foot and keep your balance through the turn.",
                "Middle sequence" to "Rising blocks with a steady rhythm; finish the last one with a kiai.",
                "Final sequence" to "Knife-hand blocks in back stance, then return to the starting point."
            )
        ),
        Kata(
            "heian_nidan", "Heian Nidan", KataDifficulty.BEGINNER, "2-3 min",
            "Adds back stance, knife-hand blocks and a front kick to the Heian foundations.",
            "Shotokan; known as Pinan Shodan in some styles", sections(
                "Opening double block" to "Raise both arms together; keep elbows relaxed and shoulders down.",
                "Side sequence" to "Transition smoothly between blocks without lifting your head.",
                "Kick & back fist" to "Chamber the knee high before the kick; land in a stable stance.",
                "Knife-hand line" to "Back stance with weight on the rear leg; hands move with the hips.",
                "Closing sequence" to "Finish sharp and return to ready stance with calm breathing."
            )
        ),
        Kata(
            "heian_sandan", "Heian Sandan", KataDifficulty.BEGINNER, "2-3 min",
            "Close-range techniques with elbow strikes, double blocks and a horse-riding stance.",
            "Shotokan and related styles", sections(
                "Opening blocks" to "Alternate blocks in one smooth rhythm without pausing between arms.",
                "Double blocks" to "Keep both arms moving together; tighten the core at the end.",
                "Elbow sequence" to "Stamp and strike in horse stance; keep your back upright.",
                "Final turn & closing" to "Turn with control and finish with both fists at the hips."
            )
        ),
        Kata(
            "heian_yondan", "Heian Yondan", KataDifficulty.INTERMEDIATE, "3 min",
            "Develops balance with a crane stance, side kicks and elbow strikes into the palm.",
            "Shotokan; Pinan Yondan in other styles", sections(
                "Slow opening" to "Move slowly with full tension; breathing matches the movement.",
                "Side kick & elbow" to "Balance on the standing leg before the kick; strike through the target.",
                "Cross blocks" to "Keep stances deep while the arms cross in front of the body.",
                "Knee strike" to "Pull the hands down as the knee rises; land softly.",
                "Closing sequence" to "Finish with sharp knife-hand blocks and a steady return."
            )
        ),
        Kata(
            "heian_godan", "Heian Godan", KataDifficulty.INTERMEDIATE, "3 min",
            "Includes a jump, cross blocks and stance transitions that demand timing and control.",
            "Shotokan; Pinan Godan in other styles", sections(
                "Opening sequence" to "Short, precise blocks; do not over-extend the arms.",
                "Cross blocks" to "Lead with the hips and keep the arms close to the centre line.",
                "Jump & landing" to "Jump with both feet together and land low and balanced.",
                "Stance transitions" to "Move through the stances without bouncing.",
                "Final sequence" to "Finish with a strong, clean closing and hold stillness."
            )
        ),
        Kata(
            "tekki_shodan", "Tekki Shodan", KataDifficulty.INTERMEDIATE, "2 min",
            "Performed entirely in horse-riding stance along a straight line with close-range techniques.",
            "Shotokan; Naihanchi in other styles", sections(
                "Cross step & first strike" to "Cross step without lifting the body; sink into horse stance.",
                "Middle combinations" to "Hips stay low and still; power comes from the torso.",
                "Wave kick & block" to "Lift the foot as the arm blocks; stay square to the front.",
                "Closing mirror" to "Repeat to the other side with equal timing and finish sharp."
            )
        ),
        Kata(
            "bassai_dai", "Bassai Dai", KataDifficulty.INTERMEDIATE, "3-4 min",
            "A powerful kata with quick changes of direction and strong hip rotation.",
            "Shotokan; Passai in other styles", sections(
                "Opening charge" to "Drive forward from the rear foot; land solid.",
                "Hip rotation blocks" to "Turn the hips fully with every block.",
                "Scooping blocks" to "Keep the stance low and the blocks connected.",
                "Kicks & strikes" to "Kick with a high knee and recover your balance quickly.",
                "Closing sequence" to "Slow, deliberate final moves with full focus."
            )
        ),
        Kata(
            "kanku_dai", "Kanku Dai", KataDifficulty.ADVANCED, "4-5 min",
            "A long kata with a characteristic opening, many direction changes, jumps and ground work.",
            "Shotokan; Kushanku in other styles", sections(
                "Opening & sky view" to "Raise the hands slowly and follow them with your eyes.",
                "First combinations" to "Link blocks and strikes into smooth combinations.",
                "Kicks & back fists" to "Stay balanced on one leg and keep the guard up.",
                "Ground sequence" to "Drop low with control and rise without using the hands.",
                "Jump & spin" to "Commit fully to the jump and land ready.",
                "Closing sequence" to "Mirror the opening calmly and finish in stillness."
            )
        ),
        Kata(
            "jion", "Jion", KataDifficulty.ADVANCED, "3-4 min",
            "A strong, grounded kata with front stances, powerful blocks and a steady rhythm.",
            "Shotokan and related styles", sections(
                "Opening & first line" to "Strong front stances with blocks landing on the step.",
                "Side lines" to "Keep the same rhythm to both sides.",
                "Rising blocks" to "Rise with the block while the stance stays deep.",
                "Horse stance sequence" to "Sink the hips and strike from a stable base.",
                "Closing sequence" to "End with full control and a clear kiai."
            )
        ),
        Kata(
            "empi", "Empi", KataDifficulty.ADVANCED, "3 min",
            "A fast kata with rising and falling movement, likened to a swallow in flight.",
            "Shotokan; Wanshu in other styles", sections(
                "Low opening" to "Start low and explode upward on the first technique.",
                "Fast combinations" to "Speed without losing the shape of each stance.",
                "Rising & falling" to "Change levels quickly while keeping the head controlled.",
                "Jump & turn" to "Rotate in the air and land in a stable stance.",
                "Closing sequence" to "Finish quick and sharp, then hold stillness."
            )
        ),
        Kata(
            "hangetsu", "Hangetsu", KataDifficulty.ADVANCED, "3-4 min",
            "Slow, tense movements in half-moon stance with deep breathing and circular stepping.",
            "Shotokan; Seisan in other styles", sections(
                "Half-moon stepping" to "Circular steps with the knees pressed inward.",
                "Slow tension blocks" to "Full muscle tension coordinated with exhaling.",
                "Direction changes" to "Turn smoothly and settle into the stance before moving.",
                "Kicks & closing" to "Kick from a stable base and finish with steady breathing."
            )
        )
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String?): Kata? = id?.let { byId[it] }

    /** Rotates through the beginner and intermediate kata, one per calendar week. */
    fun kataOfWeek(weekIndex: Long): Kata {
        val pool = all.filter { it.difficulty != KataDifficulty.ADVANCED }
        return pool[(Math.floorMod(weekIndex, pool.size.toLong())).toInt()]
    }
}
