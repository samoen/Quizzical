package sam.quizzical

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.text.toInt
import ninja.sakib.pultusorm.core.PultusORM

class MainActivity : AppCompatActivity() {

    var currentQuestion = Question()
    var currentQuestionNumber:Int = -1
    var history = mutableListOf<Pair<Int,String>>()
    val appPath: String by lazy { this.applicationContext.getFilesDir().getAbsolutePath() }
    val pultusORM: PultusORM by lazy { PultusORM("quiz.db", appPath) }
    val TIME_LIMIT = "03:00"
    val ANIMATION_TIME:Long = 300
    var lock_ftb = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(GlobalObject.saveQuestions){
            SaveQuestions()
            GlobalObject.saveQuestions = false
        }
        if(CheckDBexist()){
            ReadDBquestions()
        }
        ReadDBscores()
        InitViews()
        RetryQuiz()
    }

    fun InitViews(){
        textView_correctAnswers.text = "0"
        button_edit.setOnClickListener {
            startActivity(Intent(this,EditActivity::class.java))
        }
        button_clear_history.setOnClickListener {
            pultusORM.drop(ScoreTime())
            pultusORM.drop(QuestionList())
            pultusORM.drop(DataExists())
            GlobalObject.dbexist = false
            this.recreate()
        }
        button_reset.setOnClickListener { RetryQuiz() }
        button_history.setOnClickListener { ShowHistory() }
        folding_tab_bar.onFoldingItemClickListener = object : FoldingTabBar.OnFoldingItemSelectedListener {
            override fun onFoldingItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.ftb_menu_head -> OptionClicked(1)
                    R.id.ftb_menu_shoulders -> OptionClicked(2)
                    R.id.ftb_menu_legs -> OptionClicked(3)
                    R.id.ftb_menu_offhand -> OptionClicked(4)
                    R.id.ftb_menu_mainhand -> OptionClicked(5)
                    R.id.ftb_menu_wait -> OptionClicked(6)
                }
                return false
            }
        }
        chronometer_time.onChronometerTickListener = object: OnChronometerTickListener{
            override fun onChronometerTick(chronometer: Chronometer?) {
                if(chronometer?.text == TIME_LIMIT){
                    lock_ftb = false
                    StopQuiz()
                    UpdateTextViews()
                }
            }
        }
    }

    fun NextQuestion(){
        if(currentQuestionNumber == GlobalObject.questions.size-1) {
            StopQuiz()
            UpdateTextViews()
        }else if(currentQuestionNumber+1 in 0..GlobalObject.questions.size-1) {
            currentQuestionNumber++
            currentQuestion = GlobalObject.questions.get(currentQuestionNumber)
            if (currentQuestion.question_text.isBlank()){
                StopQuiz()
                UpdateTextViews()
            }else{
                Handler().postDelayed({ if(this@MainActivity.applicationContext != null){ folding_tab_bar.expand() } },ANIMATION_TIME)
            }
        }
    }

    fun RetryQuiz(){
        chronometer_time.base = SystemClock.elapsedRealtime()
        chronometer_time.start()
        textView_correctAnswers.text = "0"
        currentQuestionNumber = -1
        NextQuestion()
        UpdateTextViews()
    }

    fun StopQuiz(){
        Handler().postDelayed({ if(this@MainActivity.applicationContext != null){
            if(!lock_ftb){
                folding_tab_bar.rollUp()
                lock_ftb = true
            }
        } },ANIMATION_TIME)
        chronometer_time.stop()
        currentQuestion = Question()
        currentQuestionNumber = -1
        history.add(Pair((textView_correctAnswers.text as String).toInt(),chronometer_time.text as String))
        SaveScoreTime()
    }

    fun UpdateTextViews(){
        if (currentQuestionNumber == -1){
            textview_question.text =  "Press Retry to Take the Quiz"
            textView_option1.text = ""
            textView_option2.text = ""
            textView_option3.text = ""
            textView_option4.text = ""
            textView_option5.text = ""
            textView_option6.text = ""
        }else{
            textview_question.text = currentQuestion.question_text
            textView_option1.text = "A: ${currentQuestion.option1}"
            textView_option2.text = "B: ${currentQuestion.option2}"
            textView_option3.text = "C: ${currentQuestion.option3}"
            textView_option4.text = "D: ${currentQuestion.option4}"
            textView_option5.text = "E: ${currentQuestion.option5}"
            textView_option6.text = "F: ${currentQuestion.option6}"
        }
    }

    fun OptionClicked(button_number: Int){
        if (button_number == currentQuestion.correct_option){
            textView_correctAnswers.text = (textView_correctAnswers.text.toString().toInt() + 1).toString()
            Toast.makeText(this,"Correct!",0).show()
        } else{
            Toast.makeText(this,"Incorrect",0).show()
        }
        NextQuestion()
        UpdateTextViews()
    }

    fun ShowHistory(){
        val simpleAlert = AlertDialog.Builder(this).create()
        simpleAlert.setTitle("Score History")
        val builder = StringBuilder()
        for (entry in history){
            builder.append("Points: ${entry.first} Time: ${entry.second}")
            builder.append("\n")
        }
        simpleAlert.setMessage(builder.toString())
        simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, "OK", {_,_ -> })
        simpleAlert.show()
    }
    fun SaveScoreTime(){
        val st = ScoreTime()
        st.score = (textView_correctAnswers.text as String).toInt()
        st.time = chronometer_time.text as String
        pultusORM.save(st)
    }
    fun ReadDBscores(){
        val scoretimes = pultusORM.find(ScoreTime())
        for (v in scoretimes) {
            val st = v as ScoreTime
            history.add(Pair(st.score,st.time))
        }
    }

    fun CheckDBexist():Boolean{
        val exists = pultusORM.find(DataExists())
        for (v in exists){
            val e = v as DataExists
            if (e.data_exists){
                return true
            }
        }
        return false
    }

    fun ReadDBquestions(){
        val questionlists = pultusORM.find(QuestionList())
        for (v in questionlists){
            val V = v as QuestionList
            GlobalObject.ClearQuestions()
            GlobalObject.questions.add(Question(V.q0q,V.q0a,V.q0b,V.q0c,V.q0d,V.q0e,V.q0f,V.q0g))
            GlobalObject.questions.add(Question(V.q1q,V.q1a,V.q1b,V.q1c,V.q1d,V.q1e,V.q1f,V.q1g))
            GlobalObject.questions.add(Question(V.q2q,V.q2a,V.q2b,V.q2c,V.q2d,V.q2e,V.q2f,V.q2g))
            GlobalObject.questions.add(Question(V.q3q,V.q3a,V.q3b,V.q3c,V.q3d,V.q3e,V.q3f,V.q3g))
            GlobalObject.questions.add(Question(V.q4q,V.q4a,V.q4b,V.q4c,V.q4d,V.q4e,V.q4f,V.q4g))
            GlobalObject.questions.add(Question(V.q5q,V.q5a,V.q5b,V.q5c,V.q5d,V.q5e,V.q5f,V.q5g))
            GlobalObject.questions.add(Question(V.q6q,V.q6a,V.q6b,V.q6c,V.q6d,V.q6e,V.q6f,V.q6g))
            GlobalObject.questions.add(Question(V.q7q,V.q7a,V.q7b,V.q7c,V.q7d,V.q7e,V.q7f,V.q7g))
            GlobalObject.questions.add(Question(V.q8q,V.q8a,V.q8b,V.q8c,V.q8d,V.q8e,V.q8f,V.q8g))
            GlobalObject.questions.add(Question(V.q9q,V.q9a,V.q9b,V.q9c,V.q9d,V.q9e,V.q9f,V.q9g))
            GlobalObject.questions.add(Question(V.q10q,V.q10a,V.q10b,V.q10c,V.q10d,V.q10e,V.q10f,V.q10g))
        }
    }
    fun SaveQuestions(){
        val ques = QuestionList()

        if(GlobalObject.questions.size >= 1){
            ques.q0q = GlobalObject.questions[0].question_text
            ques.q0a = GlobalObject.questions[0].option1
            ques.q0b = GlobalObject.questions[0].option2
            ques.q0c = GlobalObject.questions[0].option3
            ques.q0d = GlobalObject.questions[0].option4
            ques.q0e = GlobalObject.questions[0].option5
            ques.q0f = GlobalObject.questions[0].option6
            ques.q0g = GlobalObject.questions[0].correct_option
        }


        if(GlobalObject.questions.size >= 2){
            ques.q1q = GlobalObject.questions[1].question_text
            ques.q1a = GlobalObject.questions[1].option1
            ques.q1b = GlobalObject.questions[1].option2
            ques.q1c = GlobalObject.questions[1].option3
            ques.q1d = GlobalObject.questions[1].option4
            ques.q1e = GlobalObject.questions[1].option5
            ques.q1f = GlobalObject.questions[1].option6
            ques.q1g = GlobalObject.questions[1].correct_option
        }


        if(GlobalObject.questions.size >= 3){
            ques.q2q = GlobalObject.questions[2].question_text
            ques.q2a = GlobalObject.questions[2].option1
            ques.q2b = GlobalObject.questions[2].option2
            ques.q2c = GlobalObject.questions[2].option3
            ques.q2d = GlobalObject.questions[2].option4
            ques.q2e = GlobalObject.questions[2].option5
            ques.q2f = GlobalObject.questions[2].option6
            ques.q2g = GlobalObject.questions[2].correct_option
        }


        if(GlobalObject.questions.size >= 4){
            ques.q3q = GlobalObject.questions[3].question_text
            ques.q3a = GlobalObject.questions[3].option1
            ques.q3b = GlobalObject.questions[3].option2
            ques.q3c = GlobalObject.questions[3].option3
            ques.q3d = GlobalObject.questions[3].option4
            ques.q3e = GlobalObject.questions[3].option5
            ques.q3f = GlobalObject.questions[3].option6
            ques.q3g = GlobalObject.questions[3].correct_option
        }

        if(GlobalObject.questions.size >= 5){
            ques.q4q = GlobalObject.questions[4].question_text
            ques.q4a = GlobalObject.questions[4].option1
            ques.q4b = GlobalObject.questions[4].option2
            ques.q4c = GlobalObject.questions[4].option3
            ques.q4d = GlobalObject.questions[4].option4
            ques.q4e = GlobalObject.questions[4].option5
            ques.q4f = GlobalObject.questions[4].option6
            ques.q4g = GlobalObject.questions[4].correct_option
        }

        if(GlobalObject.questions.size >= 6){
            ques.q5q = GlobalObject.questions[5].question_text
            ques.q5a = GlobalObject.questions[5].option1
            ques.q5b = GlobalObject.questions[5].option2
            ques.q5c = GlobalObject.questions[5].option3
            ques.q5d = GlobalObject.questions[5].option4
            ques.q5e = GlobalObject.questions[5].option5
            ques.q5f = GlobalObject.questions[5].option6
            ques.q5g = GlobalObject.questions[5].correct_option
        }

        if(GlobalObject.questions.size >= 7){
            ques.q6q = GlobalObject.questions[6].question_text
            ques.q6a = GlobalObject.questions[6].option1
            ques.q6b = GlobalObject.questions[6].option2
            ques.q6c = GlobalObject.questions[6].option3
            ques.q6d = GlobalObject.questions[6].option4
            ques.q6e = GlobalObject.questions[6].option5
            ques.q6f = GlobalObject.questions[6].option6
            ques.q6g = GlobalObject.questions[6].correct_option
        }

        if(GlobalObject.questions.size >= 8){
            ques.q7q = GlobalObject.questions[7].question_text
            ques.q7a = GlobalObject.questions[7].option1
            ques.q7b = GlobalObject.questions[7].option2
            ques.q7c = GlobalObject.questions[7].option3
            ques.q7d = GlobalObject.questions[7].option4
            ques.q7e = GlobalObject.questions[7].option5
            ques.q7f = GlobalObject.questions[7].option6
            ques.q7g = GlobalObject.questions[7].correct_option
        }

        if(GlobalObject.questions.size >= 9){
            ques.q8q = GlobalObject.questions[8].question_text
            ques.q8a = GlobalObject.questions[8].option1
            ques.q8b = GlobalObject.questions[8].option2
            ques.q8c = GlobalObject.questions[8].option3
            ques.q8d = GlobalObject.questions[8].option4
            ques.q8e = GlobalObject.questions[8].option5
            ques.q8f = GlobalObject.questions[8].option6
            ques.q8g = GlobalObject.questions[8].correct_option
        }

        if(GlobalObject.questions.size >= 10){
            ques.q9q = GlobalObject.questions[9].question_text
            ques.q9a = GlobalObject.questions[9].option1
            ques.q9b = GlobalObject.questions[9].option2
            ques.q9c = GlobalObject.questions[9].option3
            ques.q9d = GlobalObject.questions[9].option4
            ques.q9e = GlobalObject.questions[9].option5
            ques.q9f = GlobalObject.questions[9].option6
            ques.q9g = GlobalObject.questions[9].correct_option
        }

        if(GlobalObject.questions.size >= 11){
            ques.q10q = GlobalObject.questions[10].question_text
            ques.q10a = GlobalObject.questions[10].option1
            ques.q10b = GlobalObject.questions[10].option2
            ques.q10c = GlobalObject.questions[10].option3
            ques.q10d = GlobalObject.questions[10].option4
            ques.q10e = GlobalObject.questions[10].option5
            ques.q10f = GlobalObject.questions[10].option6
            ques.q10g = GlobalObject.questions[10].correct_option
        }
        pultusORM.save(ques)
        val de = DataExists()
        de.data_exists = true
        pultusORM.save(de)
    }

    class ScoreTime {
        var score: Int = 0
        var time: String = ""
    }
    class DataExists{
        var data_exists: Boolean = false
    }

    class QuestionList{
        var q0q:String=""
        var q1q: String = ""
        var q2q: String = ""
        var q3q: String = ""
        var q4q: String = ""
        var q5q: String = ""
        var q6q: String = ""
        var q7q: String = ""
        var q8q: String = ""
        var q9q: String = ""
        var q10q: String = ""

        var q0a:String = ""
        var q1a: String = ""
        var q2a: String = ""
        var q3a: String = ""
        var q4a: String = ""
        var q5a: String = ""
        var q6a: String = ""
        var q7a: String = ""
        var q8a: String = ""
        var q9a: String = ""
        var q10a: String = ""

        var q0b: String = ""
        var q1b: String = ""
        var q2b: String = ""
        var q3b: String = ""
        var q4b: String = ""
        var q5b: String = ""
        var q6b: String = ""
        var q7b: String = ""
        var q8b: String = ""
        var q9b: String = ""
        var q10b: String = ""

        var q0c: String = ""
        var q1c: String = ""
        var q2c: String = ""
        var q3c: String = ""
        var q4c: String = ""
        var q5c: String = ""
        var q6c: String = ""
        var q7c: String = ""
        var q8c: String = ""
        var q9c: String = ""
        var q10c: String = ""

        var q0d: String = ""
        var q1d: String = ""
        var q2d: String = ""
        var q3d: String = ""
        var q4d: String = ""
        var q5d: String = ""
        var q6d: String = ""
        var q7d: String = ""
        var q8d: String = ""
        var q9d: String = ""
        var q10d: String = ""

        var q0e: String = ""
        var q1e: String = ""
        var q2e: String = ""
        var q3e: String = ""
        var q4e: String = ""
        var q5e: String = ""
        var q6e: String = ""
        var q7e: String = ""
        var q8e: String = ""
        var q9e: String = ""
        var q10e: String = ""

        var q0f: String = ""
        var q1f: String = ""
        var q2f: String = ""
        var q3f: String = ""
        var q4f: String = ""
        var q5f: String = ""
        var q6f: String = ""
        var q7f: String = ""
        var q8f: String = ""
        var q9f: String = ""
        var q10f: String = ""

        var q0g: Int = 0
        var q1g: Int = 0
        var q2g: Int = 0
        var q3g: Int = 0
        var q4g: Int = 0
        var q5g: Int = 0
        var q6g: Int = 0
        var q7g: Int = 0
        var q8g: Int = 0
        var q9g: Int = 0
        var q10g: Int = 0
    }
}





