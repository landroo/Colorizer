package org.landroo.colorizer;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.Log;

// Colorizer osztály	
public class ColorizerClass
{
	private static final String TAG = "ColorizerClass";

	private Bitmap bitmap = null;			// A canvas graphics tagja
	
	public int miRectSize = 0;				// A négyzet mérete
	public int miFullNum = 0;				// A trapézok száma
	public int miColNum = 0;				// A szomszéd szín száma
	public String msLastCols = "";			// Az utolsó foglalt színek
	public String msEndPlayers = "";		// Finished players
	
	public int miRectMaxX = 0;				// A játék tábla szélesség
	public int miRectMaxY = 0;				// A játék tábla magassága
			
	private int miOffsetX = 0;				// A rajzolás kezdő X pozíciója
	private int miOffsetY = 0;				// A rajzolás kezdő Y pozíciója
	
	private int[][] maTable = null;			// Tábla mezői
	private int[][] maUndoTable = null;		// Undo table
	
	private int miNumOfPlayres = 2;			// A játékosok száma
	
	public String playerName;// player name
	public Bitmap button1;// button normal
	public Bitmap button2;// button 
	public Bitmap button3;// button	
	public boolean newEvent = false;
			
	// Konstruktor
	public ColorizerClass(int rectSize, int iPlyNum)
	{
		this.miRectSize = rectSize;
		this.miNumOfPlayres = iPlyNum;
		
		this.msEndPlayers = "";
	}
	
	// Új játék kezdete vagy újrarajzolás
	public Bitmap drawGame(int width, int height, String sFields)
	{
		int w = 0;
		int h = 0;
		int x = 0;
		int y = 0;
		int i = 0;
		
		w = width - (width % this.miRectSize);
		h = height - (height % this.miRectSize);
		
		if(width % this.miRectSize == 0) w -= this.miRectSize / 2;
		if(height % this.miRectSize == 0) h -= this.miRectSize / 2;

		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(getColor(Color.WHITE));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		
		RectF rect = new RectF(0, 0, w, h);
		canvas.drawRect(rect, paint);
		
		this.miRectMaxX = w / this.miRectSize;
		this.miRectMaxY = (h / this.miRectSize) * 2;

		this.miOffsetX = (w % this.miRectSize) / 2;
		this.miOffsetY = (w % this.miRectSize) / 2; 
		
		// set the table field to random color numbers 1 to 7
		this.maTable = new int[this.miRectMaxX][this.miRectMaxY];
		this.maUndoTable = new int[this.miRectMaxX][this.miRectMaxY];
		if(sFields.equals(""))
		{
			for(x = 0; x < this.miRectMaxX; x++)
			{
				for(y = 0; y < this.miRectMaxY; y++)
				{
					this.maTable[x][y] = getRandColor(-1);
					this.maUndoTable[x][y] = this.maTable[x][y];
				}
			}
		}
		else setFields(sFields);
		
		// sign the unused fields with -1
		for(i = 0; i < this.miRectMaxX; i++) this.maTable[i][this.miRectMaxY - 1] = -1;
		for(i = 1; i < this.miRectMaxY; i += 2) this.maTable[this.miRectMaxX - 1][i] = -1;
		
		// set the start position of the players
		this.setStPos();
		
		this.miFullNum = 0;
		
		// draw the whole table and calculate the cell number
		for(x = 0; x < this.miRectMaxX; x++)
		{
			for(y = 0; y < this.miRectMaxY; y++)
			{
				if(this.maTable[x][y] != -1)
				{
					drawCell(x, y, this.maTable[x][y]);
					this.miFullNum++;
				}
			}
		}
		
		return bitmap;
	}
	
	private void drawCell(int x, int y, int color)
	{
		int px = x *  this.miRectSize;
		int py = y / 2 *  this.miRectSize;
		if(y % 2 == 1)
		{
			py += this.miRectSize;
			drawPoly(color, px + this.miRectSize / 2 + this.miOffsetX, py - this.miRectSize / 2 + this.miOffsetY, this.miRectSize, this.miRectSize);
		}
		else drawPoly(color, px + this.miOffsetX, py + this.miOffsetY, this.miRectSize, this.miRectSize);
	}
	
	// Trapéz kirajzolása
	private void drawPoly(int color, int x, int y, int h, int w)
	{
		Canvas canvas = new Canvas(bitmap);
		
		Paint paint = new Paint();
		paint.setColor(getColor(color));
		paint.setAntiAlias(true);

		Path path = new Path();
		path.moveTo(x + w / 2, y);
		path.lineTo(x + w, y + h / 2);
		path.lineTo(x + w / 2, y + h);
		path.lineTo(x, y + h / 2);
		path.lineTo(x + w / 2, y);

		canvas.drawPath(path, paint);
		
		paint.setStyle(Paint.Style.STROKE);
		if(color == 0) paint.setColor(Color.WHITE);
		else paint.setColor(Color.BLACK);
		paint.setStrokeWidth(1);
		
		canvas.drawPath(path, paint);
	}
	
	// 7 véletlen
	private int getRandColor(int iEx)
	{
		int iRetCol = this.random(1, 7, 1);
		
		if(iRetCol == iEx) getRandColor(iEx);
			
		return iRetCol;
	}
	
	// Szín érték szinkódból
	private int getColor(int iColNum)
	{
		int uRetCol = 0; 
		
		iColNum = iColNum % 10;
		
		switch(iColNum)
		{
			case 1:	// Red
				uRetCol = Color.RED;
				break;
			case 2:	// Green
				uRetCol = Color.GREEN;
				break;
			case 3:	// Blue
				uRetCol = Color.BLUE;
				break;
			case 4:	// Magenta
				uRetCol = Color.MAGENTA;
				break;
			case 5:	// Yellow
				uRetCol = Color.YELLOW;
				break;
			case 6:	// Cyan
				uRetCol = Color.CYAN;
				break;
			case 7:	// White
				uRetCol = Color.WHITE;
				break;
			case 8:	// Grey
				uRetCol = Color.GRAY;
				break;
			default:
				uRetCol = Color.BLACK;
		}
			
		return uRetCol;
	}
	
	// undo last colors
	public void undo()
	{
		int x;
		int y;
		
		for(x = 0; x < this.miRectMaxX ; x++)
			for(y = 0; y < this.miRectMaxY; y++)
				 this.maTable[x][y] = maUndoTable[x][y];
		
		// draw the whole table and calculate the cell number
		for(x = 0; x < this.miRectMaxX; x++)
			for(y = 0; y < this.miRectMaxY; y++)
				if(this.maTable[x][y] != -1)
					drawCell(x, y, this.maTable[x][y]);
	}

	// A kiválsztott szín beálítása
	public void changeColor(int iPly, int newColor, boolean bSet)
	{
		int x = 0;
		int y = 0;
		
		this.miColNum = 0;
		
		if(iPly == 1)
		{
			for(x = 0; x < this.miRectMaxX ; x++)
				for(y = 0; y < this.miRectMaxY; y++)
					maUndoTable[x][y] = this.maTable[x][y];
		}
		
		for(x = 0; x < this.miRectMaxX ; x++)
		{
			for(y = 0; y < this.miRectMaxY; y++)
			{
				// Választott színű üres mező
				if(this.maTable[x][y] % 10 == newColor && this.maTable[x][y] / 10 == 0)
				{
					if(y % 2 == 0)
					{
						// Falfent [x - 1][y - 1]
						if(x > 0 && y > 0 && this.maTable[x - 1][y - 1] / 10 == iPly) checkNext(x, y, iPly, newColor, bSet);
						// Ballent [x - 1][y + 1]
						if(x > 0 && y < this.miRectMaxY - 2 && this.maTable[x - 1][y + 1] / 10 == iPly) checkNext(x, y, iPly, newColor, bSet);
					}
					else
					{
						// Falfent [x + 1][y - 1]
						if(x < this.miRectMaxX - 1 && y > 0 && this.maTable[x + 1][y - 1] / 10 == iPly) checkNext(x, y, iPly, newColor, bSet);
						// Ballent [x + 1][y + 1]
						if(x < this.miRectMaxX - 1 && y < this.miRectMaxY - 2 && this.maTable[x + 1][y + 1] / 10 == iPly) checkNext(x, y, iPly, newColor, bSet);
					}
					// Jobb v. bal fent [x][y - 1]
					if(y > 0 && this.maTable[x][y - 1] / 10 == iPly) checkNext(x, y, iPly, newColor, bSet);
					// Jobb v. bal lent [x][y + 1]
					if(y < this.miRectMaxY - 2 && this.maTable[x][y + 1] / 10 == iPly) checkNext(x, y, iPly, newColor, bSet);
				}
				
				// Saját mezők
				if(this.maTable[x][y] / 10 == iPly && this.maTable[x][y] % 10 != newColor && bSet)
				{
					if(this.maTable[x][y] != iPly * 10 + newColor) drawCell(x, y, newColor);
					this.maTable[x][y] = iPly * 10 + newColor;
				}
			}
		}
		
		if(iPly == 1) this.msLastCols = "" + newColor;
	}
	
	// Mező kisajátító rekurzió
	private boolean checkNext(int x, int y, int iPly, int newColor, boolean bSet)
	{
		if(bSet) this.maTable[x][y] = this.maTable[x][y] % 10 + iPly * 10;
		this.miColNum++;
		
		if(y % 2 == 0)
		{
			// Balfent [x - 1][y - 1]
			if(x > 0 && y > 0 && (int)(this.maTable[x - 1][y - 1] / 10) == 0 && this.maTable[x - 1][y - 1] % 10 == newColor)
			{
				if(this.maTable[x - 1][y - 1] != iPly * 10 + newColor) drawCell(x - 1, y - 1, newColor);
				this.maTable[x - 1][y - 1] = iPly * 10 + newColor;
				checkNext(x - 1, y - 1, iPly, newColor, bSet);
				if(!bSet) this.maTable[x - 1][y - 1] = newColor;
			}
			// Ballent [x - 1][y + 1]
			if(x > 0 && y < this.miRectMaxY - 2 && (int)(this.maTable[x - 1][y + 1] / 10) == 0 && this.maTable[x - 1][y + 1] % 10 == newColor)
			{
				if(this.maTable[x - 1][y + 1] != iPly * 10 + newColor) drawCell(x - 1, y + 1, newColor);
				this.maTable[x - 1][y + 1] = iPly * 10 + newColor;
				checkNext(x - 1, y + 1, iPly, newColor, bSet);
				if(!bSet) this.maTable[x - 1][y + 1] = newColor;
			}
		}
		else
		{
			// Balfent [x + 1][y - 1]
			if(x < this.miRectMaxX - 1 && y > 0  && (int)(this.maTable[x + 1][y - 1] / 10) == 0 && this.maTable[x + 1][y - 1] % 10 == newColor)
			{
				if(this.maTable[x + 1][y - 1] != iPly * 10 + newColor) drawCell(x + 1, y - 1, newColor);
				this.maTable[x + 1][y - 1] = iPly * 10 + newColor;
				checkNext(x + 1, y - 1, iPly, newColor, bSet);
				if(!bSet) this.maTable[x + 1][y - 1] = newColor;
			}
			// Ballent [x + 1][y + 1]
			if(x < this.miRectMaxX - 1 && y < this.miRectMaxY - 2 && (int)(this.maTable[x + 1][y + 1] / 10) == 0 && this.maTable[x + 1][y + 1] % 10 == newColor)
			{
				if(this.maTable[x + 1][y + 1] != iPly * 10 + newColor) drawCell(x + 1, y + 1, newColor);
				this.maTable[x + 1][y + 1] = iPly * 10 + newColor;
				checkNext(x + 1, y + 1, iPly, newColor, bSet);
				if(!bSet) this.maTable[x + 1][y + 1] = newColor;
			}
		}
		// Jobb v. bal fent [x][y - 1]
		if(y > 0 && (int)(this.maTable[x][y - 1] / 10) == 0 && this.maTable[x][y - 1] % 10 == newColor)
		{
			if(this.maTable[x][y - 1] != iPly * 10 + newColor) drawCell(x, y - 1, newColor);
			this.maTable[x][y - 1] = iPly * 10  + newColor;
			checkNext(x, y - 1, iPly, newColor, bSet);
			if(!bSet) this.maTable[x][y - 1] = newColor;
		}
		// Jobb v. bal lent [x][y + 1]
		if(y < this.miRectMaxY - 2 && (int)(this.maTable[x][y + 1] / 10) == 0 && this.maTable[x][y + 1] % 10 == newColor)
		{
			if(this.maTable[x][y + 1] != iPly * 10 + newColor) drawCell(x, y + 1, newColor);
			this.maTable[x][y + 1] = iPly * 10 + newColor;
			checkNext(x, y + 1, iPly, newColor, bSet);
			if(!bSet) this.maTable[x][y + 1] = newColor;
		}
		
		return true;
	}
	
	/**
	 * Count the cells for each owner
	 * @return
	 */
	public int[] getFullNum()
	{
		int[] aOwnNum = new int[6];
		for(int i = 0; i < 5 ; i++)	aOwnNum[i] = 0;
		aOwnNum[5] = this.miFullNum;			
		for(int x = 0; x < this.miRectMaxX ; x++)
			for(int y = 0; y < this.miRectMaxY; y++)
				if(this.maTable[x][y] != -1)
					aOwnNum[(int)(this.maTable[x][y] / 10)]++;
		
		return aOwnNum;
	}
	
	/**
	 * Droid steps
	 * @param iPlayer
	 * @return 	0 when selected color is unavailable otherwise the selected color  
	 */
	public int colorizerAI(int iPlayer)
	{
		int iMax = 0;
		int iCol = 0;
		int iColDir = getOppDir(iPlayer);
		int iColNum = 0;
		int[] aColors = new int[8];
		int stepNo = 0;
		// Number of surroundings color 1-7  
		for(int i = 1; i < 8 ; i++)
		{
			changeColor(iPlayer, i, false);
			aColors[i] = this.miColNum;
			stepNo += aColors[i]; 
		}
		//Log.i(TAG, "R1-" + aColors[1] + " G2-" + aColors[2] + " B3-" + aColors[3] + " M4-" + aColors[4] + " Y5-" + aColors[5] + " C6-" + aColors[6] + " W7-" + aColors[7]);
		
		// select the highest number if it is not used 1-7
		for(int i = 1; i < 8 ; i++)
		{
			if(this.msLastCols.indexOf("" + i) == -1)
			{
				if(aColors[i] > iMax)
				{
					iMax = aColors[i];
					iColNum = i;
				}
				else if(aColors[i] == iMax && Math.random() < .5) iColNum = i;
			}
		}

		iCol = iColNum;
		if(iColDir > 0 && this.msLastCols.indexOf("" + iColDir) == -1) iCol = iColDir;
		if(iColNum > 10) iCol = iColNum;
		if(stepNo == 0) iCol = 0;
		//Log.i(TAG, "iColNum:" + iColNum + " iColDir:" + iColDir + " iCol:" + iCol + " stepNo:" + stepNo);
		
		changeColor(iPlayer, iCol, true);
		
		this.msLastCols += "" + iCol;
		
		return iCol;	
	}
	
	/**
	 * Set the start postitions of the players
	 */
	private void setStPos()
	{
		// max four, one player three droid 
		for(int i = 1; i <= this.miNumOfPlayres; i++)
		{
			switch(i)
			{
				case 1:	// left dbottom the plyer
					this.maTable[0][this.miRectMaxY - 2] = i * 10;
					break;
				case 2:	// right up droid 1
					this.maTable[this.miRectMaxX - 1][0] = i * 10; 
					break;
				case 3: // left up droid 2
					this.maTable[0][0] = i * 10; 
					break;
				case 4: // rigth bottom droid 3
					this.maTable[this.miRectMaxX - 1][this.miRectMaxY - 2] = i * 10;
					break;
			}
		}
		
		return;
	}
	
	// Melleti mező visszaadása
	// 1 balfent, 2 jobfent, 3 joblent, 4 ballent
	private cell getNextCell(int x, int y, int iDir)
	{
		cell iRet = new cell();
		
		if(y % 2 == 0)
		{
			// Balfent [x - 1][y - 1]
			if(x > 0 && y > 0 && iDir == 1)
			{
//				iRet.x = x - 1;
//				iRet.y = x - 1;
				iRet.o = (int)(this.maTable[x - 1][y - 1] / 10);
				iRet.c = this.maTable[x - 1][y - 1] % 10;
			}
			// Jobbfent [x][y - 1]
			if(y > 0 && iDir == 2)
			{
//				iRet.x = x;
//				iRet.y = y - 1;
				iRet.o = (int)(this.maTable[x][y - 1] / 10); 
				iRet.c = this.maTable[x][y - 1] % 10;
			}
			// Jobblent [x][y + 1]
			if(y < this.miRectMaxY - 2 && iDir == 3)
			{
//				iRet.x = x;
//				iRet.y = y + 1;
				iRet.o = (int)(this.maTable[x][y + 1] / 10);
				iRet.c = this.maTable[x][y + 1] % 10;
			}
			// Ballent [x - 1][y + 1]
			if(x > 0 && y < this.miRectMaxY - 2 && iDir == 4)
			{
//				iRet.x = x - 1;
//				iRet.y = y + 1;
				iRet.o = (int)(this.maTable[x - 1][y + 1] / 10);
				iRet.c = this.maTable[x - 1][y + 1] % 10;
			}
		}
		else
		{
			// Balfent [x][y - 1]
			if(y > 0 && iDir == 1)
			{
//				iRet.x = x;
//				iRet.y = y - 1;
				iRet.o = (int)(this.maTable[x][y - 1] / 10); 
				iRet.c = this.maTable[x][y - 1] % 10;
			}
			// Jobbfent [x + 1][y - 1]
			if(x < this.miRectMaxX - 1 && y > 0  && iDir == 2)
			{
//				iRet.x = x + 1;
//				iRet.y = y - 1;
				iRet.o = (int)(this.maTable[x + 1][y - 1] / 10);
				iRet.c = this.maTable[x + 1][y - 1] % 10;
			}
			// Jobblent [x + 1][y + 1]
			if(x < this.miRectMaxX - 1 && y < this.miRectMaxY - 2 && iDir == 3)
			{
//				iRet.x = x + 1;
//				iRet.y = y + 1;
				iRet.o = (int)(this.maTable[x + 1][y + 1] / 10);
				iRet.c = this.maTable[x + 1][y + 1] % 10;
			}
			// Ballent [x][y + 1]
			if(y < this.miRectMaxY - 2 && iDir == 4)
			{
//				iRet.x = x;
//				iRet.y = y + 1;
				iRet.o = (int)(this.maTable[x][y + 1] / 10);
				iRet.c = this.maTable[x][y + 1] % 10;
			}
		}
		
		return iRet;
	}
	
	// Az átlós oldali felé mutató mező
	private int getOppDir(int iPly)
	{
		cell o = new cell();
		int x = 0;
		int y = 0;
		int iCol = 0;
		
		float nAngle = 0;
		float nDestAng = 0;
		
		float nDist = 0;
		float nLastDist = 0;
		
		switch(iPly)
		{
			case 2:	// Jobb fent
				nDestAng = this.getViewAngDist(this.miRectMaxX - 1, 0, 0, this.miRectMaxY - 2, true);
				nLastDist = this.getViewAngDist(this.miRectMaxX - 1, 0, 0, this.miRectMaxY - 2, false);					
				for(x = 0; x < this.miRectMaxX ; x++)
				{
					for(y = 0; y < this.miRectMaxY; y++)
					{
						if((int)(this.maTable[x][y] / 10) == iPly)
						{
							nAngle = this.getViewAngDist(x, y, 0, this.miRectMaxY - 2, true);
							nDist = this.getViewAngDist(x, y, 0, this.miRectMaxY - 2, false);
							o = getNextCell(x, y, 4);
							if(nDestAng + 0.01 > nAngle && nDestAng - 0.01 < nAngle && o.o == 0 && this.msLastCols.indexOf(o.c) == -1 && nDist < nLastDist)
							{
								iCol = o.c;
								nLastDist = nDist;
							}
						}
					}
				}
				break;
			case 3:	// Bal fent
				nDestAng = this.getViewAngDist(0, 0, this.miRectMaxX - 1, this.miRectMaxY - 2, true);
				nLastDist = this.getViewAngDist(0, 0, this.miRectMaxX - 1, this.miRectMaxY - 2, false);					
				for(x = 0; x < this.miRectMaxX ; x++)
				{
					for(y = 0; y < this.miRectMaxY; y++)
					{
						if((int)(this.maTable[x][y] / 10) == iPly)
						{
							nAngle = this.getViewAngDist(x, y, this.miRectMaxX - 1, this.miRectMaxY - 2, true);
							nDist = this.getViewAngDist(x, y, this.miRectMaxX - 1, this.miRectMaxY - 2, false);
							o = getNextCell(x, y, 3);
							if(nDestAng + 0.01 > nAngle && nDestAng - 0.01 < nAngle && o.o == 0 && this.msLastCols.indexOf(o.c) == -1 && nDist < nLastDist)
							{
								iCol = o.c;
								nLastDist = nDist;
							}
						}
					}
				}
				break;
			case 4:	// Jobb lent
				nDestAng = this.getViewAngDist(this.miRectMaxX - 1, this.miRectMaxY - 2, 0, 0, true);
				nLastDist = this.getViewAngDist(this.miRectMaxX - 1, this.miRectMaxY - 2, 0, 0, false);					
				for(x = 0; x < this.miRectMaxX ; x++)
				{
					for(y = 0; y < this.miRectMaxY; y++)
					{
						if((int)(this.maTable[x][y] / 10) == iPly)
						{
							nAngle = this.getViewAngDist(x, y, 0, 0, true);
							nDist = this.getViewAngDist(x, y, 0, 0, false);
							o = getNextCell(x, y, 1);
							if(nDestAng + 0.01 > nAngle && nDestAng - 0.01 < nAngle && o.o == 0 && this.msLastCols.indexOf(o.c) == -1 && nDist < nLastDist)
							{
								iCol = o.c;
								nLastDist = nDist;
							}
						}
					}
				}
				break;
		}

		return iCol;
	}
	
	// Szöben látszik vagy távolság
	private float getViewAngDist(int x1, int y1, int x2, int y2, boolean bMode)
    {

        int nDelX = x2 - x1;
        int nDelY = y2 - y1;

        float nde = 0;

        float nDist = (float)Math.sqrt(nDelY * nDelY + nDelX * nDelX);

        if(nDelX != 0)
        {
            nde = 2 * (float)Math.PI;
            nde = nde + (float)Math.atan(nDelY / nDelX);
            if(nDelX <= 0)
            {
                nde = (float)Math.PI;
                nde = nde + (float)Math.atan(nDelY / nDelX);
            }
            else
            {
                if(nDelY >= 0)
                {
                    nde = 0;
                    nde = nde + (float)Math.atan(nDelY / nDelX);
                }
            }
        }
        else
        {
            if(nDelY == 0)
                nde = 0;
            else
            {
                if(nDelY < 0)
                    nde = (float)Math.PI;
                nde = nde + (float)Math.PI / 2;
            }
        }

        // Radian
        float nAngle = nde;
        //	Angle = de / Math.PI * 180;

		if(bMode) return nAngle;
        else return nDist;
    }
    
    // Játékmező megadása
    public void setFields(String sFields)
    {
    	int iCnt = 0;
    	String[] sArr = sFields.split(";");

		for(int x = 0; x < this.miRectMaxX; x++)		
			for(int y = 0; y < this.miRectMaxY; y++)				
				this.maTable[x][y] = Integer.parseInt(sArr[iCnt++]);
    		
    	return;
    }
    
    // Játékmező lekérése
    public String getFields()
    {
    	String sFields = "";
    	
		// Játékmezők kiírása
		for(int x = 0; x < this.miRectMaxX; x++)
			for(int y = 0; y < this.miRectMaxY; y++)
				sFields += this.maTable[x][y] + ";";
		
    	return sFields;
    }

    // Megfordítja a tábla mezőit
    public boolean reverseTable()
    {
    	String sTmp = "";
    	int x = 0;
    	int y = 0;
    	int i = 0;
    	int iPly = 0;
    	
    	// serialize the table
		for(y = 0; y < this.miRectMaxY; y++)
		{
			for(x = 0; x < this.miRectMaxX; x++)
			{
				if(this.maTable[x][y] != -1) sTmp += "" + (this.maTable[x][y] % 10);
			}
		}
		
		// fill the table from the end of the string
		for(y = 0; y < this.miRectMaxY; y++)
		{
			for(x = 0; x < this.miRectMaxX; x++)
			{
				if(this.maTable[x][y] != -1)
				{
					iPly = (int)(this.maTable[x][y] / 10) * 10; 
					this.maTable[x][y] = iPly + Integer.parseInt(sTmp.substring(sTmp.length() - i - 1, sTmp.length() - i));
					i++;
				}
			}
		}
		
		this.setStPos();
		
    	return true;
    }
    
	// Véletlenszám (from ACS)        
    public int random(int nMinimum, int nMaximum, int nRoundToInterval) 
	{
		if(nMinimum > nMaximum) 
		{
			int nTemp = nMinimum;
			nMinimum = nMaximum;
			nMaximum = nTemp;
		}
	
		int nDeltaRange = (nMaximum - nMinimum) + (1 * nRoundToInterval);
		double nRandomNumber = Math.random() * nDeltaRange;
	
		nRandomNumber += nMinimum;
		
		int nRet = (int)(Math.floor(nRandomNumber / nRoundToInterval) * nRoundToInterval);
	
		return nRet;
	} 
	
	public void Players(int iPlyNum)
	{
		if(iPlyNum > 0 && iPlyNum < 5) this.miNumOfPlayres = iPlyNum;
	}
	
	public Bitmap creteButton(int w, int h, int color, boolean pressed, boolean border)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(Color.TRANSPARENT);		
		Canvas canvas = new Canvas(bitmap);
		
		int[] colors = new int[3];
		if(pressed)
		{
			colors[0] = color;
			colors[1] = 0xFFFFFFFF;
			colors[2] = color;
		}
		else
		{
			colors[0] = 0xFFFFFFFF;
			colors[1] = color;
			colors[2] = 0xFFFFFFFF;
		}
		
		float[] pos = new float[3];
		pos[0] = 0f;
		pos[1] = 0.5f;
		pos[2] = 1f;
		
		LinearGradient gradient = new LinearGradient(0f, 0f, 0, (float)h, colors, pos, TileMode.CLAMP);
		
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAntiAlias(true);
		paint.setShader(gradient);
		
		RectF rect = new RectF(0, 0, w, h);
		if(border == false) canvas.drawRoundRect(rect, w / 5, h / 5, paint);
		
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(1);
		
		canvas.drawRoundRect(rect, w / 5, h / 5, paint);
		
		return bitmap;
	}
	
	private class cell
	{
//		public int x;
//		public int y;
		public int o;
		public int c;
	}
	
	public Bitmap getBackGround(int w, int h, Resources res)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		//bitmap.eraseColor(0xFFFF0000);
		Canvas canvas = new Canvas(bitmap);
		RectF rect = new RectF();

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);

		int color1 = 0xFF222222;
		int color2 = 0xFF882222;

		int[] colors = new int[2];
		colors[0] = color1;
		colors[1] = color2;

		float bw = w / 10;
		float bh = h / 40;
		float gap = w / 200;

		LinearGradient grad;

		for (int i = 0; i < 11; i++)
		{
			for (int j = 0; j < 40; j++)
			{
				if (random(0, 1, 1) == 1)
				{
					colors[0] = color1;
					colors[1] = color2 + random(0, 3, 1) * 0x1100;
				}
				else
				{
					colors[1] = color1;
					colors[0] = color2 + random(0, 3, 1) * 0x1100;
				}

				if (j % 2 == 0)
				{
					grad = new LinearGradient(i * bw, j * bh, i * bw + bw, j * bh + bh, colors, null,
							android.graphics.Shader.TileMode.REPEAT);
					rect.set(i * bw + gap, j * bh + gap, i * bw + bw - gap, j * bh + bh - gap);
				}
				else
				{
					grad = new LinearGradient(i * bw - bw / 2, j * bh, i * bw + bw - bw / 2, j * bh + bh, colors, null,
							android.graphics.Shader.TileMode.REPEAT);
					rect.set(i * bw + gap - bw / 2, j * bh + gap, i * bw + bw - gap - bw / 2, j * bh + bh - gap);
				}
				paint.setShader(grad);
				canvas.drawRect(rect, paint);
			}
		}
		
		paint.setShader(null);
		paint.setAlpha(64);
		
		float width = w / 2;
		float height = h / 2;
		
		float x, y, r, scaleWidth, scaleHeight;
		Bitmap img;

		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10) * 2;
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.jewel1), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		canvas.drawBitmap(img, x, y, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10);
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.piper), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		canvas.drawBitmap(img, x + width, y + height, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10);
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.jewel2), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		r = random(0, 7, 1) * 45;
		canvas.drawBitmap(rotImage(img, r), x + width, y, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10) * 2;
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10) / 2;
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.amoba), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		r = random(0, 7, 1) * 45;
		canvas.drawBitmap(rotImage(img, r), x, y + height, paint);

		return bitmap;
	}
	
	private Bitmap rotImage(Bitmap img, float rot)
	{
		int origWidth = img.getWidth();
		int origHeight = img.getHeight();
		Matrix matrix = new Matrix();
		matrix.setRotate(rot, img.getWidth() / 2, img.getHeight() / 2);
		Bitmap outImage = Bitmap.createBitmap(img, 0, 0, origWidth, origHeight, matrix, false);
		
		return outImage;
	}
	
	public void setName(String name)
	{
		playerName = name;

		int color = getColor(random(1, 8, 1));

		button1 = creteButton(120, 40, color, false, false, name);
		button2 = creteButton(120, 40, color, true, false, name);
		button3 = creteButton(120, 40, color, false, true, name);
	}
	
	private Bitmap creteButton(int w, int h, int color, boolean pressed, boolean border, String text)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);
		int borderColor = Color.WHITE;

		int[] colors = new int[3];
		if (pressed)
		{
			colors[0] = color;
			colors[1] = 0xFFFFFFFF;
			colors[2] = color;
		}
		else
		{
			colors[0] = 0xFFFFFFFF;
			colors[1] = color;
			colors[2] = 0xFFFFFFFF;
		}

		float[] pos = new float[3];
		pos[0] = 0f;
		pos[1] = 0.5f;
		pos[2] = 1f;

		LinearGradient gradient = new LinearGradient(0f, 0f, 0, (float) h, colors, pos, TileMode.CLAMP);

		Paint paint = new Paint();
		paint.setColor(color);
		paint.setAntiAlias(true);
		paint.setShader(gradient);

		RectF rect = new RectF(0, 0, w, h);
		if (border == false) canvas.drawRoundRect(rect, w / 5, h / 5, paint);

		//
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(borderColor);
		paint.setStrokeWidth(5);
		paint.setShader(null);

		canvas.drawRoundRect(rect, w / 5, h / 5, paint);

		// draw text
		paint.setTextSize(20);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		paint.setFakeBoldText(true);
		paint.setShadowLayer(3, 0, 0, Color.BLACK);

		float f = paint.measureText(text);
		canvas.drawText(text, (w - f) / 2, 26, paint);

		return bitmap;
	}	
	
}
