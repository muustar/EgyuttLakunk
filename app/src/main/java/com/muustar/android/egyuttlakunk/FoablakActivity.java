package com.muustar.android.egyuttlakunk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;


public class FoablakActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnLongClickListener, View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "FECO";
    private static final int RC_SIGN_IN = 123;
    private static final String ANONYMOUS = "Anonymous";
    private LinearLayout ll;
    private LinearLayout llUzenetek;
    private RelativeLayout rlListaelem;
    private LinearLayout.LayoutParams checkbox_relativeParams;
    private ImageView Prof_img;
    private TextView Name;
    private TextView Email;
    private static int REQ_CODE = 9001;

    //firebase
    private String mNinckName;
    private String mUsername;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth mFirebaseAuth;

    //adatbázis
    private FirebaseDatabase db ;
    private DatabaseReference bevasarlolistaRef ;
    private DatabaseReference keszBevasarlolistaRef;
    private DatabaseReference uzenetekRef;
    private ChildEventListener mChildEventListener;
    private ChildEventListener mChildUzenetekListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foablak);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //inicializáljuk a felület elemeit
        Prof_img = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.prof_img);
        Name = navigationView.getHeaderView(0).findViewById(R.id.prof_name);
        Email = navigationView.getHeaderView(0).findViewById(R.id.prof_email);
        ll = (LinearLayout) findViewById(R.id.linerarLayoutBevasatlolista);
        llUzenetek = (LinearLayout) findViewById(R.id.llUzenetek);
        rlListaelem = findViewById(R.id.rlListaelemHozzadas);

        rlListaelem.setOnClickListener(this);

        // Layout-Info for Checkbox
        checkbox_relativeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        checkbox_relativeParams.setMargins(DPtoPX(20), 0, 0, 0);

        attachDatabaseReadListener(); //elindítjuk a bejelentkezéskezelőt
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Belépés sikeres", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Mégsem léptél be", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void onSignedInInicialize(String displayName) {
        mUsername = displayName;
        int szunetIndex = mUsername.indexOf(" ");
        mNinckName = mUsername.substring(0, 1)
                + mUsername.substring(szunetIndex + 1, szunetIndex + 2); // monogramm : (M)olnar (F)erenc  --> MF
        checkboxListaFeltoltes(); // feltöltjük a checkboxokat
        uzenetBoxFeltoltes(); //  feltöltjük az üzenőboxot
        initNavigationBar();  // beállítjuk a navigation báron a user adatait
    }

    private void initNavigationBar() {
        Name.setText(mFirebaseAuth.getCurrentUser().getDisplayName());
        Email.setText(mFirebaseAuth.getCurrentUser().getEmail());
        if (mFirebaseAuth.getCurrentUser().getPhotoUrl() != null) {
            String photoUrl = mFirebaseAuth.getCurrentUser().getPhotoUrl().toString();
            if (photoUrl.length() != 0) {
                Glide.with(this).load(photoUrl).into(Prof_img);
            }
        }
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        ll.removeAllViews();
        llUzenetek.removeAllViews();
        detachDatabaseReadListener();
        clearNavigationBar();
    }

    private void clearNavigationBar() {
        Name.setText(mUsername);
        Email.setText("email@email.hu");
        Prof_img.setImageResource(R.mipmap.ic_launcher_round);
        mNinckName = "Anon";
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            bevasarlolistaRef.removeEventListener(mChildEventListener);
            keszBevasarlolistaRef.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }

        if (mChildUzenetekListener != null) {
            uzenetekRef.removeEventListener(mChildUzenetekListener);
            mChildUzenetekListener = null;
        }
    }

    public void attachDatabaseReadListener() {
        //adatbázis
        mFirebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        bevasarlolistaRef = db.getReference().child("bevasarlolista");
        keszBevasarlolistaRef = db.getReference().child("keszbevasarlolista");
        uzenetekRef = db.getReference().child("uzenetek");

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user belépve
                    onSignedInInicialize(user.getDisplayName());
                    //Toast.makeText(getApplicationContext(), "Üdv " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                } else {
                    onSignedOutCleanup();
                    //user még nincs belépve
                    //Toast.makeText(getApplicationContext(), "nincs belépve senki", Toast.LENGTH_SHORT).show();
                    //
                    //Easily add sign-in to your Android app with FirebaseUI
                    //https://firebase.google.com/docs/auth/android/firebaseui?authuser=0
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    private void checkboxListaFeltoltes() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    BevasarloListaElem e = dataSnapshot.getValue(BevasarloListaElem.class);
                    CheckBox ch = new CheckBox(FoablakActivity.this);
                    ch.setLayoutParams(checkbox_relativeParams);
                    ch.setTag(dataSnapshot.getKey());
                    ch.setChecked(e.isChecked());
                    ch.setText(e.getValue());
                    ch.setOnClickListener(FoablakActivity.this);
                    ch.setOnLongClickListener(FoablakActivity.this);
                    if (e.isChecked()) {
                        ll.addView(ch, -1);
                        //Log.d(TAG, ch.getText().toString());
                    } else {
                        ll.addView(ch, 0);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Toast.makeText(getApplicationContext(), "onChildChanged", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    ArrayList<View> v = getViewsByTag(ll, dataSnapshot.getKey().toString());
                    ll.removeView((CheckBox) v.get(0));
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Toast.makeText(getApplicationContext(), "onChildMoved", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getApplicationContext(), "onCancelled", Toast.LENGTH_SHORT).show();
                }
            };
        }
        bevasarlolistaRef.addChildEventListener(mChildEventListener);
        keszBevasarlolistaRef.addChildEventListener(mChildEventListener);
    }

    private void uzenetBoxFeltoltes() {
        if (mChildUzenetekListener == null) {
            mChildUzenetekListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Uzenet uzi = dataSnapshot.getValue(Uzenet.class);
                    String uziID = dataSnapshot.getKey();
                    uzenetBeleABoxba(uzi, uziID);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    String uziTag = dataSnapshot.getKey();
                    ArrayList<View> v = getViewsByTag(llUzenetek, uziTag);
                    TextView tv = (TextView) v.get(0);
                    llUzenetek.removeView(tv);
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
        }
        uzenetekRef.addChildEventListener(mChildUzenetekListener);
    }

    private static ArrayList<View> getViewsByTag(ViewGroup root, String tag) {
        ArrayList<View> views = new ArrayList<View>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getViewsByTag((ViewGroup) child, tag));
            }

            final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(tag)) {
                views.add(child);
            }

        }
        return views;
    }

    private void uzenetBeleABoxba(Uzenet uzi, String uzenetID) {
        TextView tv = new TextView(FoablakActivity.this);
        tv.setTag(uzenetID);
        tv.setText(uzi.getUzenetSzovege() + "\n........................................");
        tv.setTextColor(getApplicationContext().getResources().getColor(R.color.primary_text));
        tv.setPadding(0, 0, 0, DPtoPX(40));
        tv.setOnLongClickListener(FoablakActivity.this);
        llUzenetek.addView(tv, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
            detachDatabaseReadListener();
            ll.removeAllViews();
            llUzenetek.removeAllViews();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    private void ujelemHozzaadas() {
        // a fleugró ablak
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoablakActivity.this);
        alertDialog.setTitle("Mit kell venni?");
        //alertDialog.setMessage("termék neve");
        final EditText input = new EditText(FoablakActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.requestFocus();
        alertDialog.setView(input);
        alertDialog.setIcon(android.R.drawable.ic_input_add);
        alertDialog.setPositiveButton("Hozzáadás",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // adatbázisba írás
                        BevasarloListaElem e = new BevasarloListaElem(false, input.getText().toString().trim());
                        bevasarlolistaRef.push().setValue(e);

                        //billentyűzet elrejtése
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    }
                });
        alertDialog.setNegativeButton("Mégsem",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        // hide keyboard
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialog.cancel();
                    }
                });

        alertDialog.show();
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }


    private int DPtoPX(int sizeInDP) {
        int sizeInPixel = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, sizeInDP, getResources()
                        .getDisplayMetrics());
        return sizeInPixel;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.foablak, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_new_message) {

            ujUzenetKuldese();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void ujUzenetKuldese() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoablakActivity.this);
        alertDialog.setTitle("Mit üzennél?");
        //alertDialog.setMessage("termék neve");

        final EditText input = new EditText(FoablakActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        input.setLayoutParams(lp);

        alertDialog.setView(input);
        alertDialog.setIcon(android.R.drawable.ic_input_add);

        alertDialog.setPositiveButton("Küldés",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Uzenet uzi = new Uzenet();
                        uzi.setUzenetSzovege(input.getText().toString().trim() + " -" + mNinckName);
                        uzenetekRef.push().setValue(uzi);

                        //billentyűzet elrejtése
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);


                    }
                });

        alertDialog.setNegativeButton("Mégsem",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                        dialog.cancel();
                    }
                });

        alertDialog.show();
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_send) {

        } else if (id == R.id.logOut) {
            signOut();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void signOut() {
        AuthUI.getInstance().signOut(this);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.rlListaelemHozzadas) {
            ujelemHozzaadas();
        } else {

            CheckBox ch = (CheckBox) view;
            BevasarloListaElem e = new BevasarloListaElem();
            if (ch.isChecked()) {
                //ha nem volt bepipálva akkor - pipa be, szöveg hozzáfűz és átrakjuk a kész DB be
                e.setChecked(true);
                e.setValue(ch.getText().toString() + " - pipa by " + mNinckName);

                //keszBevasarlolistaRef.child(ch.getTag().toString()).setValue(e);
                keszBevasarlolistaRef.push().setValue(e);
                bevasarlolistaRef.child(ch.getTag().toString()).removeValue();
            } else {
                //ha be volt pipálva, akkor visszatesszük a bevlistába
                e.setChecked(false);
                String s = ch.getText().toString();
                e.setValue(s.substring(0, s.length() - 13));

                //bevasarlolistaRef.child(ch.getTag().toString()).setValue(e);
                bevasarlolistaRef.push().setValue(e);
                keszBevasarlolistaRef.child(ch.getTag().toString()).removeValue();
            }
        }
    }

    @Override
    public boolean onLongClick(final View view) {

        if (view instanceof CheckBox) {
            final CheckBox ch = (CheckBox) view;
            //Toast.makeText(getApplicationContext(), ch.getText().toString() + " törölve", Toast.LENGTH_LONG).show();
            ch.animate().scaleY(3f).scaleX(3f).setDuration(3000);
            new CountDownTimer(2000, 100) {
                @Override
                public void onTick(long l) {
                }

                @Override
                public void onFinish() {
                    if (ch.isChecked()) {
                        keszBevasarlolistaRef.child(ch.getTag().toString()).removeValue();
                    } else {
                        bevasarlolistaRef.child(ch.getTag().toString()).removeValue();
                    }
                }
            }.start();
        } else if (view instanceof TextView) {
            final TextView tv = (TextView) view;
            tv.animate().scaleY(3f).scaleX(3f).setDuration(3000);
            new CountDownTimer(2000, 100) {
                @Override
                public void onTick(long l) {
                }

                @Override
                public void onFinish() {
                    // üzenet törlése az adatbázisból, majd az meghívja a TV törlését is
                    uzenetekRef.child(tv.getTag().toString()).removeValue();
                }
            }.start();
        }
        Toast.makeText(this, "üzenet törölve", Toast.LENGTH_SHORT).show();
        return true;
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
