package com.duhanaktan.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.duhanaktan.artbook.databinding.ActivityArtBinding;
import com.duhanaktan.artbook.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;      //Galeriye gitmek için
    ActivityResultLauncher<String> permissionLauncher;          //izin istemek için
    Bitmap selectedImage;       //bitmap e çevirilmiş resim

    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        database=this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
        registerLauncher();

        Intent intent=getIntent();
        String info=intent.getStringExtra("info");
        if(info.equals("new")){
            // add activity
            binding.artName.setText("");
            binding.yearText.setText("");
            binding.artistName.setText("");
            binding.imageView.setImageResource(R.drawable.image);
            binding.saveButton.setVisibility(View.VISIBLE);

        }else {
            int artId=intent.getIntExtra("artId",1);
            binding.saveButton.setVisibility(View.INVISIBLE);
            try {
                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});
                int artNameIx=cursor.getColumnIndex("artName");
                int artistNameIx=cursor.getColumnIndex("artistName");
                int yearIx=cursor.getColumnIndex("year");
                int imageIx=cursor.getColumnIndex("image");
                while(cursor.moveToNext()){
                    binding.artName.setText(cursor.getString(artNameIx));
                    binding.artistName.setText(cursor.getString(artistNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes= cursor.getBlob(imageIx);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);

                }
                cursor.close();
            }catch (Exception e){e.printStackTrace();}
            binding.artName.setEnabled(false);
            binding.artistName.setEnabled(false);
            binding.yearText.setEnabled(false);
            binding.imageView.setEnabled(false);
            //binding.artName.setEnabled(false);

        }
    }
    public void save(View view){
        String artname=binding.artName.getText().toString();
        String artistname=binding.artistName.getText().toString();
        String year=binding.yearText.getText().toString();

        Bitmap smallImage=makeSmallerImage(selectedImage,300); //resmi küçülttük

        // byte arraya dönüştürüp veritabanında saklıcaz
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray(); // resmi byte cinsinden arrayda tutuyoruz

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artName VARCHAR,artistName VARCHAR,year VARCHAR,image BLOB)");

            String insertSql="INSERT INTO arts (artName,artistName,year,image) VALUES (?,?,?,?)";

            SQLiteStatement sqLiteStatement= database.compileStatement(insertSql);
            sqLiteStatement.bindString(1,artname);
            sqLiteStatement.bindString(2,artistname);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){e.printStackTrace();}

        Intent intent=new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    // bundan önceki bütün aktiviteleri kapat ve gösterdiğime git
        startActivity(intent);

    }
    public Bitmap makeSmallerImage(Bitmap image,int maxSize){
        int width= image.getWidth();
        int height=image.getHeight();

        float ratio= (float) width/ (float) height;

        if(ratio>1){
            //yatay resim
            width=maxSize;
            height= (int) (ratio/width);
        }else{
            //dikey resim
            height=maxSize;
            width= (int) (height*ratio);
        }
        return image.createScaledBitmap(image,width,height,true);
    }
    public void imageSelect(View view){
        // resim seçmeye basıldığında izin verip vermediğini kontrol ediyoruz
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            //izin verilmemişse ilk seferde sonraki istekte neden göstermek
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //izin iste
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }else {
                //izin iste
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }else {
            //go gallery
            Intent intent=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intent);
        }
    }
    public void registerLauncher(){
        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                //result ta resim seçildi mi dolu mu
                if(result.getResultCode()==RESULT_OK){
                    //doluysa datayı al
                    Intent intentFrom= result.getData();
                    if(intentFrom != null){
                        // datayı tekrar al URI formatında
                        Uri imageData=intentFrom.getData();

                        try {
                            //bitmap e çevirme işlemleri
                            if(Build.VERSION.SDK_INT >= 28){
                                ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage=ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);

                            }else{
                                selectedImage=MediaStore.Images.Media.getBitmap(getContentResolver(),imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }

                        }catch (Exception e){e.printStackTrace();}
                    }
                }
            }
        });

        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //izin verildi
                    Intent intent=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intent);
                }else {
                    //izin verilmedi
                    Toast.makeText(ArtActivity.this,"İzin verilmesi gerek!!",Toast.LENGTH_LONG).show();
               }
            }
        });
    }
}
