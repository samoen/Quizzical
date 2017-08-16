package sam.quizzical

object GlobalObject {
    var questions = mutableListOf<Question>(
        Question("Who are Apps People?", "App Developers", "Godlike Beings", "Accountants", "Wizards", "Klingon Folk","MerFolk", 1),
        Question("Who made this app?", "John", "Steve", "Sam", "Emily", "Olaf","Meerman",3),
        Question("mouly?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("bouly?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("dopp?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("Wible?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("keple?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("Whoh?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("emak?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("Willapp?", "No", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",4),
        Question("verm?", "yeyeyeye", "In your dreams", "Maybe", "Fo Sho", "nehneh","huh?",1)
    )
    fun ClearQuestions(){
        questions = mutableListOf<Question>()
    }
}