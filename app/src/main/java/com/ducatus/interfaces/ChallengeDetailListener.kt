package com.ducatus.interfaces

interface ChallengeDetailListener {
    fun onTextListener(position: Int)
    fun onClickFinishedChallenge(position: Int) {
        /**
         * Default implementation
         */
    }

    fun onRestartChallenge(position:Int){
        /**
         * Default Implementation
         */
    }
}