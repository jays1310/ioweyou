package com.example.ioweyou;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        // 1) Find & configure RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerViewGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2) Build your list of groups
        List<Group> groupList = new ArrayList<>();

        // 3) Create the adapter, passing in the click callback
        GroupAdapter.OnGroupClickListener clickListener = group -> {
            Intent intent = new Intent(GroupDetailActivity.this, GroupChatActivity.class);
            intent.putExtra("group_name", group.getName());
            startActivity(intent);
        };
        GroupAdapter adapter = new GroupAdapter(groupList, clickListener);

        // 4) Attach it
        recyclerView.setAdapter(adapter);
    }
}
