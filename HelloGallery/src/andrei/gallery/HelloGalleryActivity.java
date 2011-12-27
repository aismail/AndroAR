package andrei.gallery;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class HelloGalleryActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Gallery gallery = (Gallery) findViewById(R.id.gallery);
        gallery.setAdapter(new ImageAdapter(this));
        
        gallery.setOnItemClickListener(new OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View view, int pos,
        			long id) {
        		Toast.makeText(HelloGalleryActivity.this, "" + pos, Toast.LENGTH_SHORT).show();	
        	}
		});
    }
    
    private class ImageAdapter extends BaseAdapter {
    	int mGalleryItemBackground;
    	private Context mContext;
    	
    	private Integer[] mImageIds = {
    			R.drawable.sample_0,
    			R.drawable.sample_1,
    			R.drawable.sample_2,
    			R.drawable.sample_3,
    			R.drawable.sample_4,
    			R.drawable.sample_5,
    			R.drawable.sample_6,
    			R.drawable.sample_7
    	};
    	
    	public ImageAdapter(Context c) {
    		mContext = c;
    		TypedArray attr = mContext.obtainStyledAttributes(R.styleable.HelloGallery);
    		mGalleryItemBackground = attr.getResourceId(R.styleable.HelloGallery_android_galleryItemBackground, 0);
    		attr.recycle();
    	}

		@Override
		public int getCount() {
			return mImageIds.length;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView iv = new ImageView(mContext);
			
			iv.setImageResource(mImageIds[position]);
			iv.setLayoutParams(new Gallery.LayoutParams(150, 100));
			iv.setScaleType(ImageView.ScaleType.FIT_XY);
			iv.setBackgroundResource(mGalleryItemBackground);
			return iv;
		}
    }
}