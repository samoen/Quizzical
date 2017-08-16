package sam.quizzical

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_edit.*

class EditActivity : AppCompatActivity() {

    private var optionState:Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        InitViews()
        ResetOptionState()
    }

    fun ResetOptionState(){
        optionState = 1
        textView.text = "A"
    }

    fun InitViews(){
        button_back.setOnClickListener {
            startActivity(Intent(this,MainActivity()::class.java))
        }
        button_add_question.setOnClickListener {
            GlobalObject.questions.add(Question(
                    editText.text.toString(),editText2.text.toString(),
                    editText3.text.toString(),editText4.text.toString(),
                    editText5.text.toString(),editText6.text.toString(),
                    editText7.text.toString(),optionState
            ))
            editText.text.clear()
            editText2.text.clear()
            editText3.text.clear()
            editText4.text.clear()
            editText5.text.clear()
            editText6.text.clear()
            editText7.text.clear()
            ResetOptionState()
        }
        button_choose_correct.setOnClickListener {
            if (optionState == 6)optionState = 1 else optionState++
            when(optionState){
                1-> textView.text = "A"
                2-> textView.text = "B"
                3-> textView.text = "C"
                4-> textView.text = "D"
                5-> textView.text = "E"
                6-> textView.text = "F"
            }
        }
    }
}
