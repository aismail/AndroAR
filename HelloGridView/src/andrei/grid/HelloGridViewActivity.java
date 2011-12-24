package andrei.grid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class HelloGridViewActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));
        
        gridview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Toast.makeText(HelloGridViewActivity.this, "" + position, Toast.LENGTH_SHORT).show();
			}
		});
    }
    
    public class ImageAdapter extends BaseAdapter {
    	private Context mContext;
    	
    	public ImageAdapter(Context c) {
    		mContext = c;
    	}

		@Override
		public int getCount() {
			return mThumbIds.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(8, 8, 8, 8);
			} else {
				imageView = (ImageView) convertView;
			}
			imageView.setImageResource(mThumbIds[position]);
			return imageView;
		}
		
		private Integer[] mThumbIds = {
				R.drawable.sample_0, R.drawable.sample_3,
	            R.drawable.sample_4, R.drawable.sample_5,
	            R.drawable.sample_6, R.drawable.sample_7,
	            R.drawable.sample_0, R.drawable.sample_1,
	            R.drawable.sample_2, R.drawable.sample_3,
	            R.drawable.sample_4, R.drawable.sample_5,
	            R.drawable.sample_6, R.drawable.sample_7,
	            R.drawable.sample_0, R.drawable.sample_1,
	            R.drawable.sample_2, R.drawable.sample_3,
	            R.drawable.sample_4, R.drawable.sample_5,
	            R.drawable.sample_6, R.drawable.sample_7
		};
	}
}