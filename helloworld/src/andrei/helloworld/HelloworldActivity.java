package andrei.helloworld;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

public class HelloworldActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EditText et = new EditText(this);
        et.setText("Hello Android! Edit me..");
        setContentView(et);
    }
}