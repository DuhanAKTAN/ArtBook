package com.duhanaktan.artbook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.duhanaktan.artbook.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    ArrayList<Art> artArrayList;
    ArtAdaptor artAdaptor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        artArrayList= new ArrayList<>();

        binding.recyclerViewMain.setLayoutManager(new LinearLayoutManager(this));
        artAdaptor=new ArtAdaptor(artArrayList);
        binding.recyclerViewMain.setAdapter(artAdaptor);
        getData();
    }

    private void getData(){
        try {
            SQLiteDatabase database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            Cursor cursor= database.rawQuery("SELECT * FROM arts",null);
            int nameIx=cursor.getColumnIndex("artName");
            int idIx=cursor.getColumnIndex("id");

            while(cursor.moveToNext()){
                String name= cursor.getString(nameIx);
                int ix=cursor.getInt(idIx);
                Art art = new Art(name,ix);
                artArrayList.add(art);
            }
            artAdaptor.notifyDataSetChanged();
            cursor.close();

        }catch (Exception e){e.printStackTrace();}
    }

    //menü açıldığında ne olacak
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.item_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }
    //herhangi bir item seçildiğinde ne olacak
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.addArt){
            Intent intent=new Intent(this,ArtActivity.class);
            intent.putExtra("info","new");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}