/*
This is a simple color filler game.  
The goal to capture the whole playground by choosing the right color. The player start from the left bottom corner and will own a choosen color.
- You can resize the cells and the size of the playground
- You can scroll and zoom the playground
- You can undo the last step

v 1.1.1
- Some bugfix in play table.
- Save the last playtable.
- You can undo the last step.
- Fix save table on exit.

v 1.2
- Add Hungarian language
- Some small modification.

Ez egy egyszerű szín területfoglaló játék
A cél elfoglalni a teljes játékteret a megfelelő szín kiválasztásával. A játékos a bal alsó sarokból indul és a kiválasztott szín a sajátja lesz.
- a színblokkok és a pálya átméretezhető
- a pálya görgethető és nagyítható
- az utolsó lépés visszavonható

v 1.1.1
- Hibajavítások a játék táblán.
- Az utolsó állapot mentése.
- Visszavonható az utolsó lépés.
- Hibajavítás a mentésnél.

 v 1.2
 - Magyar nyelv hozzáadása
 - Néhány apró módosítás.
 
 */

package org.landroo.colorizer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.landroo.http.WebClass;
import org.landroo.http.HttpServer;
import org.landroo.ui.UI;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class ColorizerActivity extends Activity implements org.landroo.ui.UIInterface  
{
	private static final String TAG = "ColorizerActivity"; 
	private static final int SWIPE_INTERVAL = 10;
	private static final int HTTP_PORT = 8585;
	private static final int MAX_PARTNER = 16;
	private static final String GAME_NET_NAME = "colorizer";
	
	private static Bitmap bitmap = null; // the paper

	private ColorizerClass[] colorizerList = new ColorizerClass[MAX_PARTNER]; // the main game class list
	private int activePlayerNo = 0;	
	
	private UI ui = null;
	private ColorizerView colorizerview;

	private BitmapDrawable bitmapDrawable;
	private BitmapDrawable backDrawable;

	private int displayWidth = 0;			// display width
	private int displayHeight = 0;			// display height
    
	private int sX = 0;
	private int sY = 0;
	private int mX = 0;
	private int mY = 0;
    
	private float xPos = 0;
	private float yPos = 0;
	
    public float tableWidth;
    public float tableHeight;
    public float origWidth;
    public float origHeight;

	private Timer timer = null;
	private float swipeDistX = 0;
	private float swipeDistY = 0;
	private float swipeVelocity = 0;
	private float swipeSpeed = 0;
	private float backSpeedX = 0;
	private float backSpeedY = 0;
	private float offMarginX = 0;
	private float offMarginY = 0;
	
	private float zoomSize = 0;
	
	private Bitmap[] buttons;
	private Bitmap[] pressed;
	private int selectedButton = 0;
	
	private Paint paint;
	
	private String[] score = new String[4];
	
	private int cellSize = 20;
	private float tablesizeX = 1;
	private float tablesizeY = 1;
	private boolean showScore = true;
	private boolean zoomable = true;
	private int players = 2;
	
	private int buttonHeight;
	
	private int[] playerFiled;
	
    private SensorManager sensorManager;
    private Sensor sensor;
    private float currOri = 0f;
    private float lastOri = -1f;
    
    private boolean bFirst = true;
    
    private HttpServer http;
    private WebClass webClass = null;
    private int errorCnt = 0;

    private String playerName = "Player";
	private String address = "landroo.dyndns.org";
	private String sNetError;// Error access server:
	private String sUserError;// Username alredy in use!
	private String sUserSuccess;// Login success!
	private String sNameError;//Please change your ninck name in settings!
	private String sAlredyError;
	private String sFirstError;
	private String sOfflineError;
	private boolean bInit = true;
    
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			String ip = msg.getData().getString("ipaddress");
			if (ip != null && errorCnt == 0) Toast.makeText(ColorizerActivity.this, sNetError + " " + ip, Toast.LENGTH_LONG).show();

			if (msg.what == 12) Toast.makeText(ColorizerActivity.this, sUserError, Toast.LENGTH_LONG).show();
			if (msg.what == 13)
			{
				colorizerview.invalidate();
				Toast.makeText(ColorizerActivity.this, sUserSuccess, Toast.LENGTH_LONG).show();
			}
			if (msg.what == 14 && errorCnt++ > 4) logout(); // logout if 5 times network error

			String update = msg.getData().getString("update");
			if (update != null)
			{
				processUpdate(update);
				errorCnt = 0;
			}

		}
	};
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();
		
        ui = new UI(this);

        timer = new Timer();
		timer.scheduleAtFixedRate(new SwipeTask(), 0, SWIPE_INTERVAL);
		
		paint = new Paint();
		paint.setTextSize(24);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);
		paint.setShadowLayer(3, 0, 0, Color.BLACK);

		for(int i = 0; i < 4; i++) 
			score[i] = "";
		
        colorizerview = new ColorizerView(this);
        setContentView(colorizerview);
        
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(senzorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        
		sNetError = getResources().getString(R.string.net_error);
		sUserError = getResources().getString(R.string.user_error);
		sUserSuccess = getResources().getString(R.string.net_success);
		sNameError = getResources().getString(R.string.name_error);
		sAlredyError = getResources().getString(R.string.alredy_error);
		sFirstError = getResources().getString(R.string.first_login);
		sOfflineError = getResources().getString(R.string.only_offline);
	}
	
	private void initApp(int size, float tableWidthMul, float tableHeightMul, int playerNum, String sFields)
	{
		try
		{
			if (colorizerList[activePlayerNo] == null)
			{
				tableWidth = displayWidth * tableWidthMul;
				tableHeight = displayHeight * tableHeightMul;
				origWidth = tableWidth;
				origHeight = tableHeight;
				
				int w = displayWidth / 8;
				int h = displayHeight / 16;
	
				buttonHeight = h + 10;
				
				colorizerList[activePlayerNo] = new ColorizerClass(size, playerNum);
				colorizerList[activePlayerNo].setName("Droid");
				colorizerList[activePlayerNo].newEvent = true;
				if (bitmap != null)
				{
					bitmap.recycle();
					bitmap = null;
					System.gc();
				}				
				bitmap = colorizerList[activePlayerNo].drawGame((int)tableWidth, (int)tableHeight, sFields);
				if (bitmap == null) System.exit(1);
				bitmapDrawable = new BitmapDrawable(bitmap);
				bitmapDrawable.setBounds(0, 0, (int)tableWidth, (int)tableHeight - buttonHeight);
				
				if (bInit)
				{
					Bitmap back = colorizerList[activePlayerNo].getBackGround(displayWidth, displayHeight, getResources());
					backDrawable = new BitmapDrawable(back);
					backDrawable.setBounds(0, 0, displayWidth, displayHeight);
					bInit = false;
				}
	
				setButtons(w, h);
			}
			
			// ten percent
			offMarginX = (displayWidth / 10) * (displayHeight / displayWidth);
			offMarginY = (displayHeight / 10) * (displayHeight / displayWidth);
	
			xPos = 0;
			yPos = displayHeight - tableHeight;
		}
		catch (OutOfMemoryError e)
		{
			Log.e(TAG, "Out of memory error in new page!");
			System.exit(1);
		}
		catch (Exception ex)
		{
			Log.e(TAG, "" + ex);
		}

		return;			
	}
	
	private void setButtons(int w, int h)
	{
		buttons = new Bitmap[8];
		buttons[0] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFF0000, false, false);
		buttons[1] = colorizerList[activePlayerNo].creteButton(w, h, 0xAA00FF00, false, false);
		buttons[2] = colorizerList[activePlayerNo].creteButton(w, h, 0xAA0000FF, false, false);
		buttons[3] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFF00FF, false, false);
		buttons[4] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFFFF00, false, false);
		buttons[5] = colorizerList[activePlayerNo].creteButton(w, h, 0xAA00FFFF, false, false);
		buttons[6] = colorizerList[activePlayerNo].creteButton(w, h, 0xAACCCCCC, false, false);
		buttons[7] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFFFFFF, false, true);
		
		pressed = new Bitmap[8];
		pressed[0] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFF0000, true, false);
		pressed[1] = colorizerList[activePlayerNo].creteButton(w, h, 0xAA00FF00, true, false);
		pressed[2] = colorizerList[activePlayerNo].creteButton(w, h, 0xAA0000FF, true, false);
		pressed[3] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFF00FF, true, false);
		pressed[4] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFFFF00, true, false);
		pressed[5] = colorizerList[activePlayerNo].creteButton(w, h, 0xAA00FFFF, true, false);
		pressed[6] = colorizerList[activePlayerNo].creteButton(w, h, 0xAACCCCCC, true, false);
		pressed[7] = colorizerList[activePlayerNo].creteButton(w, h, 0xAAFFFFFF, true, true);
	}
	
    @Override
    public void onStart() 
    {
        super.onStart();
    }
    
    @Override
    public synchronized void onResume() 
    {
    	boolean change = false;
    	
		SharedPreferences settings = getSharedPreferences("org.landroo.colorizer_preferences", MODE_PRIVATE);

		int i = Integer.parseInt(settings.getString("players", "2"));
		if(this.players != i) change = true;
		this.players = i;
		i = settings.getInt("cellSize", 20);
		if(this.cellSize != i) change = true;
		this.cellSize = i;
		
		float f = settings.getInt("tableSizeX", 100);
		if(this.tablesizeX != f / 100) change = true;
		this.tablesizeX = f / 100;
		f = settings.getInt("tableSizeY", 100);
		if(this.tablesizeY != f / 100) change = true;
		this.tablesizeY = f / 100;

		boolean b = settings.getBoolean("showscore", true);
		if(this.showScore != b) change = true;
		this.showScore = b;
		b = settings.getBoolean("zoom", true);
		this.zoomable = b;
		
		String s = "";
		if (bFirst) s = settings.getString("table", "");
		String table = s;
		bFirst = false;
		s = settings.getString("player", "Player");
		s = s.replace(" ", "_");
		s = s.replace("?", "_");
		s = s.replace("=", "_");
		s = s.replace("&", "_");
		s = s.replace(";", "_");
		s = s.replace(":", "_");
		s = s.replace("/", "_");
		if (!this.playerName.equals(s))
		{
			this.logout();
			change = true;
		}
		playerName = s;
		s = settings.getString("server", "landroo.dyndns.org");
		if (!this.address.equals(s)) change = true;
		address = s;
		
		if(change) colorizerList[activePlayerNo] = null;
		initApp(cellSize, tablesizeX, tablesizeY, players, table);
		
		if (change)
		{
			webClass = new WebClass(address, GAME_NET_NAME, playerName, handler, HTTP_PORT);
			webClass.size = cellSize;
			webClass.width = (int) tablesizeX * displayWidth;
			webClass.height = (int) tablesizeY * displayHeight;
		}

		if (bitmap == null) drawPlayer(activePlayerNo, 0);		
		
		if(checkWifi() && !playerName.equals("Player")) webClass.login();

        super.onResume();
    }
    
    @Override
    public synchronized void onPause() 
    {
		saveState();
		if (webClass.loggedIn) logout();

		bitmap.recycle();
		bitmap = null;
		System.gc();
        
		super.onPause();
    }
    
    @Override
    public void onStop() 
    {
		if (webClass.loggedIn) webClass.logout();

		if (http != null) http.stop();
		
        super.onStop();
    }

    @Override
    public void onDestroy() 
    {
    	if (webClass.loggedIn) webClass.logout();
    	
        super.onDestroy();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_colorizer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle item selection
		switch (item.getItemId()) 
		{
		// settings
		case R.id.menu_settings:
			Intent SettingsIntent = new Intent(this, SettingsScreen.class);
			startActivity(SettingsIntent);
			return true;
		// exit	
		case R.id.menu_exit:
			saveState();
			logout();
			this.finish();
			//System.runFinalizersOnExit(true);
			//this.setResult(1);
			//int pid = android.os.Process.myPid();
			//android.os.Process.killProcess(pid);
			//System.exit(0);
			return true;
		// new table	
		case R.id.menu_new:
			this.colorizerList[activePlayerNo] = null;
			initApp(cellSize, tablesizeX, tablesizeY, players, "");
			colorizerview.postInvalidate();
			return true;
		// undo	
		case R.id.menu_undo:
			this.colorizerList[activePlayerNo].undo();
			colorizerview.postInvalidate();
			return true;
		// login
		case R.id.menu_login:
			login();
			return true;

		// logout
		case R.id.menu_logout:
			logout();
			return true;

		// users
		case R.id.menu_list:
			userlist();
			return true;			
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onDown(float x, float y) 
	{
    	sX = (int)x;
    	sY = (int)y;
    	
    	swipeVelocity = 0;
    	
		selectedButton = checkButtons(x, y);
    	
		colorizerview.postInvalidate();
	}

	@Override
	public void onUp(float x, float y) 
	{
		selectedButton = 0;
		
		colorizerview.postInvalidate();
	}

	@Override
	public void onTap(float x, float y) 
	{
		int num = checkButtons(x, y); 
		if(num != 0)
		{
			// change player color
			colorizerList[activePlayerNo].changeColor(1, num, true);
			for(int i = 2; i <= players; i++)
			{
				if(colorizerList[activePlayerNo].msEndPlayers.indexOf("" + i) == -1)
				{
					// select AI color
					num = colorizerList[activePlayerNo].colorizerAI(i);
					// if no more place to capture
					if(num == 0)
					{
						playerFiled = colorizerList[activePlayerNo].getFullNum();
						this.checkEnd(i, "Droid " + i, num);
					}
				}
			}
			//playerFiled = colorizer.getFullNum();
			
			setScore();
			
			colorizerview.postInvalidate();
		}
	}

	@Override
	public void onHold(float x, float y) 
	{
	}

	@Override
	public void onMove(float x, float y) 
	{
		if(selectedButton != 0) return;
		
		mX = (int) x;
		mY = (int) y;

		float dx = mX - sX;
		float dy = mY - sY;

		// picture bigger than the display
		if((tableWidth >= displayWidth) && (xPos + dx < displayWidth - (tableWidth + offMarginX) || xPos + dx > offMarginX)) dx = 0;
		if((tableHeight >= displayHeight) && (yPos + dy < displayHeight - (tableHeight + offMarginY) || yPos + dy > offMarginY)) dy = 0;
		if((tableWidth < displayWidth) && (xPos + dx > displayWidth - tableWidth || xPos + dx < 0)) dx = 0;
		if((tableHeight < displayHeight) && (yPos + dy > displayHeight - tableHeight || yPos + dy < 0)) dy = 0;

		xPos += dx;
		yPos += dy;

		sX = (int) mX;
		sY = (int) mY;
		
		colorizerview.postInvalidate();
	}

	@Override
	public void onSwipe(int direction, float velocity, float x1, float y1, float x2, float y2) 
	{
    	swipeDistX = x2 - x1;
		swipeDistY = y2 - y1;    			
		swipeSpeed = 1;
		swipeVelocity = velocity;
		
		colorizerview.postInvalidate();
	}

	@Override
	public void onDoubleTap(float x, float y) 
	{
		tableWidth = origWidth;
		tableHeight = origHeight;
		
		xPos = 0;
		yPos = displayHeight - tableHeight;
		
		bitmapDrawable.setBounds(0, 0, (int)tableWidth, (int)tableHeight - buttonHeight);
		
		colorizerview.postInvalidate();
	}

	@Override
	public void onZoom(int mode, float x, float y, float distance, float xdiff,	float ydiff) 
	{
		if(!zoomable) return;
		
		int dist = (int)distance * 5;
		switch(mode)
		{
			case 1:
				zoomSize = dist;
				break;
			case 2:	
	    		int diff = (int)(dist - zoomSize);
	    		double sizeOrig = Math.sqrt(tableWidth * tableWidth + tableHeight * tableHeight);
	    		double sizeDiff = 100 / (sizeOrig / (sizeOrig + diff));
	    		int newSizeX = (int)(tableWidth * (sizeDiff / 100));
	    		int newSizeY = (int)(tableHeight * (sizeDiff / 100));

	    		// zoom between min and max value
		    	if(newSizeX > origWidth / 4 && newSizeX < origWidth * 10)
    	    	{
		    		bitmapDrawable.setBounds(0, 0, newSizeX, newSizeY - buttonHeight);
    	    		zoomSize = dist;
    	    		
    	    		float diffX = newSizeX - tableWidth;
    	    		float diffY = newSizeY - tableHeight;
    	    		float xPer = 100 / (tableWidth / (Math.abs(xPos) + mX)) / 100;
    	    		float yPer = 100 / (tableHeight / (Math.abs(yPos) + mY)) / 100;
    	    		
    	    		xPos -= diffX * xPer;
    	    		yPos -= diffY * yPer;
    	    		
    	    		tableWidth = newSizeX;
    	    		tableHeight = newSizeY;
    	    		
    	    		if(tableWidth > displayWidth || tableHeight > displayHeight)
    	    		{
        	    		if(xPos > 0) xPos = 0;
        	    		if(yPos > 0) yPos = 0;
        	    		
        	    		if(xPos + tableWidth < displayWidth) xPos = displayWidth - tableWidth;
        	    		if(yPos + tableHeight < displayHeight) yPos = displayHeight - tableHeight;
    	    		}
    	    		else
    	    		{
    	    			if(xPos <= 0) xPos = 0;
    	    			if(yPos <= 0) yPos = 0;
    	    			
    	    			if(xPos + tableWidth > displayWidth) xPos = displayWidth - tableWidth;
    	    			if(yPos + tableHeight > displayHeight) yPos = displayHeight - tableHeight;
    	    		}
	        		
	        		//Log.i(TAG, "" + xPos + " " + yPos);
    	    	}
				break;
			case 3:
				zoomSize = 0;
				break;
		}

		colorizerview.postInvalidate();	
	}

    // Touch event
    @Override
    public boolean onTouchEvent(MotionEvent event) 
    {
    	return ui.tapEvent(event);
    }
	
    /**
     * 
     * @author Lenovo
     *
     */
	private class ColorizerView extends View
    {
		private int row;
		
		public ColorizerView(Context context)
		{
			super(context);
		}
		
		@Override 
		protected void onDraw(Canvas canvas) 
		{
			if (backDrawable != null)
			{
				// canvas.save();
				// canvas.translate(xPos, yPos);
				backDrawable.draw(canvas);
				// canvas.translate(-xPos, -yPos);
				// canvas.restore();
			}
			
			// draw play table
			if(bitmapDrawable != null)
			{
				canvas.translate(xPos, yPos);
				bitmapDrawable.draw(canvas);
				canvas.restore();
			}			
			// draw buttons
			for(int i = 0; i < 7; i++)
			{
				if(colorizerList[activePlayerNo].msLastCols.indexOf("" + (i + 1)) == -1)
				{
					if(selectedButton == i + 1)
						canvas.drawBitmap(pressed[i], buttons[i].getWidth() * i + ((buttons[i].getWidth() - 10) / 6) * i + 5, displayHeight - buttons[i].getHeight() - 5, paint);
					else
						canvas.drawBitmap(buttons[i], buttons[i].getWidth() * i + ((buttons[i].getWidth() - 10) / 6) * i + 5, displayHeight - buttons[i].getHeight() - 5, paint);
				}
				else
				{
					canvas.drawBitmap(pressed[7], buttons[7].getWidth() * i + ((buttons[7].getWidth() - 10) / 6) * i + 5, displayHeight - buttons[7].getHeight() - 5, paint);
				}
			}
			
			// draw info
			if(showScore) for(int i = 0; i < players; i++) canvas.drawText(score[i], 0, i * 20 + 20, paint);
			//if(playerFiled != null) for(int i = 0; i < 6; i++) canvas.drawText("" + playerFiled[i], 0, i * 20 + 60, paint);
			//canvas.drawText("" + xPos + " " + yPos, 0, 60, paint);
			
			if (webClass.loggedIn)
			{
				row = 0;
				for (int j = 0; j < ColorizerActivity.MAX_PARTNER; j++)
				{
					if (colorizerList[j] != null)
					{
						if (colorizerList[j].newEvent) canvas.drawBitmap(colorizerList[j].button1, displayWidth - 96, row
								* buttonHeight + 10, paint);
						else canvas.drawBitmap(colorizerList[j].button3, displayWidth - 96, row * buttonHeight + 10, paint);
						row++;
					}
				}
			}

		}
    }
	
	@Override
	public void onRotate(int mode, float x, float y, float angle)
	{
	}

	class SwipeTask extends TimerTask
	{
		public void run()
		{
			if (swipeVelocity > 0)
			{
				float dist = FloatMath.sqrt(swipeDistY * swipeDistY + swipeDistX * swipeDistX);
				float x = xPos - (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				float y = yPos - (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				if ((tableWidth >= displayWidth) && (x < displayWidth - (tableWidth + offMarginX) || x > offMarginX)
						|| ((tableWidth < displayWidth) && (x > displayWidth - tableWidth || x < 0)))
				{
					swipeDistX *= -1;
					swipeSpeed = swipeVelocity;
					//swipeSpeed += .5;
				}

				if ((tableHeight >= displayHeight) && (y < displayHeight - (tableHeight + offMarginY) || y > offMarginY)
						|| ((tableHeight < displayHeight) && (y > displayHeight - tableHeight || y < 0)))
				{
					swipeDistY *= -1;
					swipeSpeed = swipeVelocity;
					//swipeSpeed += .5;
				}

				xPos -= (float) ((swipeDistX / dist) * (swipeVelocity / 10));
				yPos -= (float) ((swipeDistY / dist) * (swipeVelocity / 10));

				swipeVelocity -= swipeSpeed;
				swipeSpeed += .0001;

				colorizerview.postInvalidate();
				
				if(swipeVelocity <= 0) checkOff();
			}
			
			if(backSpeedX != 0)
			{
				if((backSpeedX < 0 && xPos <= 0.1f) || (backSpeedX > 0 && xPos + 0.1f >= displayWidth - tableWidth)) backSpeedX = 0;
				else if(backSpeedX < 0) xPos -= xPos / 20;
				else xPos += (displayWidth - (tableWidth + xPos)) / 20;

				colorizerview.postInvalidate();
			}
			
			if(backSpeedY != 0)
			{
				if((backSpeedY < 0 && yPos <= 0.1f) || (backSpeedY > 0 && yPos + 0.1f >= displayHeight - tableHeight)) backSpeedY = 0;
				else if(backSpeedY < 0) yPos -= yPos / 20;
				else yPos += (displayHeight - (tableHeight + yPos)) / 20;
				
				colorizerview.postInvalidate();
			}
			
			return;
		}
	}

	private void checkOff()
	{
		if(tableWidth >= displayWidth)
		{
			if(xPos > 0 && xPos <= offMarginX) backSpeedX = -1;
			else if(xPos < tableWidth - offMarginX && xPos <= tableWidth) backSpeedX = 1;
		}
		if(tableHeight >= displayHeight)
		{
			if(yPos > 0 && yPos <= offMarginY) backSpeedY = -1;
			else if(yPos < tableHeight - offMarginY && yPos <= tableHeight) backSpeedY = 1;
		}
	}    

    private int checkButtons(float x, float y)
    {
		float bx, by;
		
		// check 7 color
		for(int i = 0; i < 7; i++)
		{
			// calc button sizes
			bx = buttons[i].getWidth() * i + ((buttons[i].getWidth() - 10) / 6) * i;
			by = displayHeight - buttons[i].getHeight();
			
			if(x > bx && x < bx + buttons[i].getWidth() 
			&& y > by && y < by + buttons[i].getHeight()
			&& colorizerList[activePlayerNo].msLastCols.indexOf("" + (i + 1)) == -1) return i + 1;
		}
		
		return 0;
    }
    
    private void setScore()
    {
    	int iFnum = colorizerList[activePlayerNo].getFullNum()[5];
    	int iNum = colorizerList[activePlayerNo].getFullNum()[1];
    	int nPerc = (int)Math.round(((float)iNum / (float)iFnum) * 100);
    	score[0] = "Player: " + iNum + " (" + nPerc + "%)";
    	
		for(int i = 2; i <= players; i++)
		{
	    	score[i - 1] = "Droid: ";
	    	iNum = colorizerList[activePlayerNo].getFullNum()[i];
			nPerc = (int)Math.round(((float)iNum / (float)iFnum) * 100);
			score[i - 1] += iNum + " (" + nPerc + "%)";
		}
    }
    
    private void checkEnd(int iPly, String name, int num)
    {
		if(colorizerList[activePlayerNo].msEndPlayers.indexOf("" + iPly) == -1 && playerFiled[iPly] > 0)
		{
			colorizerList[activePlayerNo].msEndPlayers += iPly;
		
			int iNo = 0;
			for(int i = 1; i <= this.players; i++) if(i != iPly) iNo += playerFiled[i];
			
			if(iNo < playerFiled[iPly]) Toast.makeText(this, "Droid " + iPly + " win!", Toast.LENGTH_LONG).show();
			else Toast.makeText(this, "End for Droid " + iPly + "!", Toast.LENGTH_LONG).show();
		}
		
		if(colorizerList[activePlayerNo].msEndPlayers.length() + 1 == this.players)
		{
			for(int i = 1; i <= this.players; i++)
				if(colorizerList[activePlayerNo].msEndPlayers.indexOf("" + i) == -1)
					if(i == 1) Toast.makeText(this, "Player win!", Toast.LENGTH_LONG).show();
					else Toast.makeText(this, "Droid " + iPly + " win!", Toast.LENGTH_LONG).show();
		}
    }
    
	private SensorEventListener senzorListener = new SensorEventListener()
    {
        @Override
        public void onSensorChanged(SensorEvent event)
        {
    	    float[] values = event.values;
//    	    float x = values[0];
    	    float y = values[1];
    	    float z = values[2];
                	   
//			int angxy = Math.round(getViewAngDist(0, 0, x, y, true));
//			int angxz = Math.round(getViewAngDist(0, 0, x, z, true));
			int angyz = Math.round(getViewAngDist(0, 0, y, z, true));
			
			currOri = angDir(angyz);
			if(lastOri == -1) lastOri = currOri;
        }
        
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) 
        {
        }
    };
    
	private float getViewAngDist(float x1, float y1, float x2, float y2, boolean bMode)
    {
		float nDelX = x2 - x1;
		float nDelY = y2 - y1;
		float nDe = 0;

		if(bMode)
		{
			if(nDelX != 0)
			{
				nDe = 2 * (float)Math.PI;
				nDe = nDe + (float)Math.atan(nDelY / nDelX);
				if(nDelX <= 0)
				{
					nDe = (float)Math.PI;
					nDe = nDe + (float)Math.atan(nDelY / nDelX);
				}
				else if(nDelY >= 0)
				{
					nDe = 0;
					nDe = nDe + (float)Math.atan(nDelY / nDelX);
				}
			}
			else
			{
				if(nDelY == 0) nDe = 0;
				else
				{
					if(nDelY < 0) nDe = (float)Math.PI;
					nDe = nDe + (float)Math.PI / 2;
				}
			}
		
        	return nDe / (float)Math.PI * 180;
		}
        else
		{
        	return  (float)Math.sqrt(nDelY * nDelY + nDelX * nDelX);
		}
    }	
	
    private int angDir(int ang)
    {
    	int iRet = 0;
		if(ang > 0 && ang < 45) iRet = 0;
		else if(ang > 45 && ang < 135) iRet = 90;
		else if(ang > 135 && ang < 225) iRet = 180;
		else if(ang > 225 && ang < 305) iRet = 270;
		else iRet = 0;
		
		return iRet;
    }
    
    @Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
	    super.onConfigurationChanged(newConfig);

		Display display = getWindowManager().getDefaultDisplay();
		displayWidth = display.getWidth();
		displayHeight = display.getHeight();

		int w = displayWidth / 8;
		int h = displayHeight / 16;
		buttonHeight = h + 10;
		
		xPos = 0;
		yPos = displayHeight - tableHeight;
		
		setButtons(w, h);
	}
    
    private void saveState()
    {
    	SharedPreferences settings = getSharedPreferences("org.landroo.colorizer_preferences", MODE_PRIVATE);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString("table", this.colorizerList[activePlayerNo].getFields());
    	editor.commit();
    }

	@Override
	public void onFingerChange()
	{
		// TODO Auto-generated method stub
		
	}
	
	private void login()
	{
		if (http == null)
		{
			try
			{
				http = new HttpServer(HTTP_PORT, handler);
			}
			catch (Exception e)
			{
				Log.i(TAG, e.getMessage());
			}
		}

		if (playerName.equals("Player")) Toast.makeText(this, sNameError, Toast.LENGTH_LONG).show();
		else if (webClass.loggedIn) Toast.makeText(this, sAlredyError, Toast.LENGTH_LONG).show();
		else webClass.login();

		return;
	}
	
	private void processUpdate(String reply)
	{
		String[] arr = reply.split(":", -1);

		try
		{
			synchronized (this)
			{
				// steps: roli;5;10;miki;18;33;
//				if (!arr[0].equals("")) procStep(arr[0]);

				// messages: feri;Hali;laci;szia
//				if (!arr[1].equals("")) procMessage(arr[1]);

				// invites: laci;1280;720;40;
//				if (!arr[2].equals("")) procInvite(arr[2]);

				// added: miki;
//				if (!arr[3].equals("")) procAddUser(arr[3]);

				// removed: tibi;
//				if (!arr[4].equals("")) procRemoveUser(arr[4]);

				// newtable: tibi;
//				if (!arr[5].equals("")) procNewTable(arr[5]);

				// undo:
//				if (!arr[6].equals("")) procUndo(arr[6]);
			}
		}
		catch (Exception ex)
		{
			Log.i(TAG, reply);
		}
		return;
	}	
	
	private void logout()
	{
		if (http != null) http.stop();
		if (webClass != null && webClass.loggedIn)
		{
			drawPlayer(0, 0);

			webClass.logout();
			for (int j = 1; j < ColorizerActivity.MAX_PARTNER; j++)
				this.colorizerList[j] = null;
			activePlayerNo = 0;

			colorizerview.postInvalidate();

			System.gc();
		}

		return;
	}
	
	private boolean checkWifi()
	{
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) return true;
		
		return false;
	}
	
	private void drawPlayer(int iPly, int turn)
	{
		if (colorizerList[iPly] != null)
		{


			colorizerview.postInvalidate();
		}

		return;
	}

	private void userlist()
	{
		if (webClass.loggedIn)
		{
			ArrayList<String> activeUsers = new ArrayList<String>();
			for (int i = 1; i < this.colorizerList.length; i++)
				if (this.colorizerList[i] != null) activeUsers.add(this.colorizerList[i].playerName);
			webClass.activeUsers = activeUsers;
			webClass.showUserListDialog(this);
		}
		else Toast.makeText(this, sFirstError, Toast.LENGTH_LONG).show();

		return;
	}	
}
