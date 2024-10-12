package com.example.ideality.volley;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.ideality.activities.IdealityApplication;

public class VolleySingleton {
    private static VolleySingleton instance = null;
    private RequestQueue requestQueue;

    private VolleySingleton() {
        requestQueue = getRequestQueue();
    }

    public static synchronized VolleySingleton getInstance() {
        if (instance == null) {
            instance = new VolleySingleton();
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(IdealityApplication.getAppContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> request) {
        getRequestQueue().add(request);
    }
}