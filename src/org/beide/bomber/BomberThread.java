/*
 * Copyright (C) Kees Huiberts <itissohardtothinkofagoodemail@gmail.com> 2012
 * 
 * Licensed under the GPL v3
 * 
 * Distributed without any warranty, including those about merchantability
 * or fitness for a particular purpose.
 * If you did not receive the license along with this file, you can find it
 * at <http://www.gnu.org/licenses/>.
 */

package org.beide.bomber;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class BomberThread extends Thread implements View.OnTouchListener {
	
	public String TAG = "Bomber";
	
	Context context;
	Resources res;
	
	int[][] levels = {
		{0,0,1,2,3,4,5,4,3,2,1,0},
		{0,5,5,5,5,5,5,5,5,5,5,0},
		{0,9,8,7,6,5,4,3,2,1,0,0}
	};
	
	int UNITS_HORIZONTAL = 12;
	int UNITS_VERTICAL = 18;
	
	// Size of bomb
	int BOMB_RADIUS = 5;
	
	int PLANE_START_HEIGHT = 17;
	
	SurfaceHolder holder;
	
	Paint textpaint, paint;
	Bitmap plane, tower, bomb, background;
	
	boolean running = true;
	boolean paused;
	
	int canvaswidth = 500;
	int canvasheight = 500;
	
	// Size of one unit
	int unitheight;
	int unitwidth;
	
	int score;
	int level;
	float bombX, bombY, bombgravity;
	float planeX, planeY, velocity, planegravity, planestart;
	int[] towers;
	
	public BomberThread(SurfaceHolder hold, Context c) {
		holder = hold;
		context = c;
		
		res = c.getResources();
		
		textpaint = new Paint();
		textpaint.setColor(Color.BLACK);
		
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	}
	
	public void setRunning(boolean run) {
		running = run;
	}
	
	public void setPaused(boolean pause) {
		paused = pause;
	}
	
	public void surfaceSize(int width, int height) {
		synchronized(holder) {
			canvasheight = height;
			canvaswidth = width;
			unitwidth = width / UNITS_HORIZONTAL;
			unitheight = height / UNITS_VERTICAL;
			Log.v(TAG, "Canvas is " + canvaswidth + "x" + canvasheight);
			Log.v(TAG, "Units are " + unitwidth + "x" + unitheight);
			planestart = PLANE_START_HEIGHT * unitheight;
			bombgravity = canvasheight / 200;
			velocity = canvaswidth / 150;
			planegravity = unitheight;
			
			plane = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.plane),
																				unitwidth, (int) unitheight / 2, true);
			tower = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.tower),
																				unitwidth, unitheight, true);
			bomb = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.bomb),
																			 BOMB_RADIUS, BOMB_RADIUS * 2, true);
			background = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.background),
																						 width, height, true);
		}
	}
	
	public void restart() {
		synchronized(holder) {
			score = 0;
			initlevel(0);
		}
	}
	
	public void initlevel(int lvl) {
		Log.v(TAG, "initing level " + lvl);
		synchronized(holder) {
			level = lvl;
			bombY = 0;
			planeY = planestart;
			planeX = 0;
			towers = (int[]) levels[lvl].clone();
			bombgravity = canvasheight / 200;
			velocity = canvaswidth / 150;
			planegravity = unitheight;
		}
	}
	
	public void draw(Canvas canvas) {
		canvas.drawBitmap(background, (float) 0, (float) 0, paint);
		
		// Draw the towers
		for(int i = 0; i < towers.length; i++) {
			for(int j = 0; j < towers[i]; j++) {
				canvas.drawBitmap(tower, (float) i * unitwidth,
													(float) canvasheight - j * unitheight, paint);
			}
		}
		
		// Draw the plane
		canvas.drawBitmap(plane, planeX - unitwidth,
											canvasheight - planeY + unitheight / 2, paint);
		
		if(bombY > 0) {
			// Draw the bomb
			canvas.drawBitmap(bomb, bombX - BOMB_RADIUS, canvasheight - bombY - BOMB_RADIUS, paint);
		}
		
		canvas.drawText("Score: " + score, (float) 0, 0, textpaint);
		canvas.drawText("Level: " + (level + 1), (float) 0, unitheight, textpaint);
	}
	
	public void update() {
		
		synchronized(holder) {
			if(bombY != 0) {
				bombY -= bombgravity;
				if(towers[(int) bombX / unitwidth] * unitheight >= bombY || bombY >= canvasheight) {
					towers[(int) bombX / unitwidth]--;
					
					score++;
					bombY = 0;
				}
			}
			
			planeX += velocity;
			if(planeX >= canvaswidth) {
				planeX = 0;
				planeY -= planegravity;
			}
			
			if(towers[(int) planeX / unitwidth] * unitheight >= planeY) {
				gameover();
			}
			
			// If there are towers, return
			for(int i = 0; i < towers.length; i++) {
				if(towers[i] > 0) {
					return;
				}
			}
			
			// There are no towers, so we will level up
			initlevel(level + 1);
		}
	}
	
	public void gameover() {
		Log.i(TAG, "Game over! Score: " + score);
		score = 0;
		initlevel(0);
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		// If the bomb is available, drop it
		if(bombY == 0) {
			bombY = planeY - unitheight;
			bombX = planeX - (planeX % unitwidth) + unitwidth / 2;
		}
		
		return true;
	}
	
	public void run() {
		restart();
		while(running) {
			Canvas c = null;
			try {
				c = holder.lockCanvas(null);
				synchronized(holder) {
					if(!paused) update();
					draw(c);
				}
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				if(c != null) {
					holder.unlockCanvasAndPost(c);
				} else {
					Log.e(TAG, "Canvas is null");
				}
			}
		}
	}
}