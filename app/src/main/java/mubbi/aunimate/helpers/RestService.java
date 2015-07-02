package mubbi.aunimate.helpers;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class RestService {

    public void getJSONSounds(){

        //service api url
        String url = "http://localhost/aunimate/getAll.php";

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

            String[] soundsArray = new String[respJSON.length()];

            for (int i = 0; i < respJSON.length(); i++){
                JSONObject obj = respJSON.getJSONObject(i);

                String id = obj.getString("id");
                String autor = obj.getString("autor");
                String titulo = obj.getString("titulo");

                soundsArray[i] = id + "-" + autor + "-" + titulo;
            }

        }catch (Exception e){

        }
    }
}
