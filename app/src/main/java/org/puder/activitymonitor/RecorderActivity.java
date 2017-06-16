package org.puder.activitymonitor;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class RecorderActivity extends AppCompatActivity implements ItemAdapter.ListItemClickListener {

    final static int             MY_PERMISSIONS = 0;

    private List<String>         activitiesList;
    private RecyclerView.Adapter adapter;
    private FloatingActionButton fab;

    private DriveUploader        driveUploader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (RecorderService.isRunning) {
                    Snackbar.make(view, "Recording in progress", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (driveUploader != null) {
                    Snackbar.make(view, "Upload in progress", Snackbar.LENGTH_LONG).show();
                    return;
                }
                promptActivityType();
            }
        });

        activitiesList = StorageUtil.getDirectoryListing();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(activitiesList, this);
        recyclerView.setAdapter(adapter);

        checkPermission();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        activitiesList.clear();
        activitiesList.addAll(StorageUtil.getDirectoryListing());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DriveUploader.REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            driveUploader.connect();
        }
    }

    @Override
    public void onItemClicked(final int position) {
        final String activityType = activitiesList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete " + activityType + "?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                StorageUtil.delete(activityType);
                activitiesList.remove(position);
                adapter.notifyItemRemoved(position);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recorder, menu);
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(RecorderFinishedEvent event) {
        activitiesList.add(0, event.activityType);
        adapter.notifyItemInserted(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UploaderEvent event) {
        driveUploader = null;
        Snackbar.make(fab, event.message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_upload) {
            uploadActivities();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void uploadActivities() {
        if (driveUploader != null) {
            Snackbar.make(fab, "Upload in progress", Snackbar.LENGTH_LONG).show();
            return;
        }
        driveUploader = new DriveUploader(this);
    }

    private void promptActivityType() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter activity type:");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                String activityType = input.getText().toString();
                if (activityType.equals("")) {
                    Snackbar.make(fab, "No activity type specified", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (activitiesList.contains(activityType)) {
                    Snackbar.make(fab, "Activity type already exists", Snackbar.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(RecorderActivity.this, RecorderService.class);
                intent.putExtra(RecorderService.EXTRA_ACTIVITY_TYPE, activityType);
                startService(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, MY_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {
        switch (requestCode) {
        case MY_PERMISSIONS: {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                finish();
            }
            return;
        }
        }
    }
}
