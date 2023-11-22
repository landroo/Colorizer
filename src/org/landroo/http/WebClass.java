package org.landroo.http;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.landroo.colorizer.R;
import org.landroo.colorizer.R.string;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;

public class WebClass implements Runnable
{
	private static final String TAG = "WebClass";
	private static final String error = "error";
	
	private EditText editText;							//
	private AlertDialog alertOnTap;						//
	private	String sText = "";
	
	private ArrayList<CharSequence> selUsers = new ArrayList<CharSequence>();
	public ArrayList<Partner> userList = new ArrayList<Partner>();
	public ArrayList<String> activeUsers = new ArrayList<String>();
	
	public String mainip = "";			// 
	public String partnerip = "";		// partner ip
	public String game = "";			// game type (colorizer)
	public String myname = "";			// my player name
	public String myip = ""; 			// my ip
	public int command = 0;				// last command
	public int httpport = 8484;			// direct port
	
	public boolean showDialog = false;
	public Handler handler;
	public int polling = 5000;
	public int size = 40;
	public int width = 480;
	public int height = 800;
	
	private Thread thread;
//	private ProgressDialog pd;
	
	private Timer timer = null;
	private Context context;
	private String params = "";
	
	public boolean loggedIn = false;
	
	private boolean inRequest = false;
	
	// partner class for direct communication
	public class Partner
	{
		public String name;
		public String ip;
		
		public Partner(String n, String addr)
		{
			name = n;
			ip = addr;
		}
	}
	
	// Constructor
	public WebClass(String sUrl, String sGame, String sName, Handler cHandler, int port)
	{
		mainip = sUrl;
		game = sGame;
		myname = sName;
		httpport = port;
		
		this.handler = cHandler;
		
        timer = new Timer();
		timer.scheduleAtFixedRate(new netTimer(), 0, polling);
	}
	
	// 1 login
	// in: http://192.168.0.1105:8080/?game=amoba&name=test&command=1;129.168.0.191
	// out: miki;feri;laci;
	public void login()
	{
		if(!loggedIn)
		{
			myip = getLocalIpAddress();
			command = 1;
			params = myip + ";"; 
			sendCommand();
		}
	}
	
	// 2 ping
	// in:  http://192.168.0.122:8080/?game=amoba&name=dani&command=2
	// out: roli;5;10;miki;18;33;:feri;Hali;:laci;1280;720;40;
	public void poll()
	{
		if(loggedIn)
		{
			command = 2;
			sendCommand();
		}
	}
	
	// 3 get userlist
	public void users()
	{
		// TODO upgrade full user list
	}
	
	// 4 invite: 4;laci;1280;960;40;
	public void invite(String sUsers, String ip)
	{
		if(loggedIn)
		{
			if(checkServer(ip)) partnerip = ip + ":" + httpport;
			command = 4;
			params = sUsers;
			sendCommand();
		}
	}
	
	// 5 step: 5;laci;52;37;
	public void step(String name, String ip, int x, int y)
	{
		if(loggedIn)
		{
			if(checkServer(ip)) partnerip = ip + ":" + httpport;
			command = 5;
			params = name + ";" + x + ";" + y + ";"; 			
			sendCommand();
		}
	}
	
	// command 6 message
	// http://192.168.0.105:8080/?game=amoba&name=dani&command=6;landroo;Hali
	public void message(String sMessages)
	{
		if(loggedIn)
		{
			StringBuffer sb = new StringBuffer();
			String ip = "";
			int iCnt = 0;
			for(Partner partner: userList)
			{
				if(selUsers.contains(partner.name))
				{
			    	sb.append(partner.name);
			    	sb.append(";");
			    	sb.append(sMessages);
			    	sb.append(";");
			    	
			    	ip = partner.ip;
			    	iCnt++;
				}
			}
			if(iCnt == 1) partnerip = ip + ":" + httpport;
	    	params = sb.toString();
			
			command = 6;
			sendCommand();
		}
	}
	
	// command 7 logout: 7
	public void logout()
	{
		if(loggedIn)
		{
			this.loggedIn = false;
			command = 7;
			sendCommand();
		}
	}
	
	// 8 new table: 8;laci;
	public void newtable(String sUser)
	{
		if(loggedIn)
		{
			command = 8;
			params = sUser;
			sendCommand();
		}
	}
	
	// 9 undo: 9;laci;
	public void undo(String sUser)
	{
		if(loggedIn)
		{
			command = 9;
			params = sUser;
			sendCommand();
		}
	}
	
	public void sendCommand()
	{
		//if(showDialog && context != null) pd = ProgressDialog.show(context, "Wait!", "Processing data.", true, false);
		thread = new Thread(this);
        thread.start();
	}
	
	private String sendRequest(int iCommand) 
	{
		String result = "&";
		HttpURLConnection con = null;
		StringBuffer answer = new StringBuffer();
		
		inRequest = true;
		
		// http://192.168.0.105:8080/?game=amoba&name=zte&command=1;
		String urlAddress = "";
		String ip = partnerip.equals("") ? mainip : partnerip;
		try
		{
			urlAddress = "http://" + ip + "/?game=" + game + "&name=" + URLEncoder.encode(myname, "ISO-8859-1") + "&command=" + iCommand + ";" + URLEncoder.encode(params, "ISO-8859-1");
		}
		catch (UnsupportedEncodingException e)
		{
			Log.i(TAG, e.getMessage());
		} 
		
		synchronized(this) 
		{
			try 
			{
				// Check if task has been interrupted
				if(Thread.interrupted()) throw new InterruptedException();
	
				URL url = new URL(urlAddress);
				con = (HttpURLConnection) url.openConnection();
				con.setReadTimeout(10000);
				con.setConnectTimeout(15000);
				con.setRequestMethod("GET");
	        	con.setDoOutput(true);
	
		         // Start the query
				con.connect();
	
	            // Get the response
	            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
	            String line;
	            while ((line = reader.readLine()) != null) answer.append(line);
				reader.close();
				
				result = answer.toString();
			} 
			catch(EOFException eo)
			{
			}
			catch(Exception ex) 
			{
				// if direct communication failed try through the server
				if(!partnerip.equals(""))
				{
					partnerip = "";
					sendRequest(iCommand); 
				}
				else
				{
					Log.i(TAG, "sendRequest: " + urlAddress + " params" + params, ex);
					sendMessage("ipaddress", ip);
				}
			} 
			finally 
			{
				if(con != null) con.disconnect();
			}
		}
		// All done
		//Log.i(TAG, "sendRequest: " + urlAddress + "\nreturned: " + result);
		
		inRequest = false; 
	      
		return result;
	}

	@Override
	public void run() 
	{
		String sRet = sendRequest(command);
		//if(showDialog && context != null) pd.dismiss();
		
		switch(command)
		{
		// 1 login
		// in: http://192.168.0.122:8080/?game=amoba&name=dani&command=1
		// out: miki;feri;laci;
		case 1:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			else if(sRet.equals(error))
			{
				handler.sendEmptyMessage(12);	// username occupied
			}
			else
			{
				this.loggedIn = true;
				if(!sRet.equals(""))
				{
					this.userList.clear();
					String[] sArr = sRet.split(";");
					String[] sPair;
					for(int i = 0; i < sArr.length; i++)
					{
						sPair = sArr[i].split("&", -1);
						this.userList.add(new Partner(sPair[0], sPair[1]));
					}
				}
				handler.sendEmptyMessage(13);	// show login success
			}
			break;
		// 2 ping
		// in:  http://192.168.0.122:8080/?game=amoba&name=dani&command=2
		// out: roli;5;10;miki;18;33;:feri;Hali;:laci;1280;720;40;
		// http://192.168.0.116:8484/?game=amoba&name=Xperia&command=2;Nexus%3B1440%3B2560%3B40%3B
		case 2:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			else if(!sRet.equals("")) sendMessage("update", sRet);
			break;
		// 3 get userlist
		case 3:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			else if(!sRet.equals(""))
			{
				this.userList.clear();
				String[] sArr = sRet.split(";");
				String[] sPair;
				for(int i = 0; i < sArr.length; i++)
				{
					sPair = sArr[i].split("&", -1);
					this.userList.add(new Partner(sPair[0], sPair[1]));
				}
			}
			break;
		// 4 invite
		// Nexus;1440;2560;40;
		// http://192.168.0.116:8484/?game=amoba&name=Xperia&command=4;Nexus%3B1440%3B2560%3B40%3B
		case 4:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			break;
		// 5 step
		// 5;feri;52;37
		case 5:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			break;
		// 6 message 
		// 6;peti;Hali
		case 6:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			break;
		// 7 logout
		// in:  http://192.168.0.122:8080/?game=amoba&name=dani&command=2
		// out: 7
		case 7:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			break;
		// 8 new table
		// 8;laci;				
		case 8:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			break;
		// 9 undo
		// 9;laci;				
		case 9:
			if(sRet.equals("&"))
			{
				Log.i(TAG, "ERROR! command :" + command + " params: " + params);
				handler.sendEmptyMessage(14);	// cannot access server
			}
			break;
		}
		
		params = "";
		partnerip = "";
		
		handler.sendEmptyMessage(0);
	}
	
    class netTimer extends TimerTask 
    {
        public void run() 
        {
        	if(loggedIn && !inRequest) poll();
        }
    }
	
	private void showMessageDialog(final Context context, String sMessage)
	{
		if(context == null) return;
		
		String sSend = context.getResources().getString(R.string.send);
		String sCancel = context.getResources().getString(R.string.cancel);
		String sMess = context.getResources().getString(R.string.message);
		
		AlertDialog.Builder builder;
		builder = new AlertDialog.Builder(context);
		builder.setMessage(sMessage);
		builder.setCancelable(true);
		builder.setPositiveButton(sSend, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog,	int id) 
			{
				dialog.dismiss();
				sText = editText.getText().toString();
				if(sText.length() > 1) message(sText);
			}
		});
		builder.setNegativeButton(sCancel, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog,	int id) 
			{
				dialog.cancel();
			}
		});
		editText = new EditText(context);
		editText.setHint(sMess);
		int maxLength = 128;
		InputFilter[] FilterArray = new InputFilter[1];
		FilterArray[0] = new InputFilter.LengthFilter(maxLength);
		editText.setFilters(FilterArray);
		editText.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				editText.setText("");
			}
		});
		editText.setOnKeyListener(new View.OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event) 
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN) 
				{
					switch (keyCode)
					{
						case KeyEvent.KEYCODE_DPAD_CENTER:
						case KeyEvent.KEYCODE_ENTER:
							InputMethodManager in = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
							in.hideSoftInputFromWindow(editText.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
							
							alertOnTap.dismiss();
							sText = editText.getText().toString();
							if(sText.length() > 1) message(sText);
						
							return true;
					}
				}
				return false;
			}
		});
		builder.setView(editText);

		alertOnTap = builder.create();
		alertOnTap.show();
	}
    
    public void showUserListDialog(Context context) 
	{
    	if(context == null) return;
    	this.context = context;
    	
		String sNoPlayer = context.getResources().getString(R.string.no_player);
		String sCancel = context.getResources().getString(R.string.cancel);
		String sPlayerList = context.getResources().getString(R.string.player_list);
		String sMess = context.getResources().getString(R.string.message);
		String sInvite = context.getResources().getString(R.string.invite);
    	
    	if(userList.size() == 0)
    	{
    		Toast.makeText(context, sNoPlayer, Toast.LENGTH_LONG).show();
    		return;
    	}
    	
		boolean[] checked = new boolean[userList.size()];
		String[] names = new String[userList.size()];
		int i = 0;
		for(Partner partner: userList)
		{
			names[i] = partner.name;
			checked[i] = selUsers.contains(partner.name);
			i++;
		}
		
		DialogInterface.OnMultiChoiceClickListener dialogListener = new DialogInterface.OnMultiChoiceClickListener() 
		{
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) 
			{
				if(isChecked) selUsers.add(userList.get(which).name);
				else selUsers.remove(userList.get(which));
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(sPlayerList);
		builder.setMultiChoiceItems(names, checked, dialogListener);
		builder.setNegativeButton(sCancel, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog,	int id) 
			{
				dialog.dismiss();
			}
		});
		builder.setPositiveButton(sInvite, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog,	int id) 
			{
				// send may table size
				StringBuffer sb = new StringBuffer();
				String ip = "";
				int iCnt = 0;
				for(Partner partner: userList)
				{
					// check selected names active players too
					if(selUsers.contains(partner.name) && !activeUsers.contains(partner.name))
					{
				    	sb.append(partner.name);
				    	sb.append(";");
				    	sb.append(width);
				    	sb.append(";");
				    	sb.append(height);
				    	sb.append(";");
				    	sb.append(size);
				    	sb.append(";");
				    	
				    	ip = partner.ip;
				    	iCnt++;
					}
				}
		    	String sMessage = sb.toString();
		    	if(iCnt != 1) ip = "";
		    	invite(sMessage, ip);
				dialog.dismiss();
			}
		});
		builder.setNeutralButton(sMess, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog,	int id) 
			{
				dialog.dismiss();
				showMessageDialog(WebClass.this.context, "");				
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}
    
    private void sendMessage(String id, String message)
    {
        Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(id, message);
        msg.setData(b);
        handler.sendMessage(msg);
    }
    
    public String getLocalIpAddress() 
	{
		String res = "";
	    try 
	    {
	        for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) 
	        {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) 
	            {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) 
	                {
	                    res = inetAddress.getHostAddress();
	                }
	            }
	        }
	    } 
	    catch (SocketException ex) 
	    {
	        Log.e(TAG, ex.toString());
	    }
	    
	    //Log.i(TAG, res);
	    
	    return res;
	}
    
	private boolean checkServer(String ip)
	{
		if(ip.equals("")) return false;
		
		String[] sa = ip.split("[.]");
        byte[] addr = new byte[4];
        addr[0] = (byte) Integer.parseInt(sa[0]);
        addr[1] = (byte) Integer.parseInt(sa[1]);
        addr[2] = (byte) Integer.parseInt(sa[2]);
        addr[3] = (byte) Integer.parseInt(sa[3]);
		
		InetAddress in;
		
		try 
		{
			in = InetAddress.getByAddress(addr);
			if(in.isReachable(1000)) return true;
		}
		catch(Exception ex) 
		{
			ex.printStackTrace();
		}
		
		return false;
	}
}
