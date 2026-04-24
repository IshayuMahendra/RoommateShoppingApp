package edu.uga.cs.roommateapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateapp.HistoryAdapter;
import edu.uga.cs.roommateapp.R;
import edu.uga.cs.roommateapp.models.Purchase;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<Purchase> purchaseList;
    private DatabaseReference historyReference;
    private TextView tvEmptyHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewHistory);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        purchaseList = new ArrayList<>();
        adapter = new HistoryAdapter(purchaseList);
        recyclerView.setAdapter(adapter);

        historyReference = FirebaseDatabase.getInstance().getReference("history");

        historyReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                purchaseList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Purchase purchase = dataSnapshot.getValue(Purchase.class);
                    if (purchase != null) {
                        purchase.purchaseId = dataSnapshot.getKey();
                        purchaseList.add(0, purchase); // Show newest first
                    }
                }
                
                if (purchaseList.isEmpty()) {
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyHistory.setVisibility(View.GONE);
                }
                
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });

        return view;
    }
}
