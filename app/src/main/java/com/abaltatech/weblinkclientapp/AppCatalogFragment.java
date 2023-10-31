/****************************************************************************
 *
 * @file AppCatalogFragment.java
 * @brief
 *
 * Defines the AppCatalogFragment class.
 *
 * @author Abalta Technologies, Inc.
 * @date December/2020
 *
 * @cond Copyright
 *
 * COPYRIGHT 2020 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @endcond
 *****************************************************************************/
package com.abaltatech.weblinkclientapp;


import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.abaltatech.weblinkclient.appcatalog.IWLAppCatalogNotification;
import com.abaltatech.weblinkclient.appcatalog.WLAppCatalogManager;
import com.abaltatech.weblinkclient.appcatalog.WLAppInfo;
import com.testabalta.R;

import java.util.Map;

/**
 * Fragment that displays a List View that uses a custom Array Adapter to visualize
 * the WebLink Application Catalog.
 */
public class AppCatalogFragment extends Fragment implements IWLAppCatalogNotification {

    // Time to wait for a response after the Full Application Catalog has been requested.
    // If the response does not arrive before this timeout expires, we consider that the
    // request failed.
    private static final int APP_CATALOG_RESPONSE_TIMEOUT = 5000; // 5 seconds

    // Reference to the Application Catalog Manager for easier access
    private WLAppCatalogManager m_wlAppCatalogManager;
    // Array adapter that maintains the list of applications
    private ArrayAdapter<WLAppInfo> m_arrayAdapter;

    private boolean m_waitingForCatalog = false;

    // Handler, used to dispatch code on the main thread
    private static final Handler s_mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_wlAppCatalogManager = App.instance().getWLClientCore().getAppCatalogManager();
        if (m_wlAppCatalogManager != null) {
            // Register for notifications
            m_wlAppCatalogManager.setNotification(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_catalog_fragment,
                container, false);

        ListView listView = view.findViewById(R.id.lv_app_catalog);
        ImageView ivRequestCatalog = view.findViewById(R.id.iv_request_catalog);
        ivRequestCatalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!m_waitingForCatalog) {
                    // We will be waiting for a response from the Host Application
                    m_waitingForCatalog = true;

                    // Clear the catalog
                    m_arrayAdapter.clear();
                    m_arrayAdapter.notifyDataSetChanged();

                    // Instruct the WebLink Application Catalog Manager to clear any cached
                    // Application Catalog data
                    m_wlAppCatalogManager.clearCache();

                    if (App.instance().getWLClientCore().isConnected()) {
                        // Start the wait
                        s_mainThreadHandler.postDelayed(mTimeout, APP_CATALOG_RESPONSE_TIMEOUT);
                        // Request the Full Application Catalog
                        m_wlAppCatalogManager.requestFullCatalog();
                    }
                } else {
                    // We have already requested the application catalog
                    showWarningDialog("Application Catalog Request is still pending!");
                }
            }
        });

        m_arrayAdapter = new AppCatalogArrayAdapter(view.getContext());

        if (m_wlAppCatalogManager != null) {
            Map<String, WLAppInfo> appInfoMap = m_wlAppCatalogManager.getAppCatalog();
            for (Map.Entry<String, WLAppInfo> entry : appInfoMap.entrySet()) {
                WLAppInfo appInfo = entry.getValue();
                m_arrayAdapter.add(appInfo);
            }
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WLAppInfo item = (WLAppInfo) parent.getItemAtPosition(position);
                if (item != null) {
                    if (item.getAppIDState() != WLAppInfo.EAppInfoParamState.NotSet) {
                        m_wlAppCatalogManager.startApplication(item.getApplicationID());
                        HomeActivity activity = (HomeActivity) getActivity();
                        if (activity != null) {
                            activity.onBackPressed();
                        }
                    }
                }
            }
        });

        listView.setAdapter(m_arrayAdapter);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        s_mainThreadHandler.removeCallbacks(mTimeout);
        m_waitingForCatalog = false;
        if (m_wlAppCatalogManager != null) {
            m_wlAppCatalogManager.setNotification(null);
        }
    }

    private void showWarningDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.warning));
        builder.setMessage(message);
        builder.setNeutralButton(getString(android.R.string.ok), null);
        builder.setCancelable(true);
        builder.show();
    }

    /**
     * Called when an application has been added to the Applicaton Catalog on the Host Application
     * side.
     *
     * @param appInfo Application that was added
     */
    @Override
    public void onAppInfoAdded(final WLAppInfo appInfo) {
        final WLAppInfo item = findAppByID(appInfo.getApplicationID());
        // Only add the item if it is not already part of the catalog
        if (item == null) {
            s_mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    m_arrayAdapter.add(appInfo);
                    m_arrayAdapter.notifyDataSetChanged();
                }
            });
        }

    }

    /**
     * Called when an the information for a given application was updated on the Host Application
     * side.
     *
     * @param appInfo The application that was updated
     */
    @Override
    public void onAppInfoUpdated(final WLAppInfo appInfo) {
        final WLAppInfo item = findAppByID(appInfo.getApplicationID());
        // Update the information for an existing item from the catalog
        if (item != null) {
            s_mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    m_arrayAdapter.remove(item);
                    m_arrayAdapter.add(appInfo);
                    m_arrayAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    /**
     * Called when an application was removed from the Application Catalog on the Host Application.
     * @param appInfo The application that was removed
     */
    @Override
    public synchronized void onAppInfoRemoved(WLAppInfo appInfo) {
        final WLAppInfo item = findAppByID(appInfo.getApplicationID());
        // Remove an item from the catalog
        if (item != null) {
            s_mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    m_arrayAdapter.remove(item);
                    m_arrayAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public synchronized void onAppCatalogUpdated(Map<String, WLAppInfo> appCatalog) {
        m_waitingForCatalog = false;
        s_mainThreadHandler.removeCallbacks(mTimeout);
    }

    /**
     * Helper method which finds the item in the Array Adapter that has the specified application
     * identifier.
     *
     * @param appID Application id
     * @return {@link WLAppInfo} object or null if an application with the specified ID is not
     *         present in the array adapter
     */
    private WLAppInfo findAppByID(String appID) {
        for (int i = 0; i < m_arrayAdapter.getCount(); ++i) {
            final WLAppInfo item = m_arrayAdapter.getItem(i);
            if (item != null) {
                if (item.getApplicationID().equals(appID)) {
                    return item;
                }
            }
        }

        return null;
    }

    /**
     * Helper runnable that runs after a set amount of time after the full application catalog has
     * been requested to signal a timeout to the user.
     */
    private final Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            showWarningDialog("Application Catalog Request timeout!");
            m_waitingForCatalog = false;
        }
    };

    /**
     * Custom Array Adapter, used to visualize the WebLink Application information such as the
     * Display Name, Category, Status and Application Icon.
     */
    static class AppCatalogArrayAdapter extends ArrayAdapter<WLAppInfo> {

        private final LayoutInflater m_inflater;

        /**
         * Helper class that holds the information about a WebLink Application that is
         * used by the Array Adapter to fill the views.
         */
        static class ViewHolder {
            TextView name;
            TextView type;
            TextView status;
            ImageView icon;
        }

        /**
         * Default {@link ArrayAdapter} constructor.
         *
         * Initializes the layout inflater that will be used in the {@link #getView(int, View, ViewGroup)}
         * method.
         *
         * @param context Activity Context
         */
        public AppCatalogArrayAdapter(Context context) {
            super(context, -1);
            m_inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = m_inflater.inflate(R.layout.app_catalog_list_item, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.tv_app_name);
                viewHolder.type = (TextView) convertView.findViewById(R.id.tv_app_type);
                viewHolder.status = (TextView) convertView.findViewById(R.id.tv_app_status);
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.iv_app_icon);
                convertView.setTag(viewHolder);
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();
            WLAppInfo app = getItem(position);
            if (app != null) {
                if (app.getDisplayNameState() != WLAppInfo.EAppInfoParamState.NotSet) {
                    holder.name.setText(app.getDisplayName());
                }
                if (app.getCategoryState() != WLAppInfo.EAppInfoParamState.NotSet) {
                    holder.type.setText(app.getCategory());
                }
                if (app.getAppStatusState() != WLAppInfo.EAppInfoParamState.NotSet) {
                    holder.status.setText(app.getAppStatus().toString());
                }
                if (app.getAppImageState() != WLAppInfo.EAppInfoParamState.NotSet) {
                    byte [] imageData = app.getAppImage();
                    if (imageData != null && imageData.length > 0) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                        if (bitmap != null) {
                            holder.icon.setImageBitmap(bitmap);
                        }
                    }
                }
            }

            return convertView;
        }
    }
}
