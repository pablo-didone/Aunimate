package mubbi.aunimate.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import mubbi.aunimate.R;
import mubbi.aunimate.adapters.SoundAdapter;
import mubbi.aunimate.interfaces.PlayPauseListener;
import mubbi.aunimate.model.Sound;


public class MainActivity extends Activity {

    private ListView lstSounds;
    private File appRootDirectory;
    private ProgressDialog downloadProgressDialog, loadSoundsProgressDialog;
    private ArrayList<Sound> soundList = new ArrayList<>();
    private AdView adView;
    private MediaPlayer mp;
    private EditText txtFilter;
    private SoundAdapter adaptador;
    private ArrayList<Sound> filterArray = new ArrayList<Sound>();
    private RelativeLayout lytRetry;
    private LinearLayout lytMainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lytMainLayout = (LinearLayout)findViewById(R.id.lytMainLayout);
        lytRetry = (RelativeLayout)findViewById(R.id.lytRetry);

        //Bloqueo el teclado para que no aparezca al iniciar la activity
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //Directorio raiz de la aplicación
        appRootDirectory = getExternalFilesDir(null);

        //Cargo la lista con los audios
        loadSounds();

        //Creo el directorio para guardar los archivos de sonido.
        createDirectory();

        //LLamo a la creación del banner de publicidad
        createAdView();

    }

    public void initFilter(){
        txtFilter = (EditText)findViewById(R.id.txtFilter);

        txtFilter.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int textLength = txtFilter.length();
                filterArray.clear();

                for(int i = 0; i < soundList.size(); i++){
                    String fullName = soundList.get(i).getAutor() + " - " + soundList.get(i).getTitle();
                    if(textLength <= fullName.length()){
                        if(txtFilter.getText().toString().equalsIgnoreCase((String)fullName.subSequence(0,textLength))){
                            filterArray.add(soundList.get(i));
                        }
                    }
                }
                SoundAdapter filterAdapter = new SoundAdapter(MainActivity.this, filterArray);
                filterAdapter.attachListener(new PlayPauseListener() {
                    @Override
                    public void onPlaySound(String id) {
                        playSound(id);
                    }

                    @Override
                    public void onPauseSound() {

                    }
                });
                lstSounds.setAdapter(filterAdapter);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onPause() {
        adView.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        adView.resume();
    }

    @Override
    public void onDestroy() {
        adView.destroy();
        super.onDestroy();
    }

    public void createAdView(){
        //Crear AdView
        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.ad_view_id));
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.setBackgroundColor(getResources().getColor(R.color.light_blue));

        //Obtener el layout
        LinearLayout layout = (LinearLayout)findViewById(R.id.lytMainLayout);
        // Añadirle adView.
        layout.addView(adView);

        // Iniciar una solicitud genérica.
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR) //Emulador
                .addTestDevice("43D0F2CAF5EA4D55F9F4EAE80E8AE985") //Celu Pela
                .addTestDevice("2166C08C201B0AF963525F352149A1B2")// Celu gordo Almada.
                .build();

        // Cargar adView con la solicitud de anuncio.
        adView.loadAd(adRequest);
    }

    public void loadSounds(){
        loadSoundsProgressDialog = new ProgressDialog(MainActivity.this);
        loadSoundsProgressDialog.setMessage("Descargando lista de audios");
        loadSoundsProgressDialog.setIndeterminate(true);
        loadSoundsProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadSoundsProgressDialog.setCancelable(false);
        LoadSoundsTask loadSoundsTask = new LoadSoundsTask();
        loadSoundsTask.execute();
    }

    public void createDirectory(){

        boolean sdDisponible = false;
        boolean sdAccesoEscritura = false;

        //Comprobamos el estado de la memoria externa (tarjeta SD)
        String estado = Environment.getExternalStorageState();

        if (estado.equals(Environment.MEDIA_MOUNTED))
        {
            sdDisponible = true;
            sdAccesoEscritura = true;
        }
        else if (estado.equals(Environment.MEDIA_MOUNTED_READ_ONLY))
        {
            sdDisponible = true;
            sdAccesoEscritura = false;
        }
        else
        {
            sdDisponible = false;
            sdAccesoEscritura = false;
        }

        //Si la memoria externa está disponible y se puede escribir
        if (sdDisponible && sdAccesoEscritura)
        {
            try
            {
                File f = new File(appRootDirectory.getAbsolutePath() + "/appSounds"); //Busco carpeta de sonidos

                //Si no existe, la creo.
                if (!f.exists()){
                    f.mkdirs();
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    //Carga la lista de sonidos
    public void showSoundsList(){

        lstSounds = (ListView)findViewById(R.id.lstSounds);
        lstSounds.setTextFilterEnabled(true);

        adaptador = new SoundAdapter(this, soundList);
        adaptador.attachListener(new PlayPauseListener() {

            @Override
            public void onPlaySound(String id) {
                playSound(id);
            }

            @Override
            public void onPauseSound() {

            }
        });

        lstSounds.setAdapter(adaptador);

        lstSounds.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = ((Sound)parent.getItemAtPosition(position)).getId();
                shareSound(selectedOption);
            }
        });
    }

    public void playSound(String id){
        //Busco el audio con el nombre pasado como parametro
        File audioFile = new File(appRootDirectory.getAbsolutePath() + "/appSounds/" + id + ".3ga" );

        if (audioFile.exists()){
            mp = MediaPlayer.create(this, Uri.parse(appRootDirectory.getAbsolutePath() + "/appSounds/" + id + ".3ga"));
            mp.start();
        }else{
            downloadSound(id, "PLAY");
        }
    }

    public void shareSound(String id){

        //Busco el audio con el nombre pasado como parametro
        File audioFile = new File(appRootDirectory.getAbsolutePath() + "/appSounds/" + id + ".3ga" );

        if (audioFile.exists()){
            Uri uri = Uri.fromFile(audioFile);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("audio/*");
            startActivity(Intent.createChooser(shareIntent, "Compartir en"));
        }else{
            downloadSound(id, "");
        }
    }

    public void downloadSound(String id, String nextTask){
        downloadProgressDialog = new ProgressDialog(MainActivity.this);
        downloadProgressDialog.setMessage("Descargando audio");
        downloadProgressDialog.setIndeterminate(true);
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        downloadProgressDialog.setCancelable(true);

        final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
        downloadTask.execute("http://elportaldetandil.com.ar/dev_www/azanoni/aunimate/sounds/" + id + ".3ga", id, nextTask);

        downloadProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });
    }

    public void showRetryPanel(){
        lytRetry.setVisibility(View.VISIBLE);
        lytMainLayout.setVisibility(View.GONE);

        ImageButton btnRetry = (ImageButton)findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoadSoundsTask loadSoundsTask = new LoadSoundsTask();
                loadSoundsTask.execute();
            }
        });
    }

    private class LoadSoundsTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            loadSoundsProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            //service api url
            String url = "http://elportaldetandil.com.ar/dev_www/azanoni/aunimate/getAll.php";

            // declarar un client http en este caso DefaultHttpClient
            HttpClient client = new DefaultHttpClient();

            //Como nuestro servicio es un metodo get usamos HTTPGet
            HttpGet request = new HttpGet(url);

            //Indico el tipo de datos que se van a intercambiar
            request.setHeader("content-type", "application/json");

            try{
                HttpResponse response = client.execute(request);  //Ejecutamos el request ya compuesto
                String respString = EntityUtils.toString(response.getEntity());

                JSONArray respJSON = new JSONArray(respString);

                for (int i = 0; i < respJSON.length(); i++){
                    JSONObject obj = respJSON.getJSONObject(i);

                    String id = obj.getString("id");
                    String autor = obj.getString("autor");
                    String titulo = obj.getString("titulo");

                    publishProgress(id, autor, titulo);
                }
                return "OK";
            }catch (Exception e){
                e.printStackTrace();
                return "FAIL";
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            Sound sound = new Sound(values[0], values[1], values[2]);
            soundList.add(sound);
        }

        @Override
        protected void onPostExecute(String s) {
            if(s.equals("FAIL")){
                showRetryPanel();
            }else{
                lytRetry.setVisibility(View.GONE);
                lytMainLayout.setVisibility(View.VISIBLE);
                showSoundsList();
            }

            loadSoundsProgressDialog.dismiss();

            //Agrego funcionalidad al filtro
            initFilter();
        }
    }


    private class DownloadTask extends AsyncTask<String, Integer, String>{

        private Context context;
        private PowerManager.WakeLock wakeLock;

        public DownloadTask(Context context){
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            String soundId = params[1];
            String nextTask = params[2];
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try{
                URL url = new URL(params[0]);
                connection = (HttpURLConnection)url.openConnection();
                connection.connect();

                if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                    return "FAIL";
                }

                //Tamaño del archivo. Para mostrar el porcentaje de descarga.
                int fileLength = connection.getContentLength();

                //Descargo el archivo y lo guardo en memoria.
                input = connection.getInputStream();
                output = new FileOutputStream(appRootDirectory.getAbsolutePath() + "/appSounds/" + soundId + ".3ga");

                byte data[] = new byte[4096];
                long total = 0;
                int count;

                while((count = input.read(data)) != -1){
                    //Si se cancela el proceso
                    if (isCancelled()){
                        input.close();
                        return null;
                    }
                    total += count;

                    //Si tengo el tamaño del archivo voy mostrando el progreso.
                    if (fileLength > 0){
                        publishProgress((int)(total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }
                return nextTask + soundId;
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try{
                    if (output != null){
                        output.close();
                    }
                    if (input != null){
                        input.close();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                if (connection != null){
                    connection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            wakeLock.acquire();
            downloadProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            downloadProgressDialog.setIndeterminate(false);
            downloadProgressDialog.setMax(100);
            downloadProgressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            Log.i("FRASES", s);
            wakeLock.release();
            downloadProgressDialog.dismiss();
            if(s.equals("FAIL")){
                Toast.makeText(context, "No se pudo descargar el audio", Toast.LENGTH_SHORT).show();
            }else if(s.startsWith("PLAY")){
                playSound(s.substring(4));
            }else{
                shareSound(s);
            }
        }
    }

}
