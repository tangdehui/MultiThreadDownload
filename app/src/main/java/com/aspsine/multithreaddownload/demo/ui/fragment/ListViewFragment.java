package com.aspsine.multithreaddownload.demo.ui.fragment;


import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.aspsine.multithreaddownload.CallBack;
import com.aspsine.multithreaddownload.DownloadManager;
import com.aspsine.multithreaddownload.DownloadException;
import com.aspsine.multithreaddownload.DownloadRequest;
import com.aspsine.multithreaddownload.demo.DataSource;
import com.aspsine.multithreaddownload.demo.R;
import com.aspsine.multithreaddownload.demo.entity.AppInfo;
import com.aspsine.multithreaddownload.demo.listener.OnItemClickListener;
import com.aspsine.multithreaddownload.demo.ui.adapter.ListViewAdapter;
import com.aspsine.multithreaddownload.demo.util.Utils;
import com.aspsine.multithreaddownload.DownloadInfo;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class ListViewFragment extends Fragment implements OnItemClickListener<AppInfo> {
    @Bind(R.id.listView)
    ListView listView;

    private List<AppInfo> mAppInfos;
    private ListViewAdapter mAdapter;

    public ListViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ListViewAdapter();
        mAdapter.setOnItemClickListener(this);
        mAppInfos = DataSource.getInstance().getData();
        for (AppInfo info : mAppInfos) {
            DownloadInfo downloadInfo = DownloadManager.getInstance().getDownloadProgress(info.getUrl());
            if (downloadInfo != null) {
                info.setProgress(downloadInfo.getProgress());
                info.setDownloadPerSize(getDownloadPerSize(downloadInfo.getFinished(), downloadInfo.getLength()));
                info.setStatus(AppInfo.STATUS_PAUSE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_list_view, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setAdapter(mAdapter);
        mAdapter.setData(mAppInfos);
    }

    private static final DecimalFormat DF = new DecimalFormat("0.00");

    /**
     * Dir: /Download
     */
    private final File dir = new File(Environment.getExternalStorageDirectory(), "Download");

    @Override
    public void onItemClick(View v, final int position, final AppInfo appInfo) {

        if (appInfo.getStatus() == AppInfo.STATUS_DOWNLOADING || appInfo.getStatus() == AppInfo.STATUS_CONNECTING) {
            if (isCurrentListViewItemVisible(position)) {
                DownloadManager.getInstance().pause(appInfo.getUrl());
            }
            return;
        } else if (appInfo.getStatus() == AppInfo.STATUS_COMPLETE) {
            if (isCurrentListViewItemVisible(position)) {
                Utils.installApp(getActivity(), new File(dir, appInfo.getName() + ".apk"));
            }
            return;
        } else if (appInfo.getStatus() == AppInfo.STATUS_INSTALLED) {
            if (isCurrentListViewItemVisible(position)) {
                Utils.unInstallApp(getActivity(), appInfo.getPackageName());
            }
        } else {
            download(position, appInfo);
        }
    }

    private void download(final int position, final AppInfo appInfo) {
        DownloadRequest request = new DownloadRequest.Builder()
                .setTitle(appInfo.getName() + ".apk")
                .setUri(appInfo.getUrl())
                .setFolder(dir)
                .build();
        DownloadManager.getInstance().download(request, appInfo.getUrl(), new CallBack() {

            @Override
            public void onStarted() {

            }

            @Override
            public void onConnecting() {
                appInfo.setStatus(AppInfo.STATUS_CONNECTING);
                if (isCurrentListViewItemVisible(position)) {
                    ListViewAdapter.ViewHolder holder = getViewHolder(position);
                    holder.tvStatus.setText(appInfo.getStatusText());
                    holder.btnDownload.setText(appInfo.getButtonText());
                }
            }

            @Override
            public void onConnected(long total, boolean isRangeSupport) {
                appInfo.setStatus(AppInfo.STATUS_DOWNLOADING);
                if (isCurrentListViewItemVisible(position)) {
                    ListViewAdapter.ViewHolder holder = getViewHolder(position);
                    holder.tvStatus.setText(appInfo.getStatusText());
                    holder.btnDownload.setText(appInfo.getButtonText());
                }
            }

            @Override
            public void onProgress(long finished, long total, int progress, long speed) {
                String downloadPerSize = getDownloadPerSize(finished, total);
                appInfo.setProgress(progress);
                appInfo.setDownloadPerSize(downloadPerSize);
                appInfo.setStatus(AppInfo.STATUS_DOWNLOADING);
                if (isCurrentListViewItemVisible(position)) {
                    ListViewAdapter.ViewHolder holder = getViewHolder(position);
                    holder.tvDownloadPerSize.setText(downloadPerSize);
                    holder.progressBar.setProgress(progress);
                    holder.tvStatus.setText(appInfo.getStatusText());
                    holder.btnDownload.setText(appInfo.getButtonText());
                }
            }


            @Override
            public void onCompleted() {
                appInfo.setStatus(AppInfo.STATUS_COMPLETE);
                File apk = new File(dir, appInfo.getName() + ".apk");
                if (apk.isFile() && apk.exists()) {
                    String packageName = Utils.getApkFilePackage(getActivity(), apk);
                    appInfo.setPackageName(packageName);
                    if (Utils.isAppInstalled(getActivity(), packageName)) {
                        appInfo.setStatus(AppInfo.STATUS_INSTALLED);
                    }
                }

                if (isCurrentListViewItemVisible(position)) {
                    ListViewAdapter.ViewHolder holder = getViewHolder(position);
                    holder.tvStatus.setText(appInfo.getStatusText());
                    holder.btnDownload.setText(appInfo.getButtonText());
                }
            }

            @Override
            public void onDownloadPaused() {
                appInfo.setStatus(AppInfo.STATUS_PAUSE);
                if (isCurrentListViewItemVisible(position)) {
                    ListViewAdapter.ViewHolder holder = getViewHolder(position);
                    holder.tvStatus.setText(appInfo.getStatusText());
                    holder.btnDownload.setText(appInfo.getButtonText());
                }
            }

            @Override
            public void onDownloadCanceled() {
                appInfo.setStatus(AppInfo.STATUS_NOT_DOWNLOAD);
                appInfo.setDownloadPerSize("");
                if (isCurrentListViewItemVisible(position)) {
                    ListViewAdapter.ViewHolder holder = getViewHolder(position);
                    holder.tvStatus.setText(appInfo.getStatusText());
                    holder.tvDownloadPerSize.setText("");
                    holder.btnDownload.setText(appInfo.getButtonText());
                }
            }

            @Override
            public void onFailed(DownloadException e) {
                appInfo.setStatus(AppInfo.STATUS_DOWNLOAD_ERROR);
                appInfo.setDownloadPerSize("");
                if (isCurrentListViewItemVisible(position)) {
                    ListViewAdapter.ViewHolder holder = getViewHolder(position);
                    holder.tvStatus.setText(appInfo.getStatusText());
                    holder.tvDownloadPerSize.setText("");
                    holder.btnDownload.setText(appInfo.getButtonText());
                }
                e.printStackTrace();
            }
        });
    }

    private String getDownloadPerSize(long finished, long total) {
        return DF.format((float) finished / (1024 * 1024)) + "M/" + DF.format((float) total / (1024 * 1024)) + "M";
    }

    private boolean isCurrentListViewItemVisible(int position) {
        int first = listView.getFirstVisiblePosition();
        int last = listView.getLastVisiblePosition();
        return first <= position && position <= last;
    }

    private ListViewAdapter.ViewHolder getViewHolder(int position) {
        int childPosition = position - listView.getFirstVisiblePosition();
        View view = listView.getChildAt(childPosition);
        return (ListViewAdapter.ViewHolder) view.getTag();
    }
}
