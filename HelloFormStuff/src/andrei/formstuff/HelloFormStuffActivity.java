package andrei.formstuff;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.RatingBar.OnRatingBarChangeListener;

public class HelloFormStuffActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // button
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(HelloFormStuffActivity.this, "Beep Bop", Toast.LENGTH_SHORT).show();
			}
		});
        
        // edit text
        final EditText edittext = (EditText) findViewById(R.id.edittext);
        edittext.setText("ana are mere...");
        edittext.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					Toast.makeText(HelloFormStuffActivity.this, edittext.getText(), Toast.LENGTH_SHORT).show();
					return true;
				}
				return false;
			}
		});
        
        // check box
        final CheckBox checkbox = (CheckBox) findViewById(R.id.checkbox);
        checkbox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ( ((CheckBox) v).isChecked() )
					Toast.makeText(HelloFormStuffActivity.this, "Selected", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(HelloFormStuffActivity.this, "Not selected", Toast.LENGTH_SHORT).show();
			}
		});
        // radio button listener
        OnClickListener radio_listener = new OnClickListener() {
            public void onClick(View v) {
                // Perform action on clicks
                RadioButton rb = (RadioButton) v;
                Toast.makeText(HelloFormStuffActivity.this, rb.getText(), Toast.LENGTH_SHORT).show();
            }
        };
        final RadioButton radio_red = (RadioButton) findViewById(R.id.radio_red);
        final RadioButton radio_blue = (RadioButton) findViewById(R.id.radio_blue);
        radio_red.setOnClickListener(radio_listener);
        radio_blue.setOnClickListener(radio_listener);
        
        // toggle button
        final ToggleButton togglebutton = (ToggleButton) findViewById(R.id.togglebutton);
        togglebutton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (togglebutton.isChecked())
		            Toast.makeText(HelloFormStuffActivity.this, "Checked", Toast.LENGTH_SHORT).show();
				else
		            Toast.makeText(HelloFormStuffActivity.this, "Not checked", Toast.LENGTH_SHORT).show();
			}
		});
        
        // rating bar
        final RatingBar ratingbar = (RatingBar) findViewById(R.id.ratingbar);
        ratingbar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {
				Toast.makeText(HelloFormStuffActivity.this, "New rating: " + rating, Toast.LENGTH_SHORT).show();
			}
		});
    }
}