package org.beide.bomber;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class BomberView extends SurfaceView implements SurfaceHolder.Callback {
	
	public String TAG = "Bomber";
	
	BomberThread thread;
	Context context;
	
	public BomberThread getThread() {
		return thread;
	}
	
	public BomberView(Context c) {
		super(c);
		
		context = c;
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		thread = new BomberThread(holder);
		
		setFocusable(true);
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		holder.addCallback(this);
		setOnTouchListener(thread);
		thread.setRunning(true);
		thread.start();
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		thread.surfaceSize(w, h);
	}
	
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		thread.setRunning(false);
		
		// Bad things will happen without this
		boolean retry = true;
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}
}