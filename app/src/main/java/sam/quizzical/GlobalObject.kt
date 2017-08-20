package sam.quizzical

object GlobalObject {
    var questions = mutableListOf<Question>()
    var saveQuestions = false

    fun ClearQuestions(){
        questions = mutableListOf<Question>()
    }

    fun SetDefaultQuestions(){
        questions = mutableListOf<Question>(
                Question("Which is the island famous for it's 12 metre tall carved stone heads?", "Christmas Island", "Tuvalu", "Easter Island","The Solomon Islands","Penguin Island","Luzon",3),
                Question("Adjusted for inflation, which is the highest grossing movie of all time?","Gone with the Wind","Star Wars","The Exorcist","Titanic","Jaws","Snow White and the Seven Dwarfs",1),
                Question("In which country/region did writing first emerge?", "China", "Mesopotamia", "Egypt", "Pakistan", "Babylonia","Central Africa",5),
                Question("What is the smallest particle known to man?", "Proton", "Quark", "Neutrino", "Photon", "Atom","Molecule",3),
                Question("Who is the worlds richest person?", "Warren Buffet", "Jeff Bezos", "Bill Gates", "Mark Zuckerberg", "Michael Bloomberg","Me",3),
                Question("Which is the official language of Switzerland?", "German", "Romansh", "Italian", "French", "All of these","None of these", 5),
                Question("Which of the following countries features a firearm on their national flag?", "Venezuela", "North Korea", "Vietnam", "Lichtenstein", "Mozambique","Guam",5),
                Question("What is a String in programming?", "A sequence of numbers", "A sequence of characters", "A list of functions", "A list of variables", "A blob","A cloud",2),
                Question("Who made this app?", "Sam Oen", "A Wildling", "Four moose", "Romansh", "Jeff Bezos","Huh?",1)
        )
    }


}