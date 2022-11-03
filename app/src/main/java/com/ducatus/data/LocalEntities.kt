package com.ducatus.data

data class LocalEntities(
    var goals: Goals = Goals(),
    var listOfGoals: List<Goals> = mutableListOf()
)