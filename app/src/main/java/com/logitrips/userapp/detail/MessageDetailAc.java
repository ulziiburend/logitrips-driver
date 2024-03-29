package com.logitrips.userapp.detail;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.logitrips.userapp.R;
import com.logitrips.userapp.adapter.ChatAdapter;
import com.logitrips.userapp.model.Chat;
import com.logitrips.userapp.util.CustomRequest;
import com.logitrips.userapp.util.MySingleton;
import com.logitrips.userapp.util.Utils;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageDetailAc extends ActionBarActivity implements View.OnClickListener {

    private static final String ARG_PARAM1 = "user_id";
    private static final String DRIVER_ID = "driver_id";
    private static final String ARG_PARAM5 = "driver_name";
    private static final String ARG_PARAM2 = "auth";
    private static final String ARG_PARAM4 = "profile_pic";
    private String mParam1;
    private String driver_name;

    private String mParam4;
    private String mParam2;


    private String driver_id;
    List<Chat> chats = new ArrayList<Chat>();
    private ListView lv;
    private ChatAdapter adapter;
    private ActionBar actionBar;
    Bundle b;
    private ImageButton send;
    private ImageButton location;
    private EditText message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_messagedetail_list);
        location = (ImageButton) findViewById(R.id.chat_location);
        send = (ImageButton) findViewById(R.id.chat_send);
        message = (EditText) findViewById(R.id.chat_edit);
        b = getIntent().getExtras();
        mParam1 = b.getString(ARG_PARAM1);
        mParam2 = b.getString(ARG_PARAM2);
        driver_id = b.getString(DRIVER_ID);
        mParam4 = b.getString(ARG_PARAM4);
        driver_name = b.getString(ARG_PARAM5);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(driver_name);
        getData(mParam1, mParam2, driver_id);
        lv = (ListView) findViewById(R.id.list);
        Log.e("image", mParam4);
        adapter = new ChatAdapter(this, chats, mParam4);
        lv.setAdapter(adapter);
        send.setOnClickListener(this);
        location.setOnClickListener(this);
    }

    private void getData(String user_id, final String auth, String driver_id) {
        if (!Utils.isNetworkAvailable(MessageDetailAc.this)) {
            Toast.makeText(MessageDetailAc.this, R.string.no_net, Toast.LENGTH_SHORT).show();
        } else {
            Log.d("user and driver id", user_id + "---" + driver_id);
            CustomRequest loginReq = new CustomRequest(Request.Method.GET,
                    getString(R.string.main_url) + "/mobile/getchathistory?user_id=" + user_id + "&driver_id=" + driver_id, null, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    Log.e("chat data", response.toString());
                    try {
                        switch (response.getInt("response")) {
                            case 200:

                                getChats(response.getJSONArray("data"));
                                break;

                            case 100:
                                Toast.makeText(MessageDetailAc.this, R.string.error_request, Toast.LENGTH_SHORT).show();
                                break;
                            case 300:
                                Toast.makeText(MessageDetailAc.this, R.string.no_available_cars, Toast.LENGTH_SHORT).show();
                                break;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }


            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO Auto-generated method stub
                    Toast.makeText(MessageDetailAc.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Authorization", auth);
                    return params;
                }
            };
            MySingleton.getInstance(MessageDetailAc.this).addToRequestQueue(loginReq);
        }

    }

    private void getChats(JSONArray array) throws JSONException {

        if (array.length() == 0) {
            Toast.makeText(MessageDetailAc.this, R.string.error_request, Toast.LENGTH_SHORT).show();
        }
        for (int i = array.length() - 1; i >= 0; i--) {
            JSONObject item = array.getJSONObject(i);
            Chat chat = new Chat();
            chat.setDriver_id(item.getInt("driver_id"));
            chat.setUser_id(item.getInt("user_id"));
            chat.setDate_sent(item.getLong("date_sent"));
            chat.setIs_recv(item.getInt("is_received"));
            chat.setMessage(item.getString("message"));
            chat.setLocation(item.getString("location"));

            chats.add(chat);

        }

        adapter.notifyDataSetChanged();
        lv.setSelection(lv.getCount() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v == send) {
            if (!TextUtils.isEmpty(message.getText())) {
                sendMsg(message.getText().toString(), mParam2, null);
            }
        }
        if (v == location) {
            Intent mapIntent = new Intent(MessageDetailAc.this, MapsActivity.class);
            startActivityForResult(mapIntent, 123);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Location location = new Location("");
            location.setLatitude(data.getDoubleExtra("lat", 0.0));
            location.setLongitude(data.getDoubleExtra("lon", 0.0));
            sendMsg(" ", mParam2, location);
        }
    }

    private void sendMsg(final String messageStr, final String auth, final Location location) {
        if (!Utils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.no_net, Toast.LENGTH_SHORT).show();
        } else {
            CustomRequest loginReq = new CustomRequest(Request.Method.POST,
                    getString(R.string.main_url) + "/mobile/composemessage", null, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    Log.e("send msg data", response.toString());
                    try {
                        switch (response.getInt("response")) {
                            case 200:

                                message.setText("");
                                Chat chat = new Chat();
                                chat.setMessage(messageStr);
                                chat.setIs_recv(0);
                                chat.setUser_id(Integer.parseInt(mParam1));
                                chat.setDriver_id(Integer.parseInt(driver_id));
                                chat.setDate_sent(System.currentTimeMillis());
                                if (location != null)
                                    chat.setLocation(location.getLatitude() + "," + location.getLongitude());
                                chats.add(chat);

                                int index = lv.getFirstVisiblePosition() + chats.size();
                                View v = lv.getChildAt(lv.getHeaderViewsCount());
                                int top = (v == null) ? 0 : v.getTop();

                                adapter.notifyDataSetChanged();

                                lv.setSelectionFromTop(index, top);
                                break;

                            case 100:
                                Toast.makeText(MessageDetailAc.this, R.string.error_request, Toast.LENGTH_SHORT).show();
                                break;
                            case 300:
                                Toast.makeText(MessageDetailAc.this, R.string.no_available_cars, Toast.LENGTH_SHORT).show();
                                break;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }


            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO Auto-generated method stub
                    Toast.makeText(MessageDetailAc.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("Authorization", auth);
                    return params;
                }

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("user_id", mParam1 + "");
                    params.put("driver_id", driver_id + "");
                    params.put("message", messageStr + "");
                    if (location != null) {
                        params.put("location", location.getLatitude() + "," + location.getLongitude());
                    }

                    return params;
                }
            };
            MySingleton.getInstance(this).addToRequestQueue(loginReq);
        }

    }
}
