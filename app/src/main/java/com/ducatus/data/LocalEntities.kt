package com.ducatus.data

class LocalEntities() {
    lateinit var goals: Goals
    lateinit var goalHistory: GoalHistory
    lateinit var listOfGoals: List<Goals>

    fun newInstance(g: Goals, gh: GoalHistory, list: List<Goals>) {
        this.goals = g
        this.goalHistory = gh
        this.listOfGoals = list
    }
}