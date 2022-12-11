package com.ducatus.data

class LocalEntities() {
    lateinit var goals: Goals
    lateinit var goalHistory: GoalHistory
    lateinit var listOfGoals: List<Goals>
    lateinit var challengeHistory: ChallengeHistory

    fun newInstance(g: Goals, gh: GoalHistory, list: List<Goals>, ch:ChallengeHistory) {
        this.goals = g
        this.goalHistory = gh
        this.listOfGoals = list
        this.challengeHistory = ch
    }
}