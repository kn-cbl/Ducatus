package com.ducatus.data

data class Challenges(
    var key: String = "",
    var challengeName: String = "",
    var amount: Double = 0.0,
    var earned: Double = 0.0,
    var remaining: Double = 0.0,
    var isFinished: Boolean = false,
    var startDatePaid: String = "",
    var countMatch: Int = 0,
    var values: Array<Int> = arrayOf<Int>(),
    var availedChallengeMap: HashMap<Int, Int> = HashMap<Int, Int>()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Challenges

        if (key != other.key) return false
        if (challengeName != other.challengeName) return false
        if (amount != other.amount) return false
        if (earned != other.earned) return false
        if (remaining != other.remaining) return false
        if (!values.contentEquals(other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + challengeName.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + earned.hashCode()
        result = 31 * result + remaining.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}
