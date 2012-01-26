package javarestclient;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;

public class JavaRestClient {
    final static String webServiceServer = "https://ws.agiloffice.fr";
    public static String cookieStr;
    public static WebResource webResource;
    public static ClientResponse response;

    public static void main(String[] args) {
        setFakeTrustManager();

        try {
            Client client = Client.create();

            // Identification sur le web_service en tant que société
            webResource = client.resource(webServiceServer + "/demo/session_firm_login?password=xxxxxx");
            response = webResource.
                    accept("application/json").
                    post(ClientResponse.class, "");
            if (response.getStatus() == 200) {
                System.out.println("Ok : identification effectuée");
                List cookies = response.getCookies();
                for(int i = 0; i < cookies.size(); i ++){
                    String cookie = cookies.toArray()[i].toString();
                    if(cookie.contains("_agil_ws_session_id")){
                        cookieStr = cookie.split(";")[0];
                    }
                }

                // Raccrochage du poste 8003
                webResource = client.resource(webServiceServer + "/demo/phone_hangup?phone=8003");
                response = webResource.
                        accept("application/json").
                        cookie(new Cookie(cookieStr.split("=")[0], cookieStr.split("=")[1])).
                        post(ClientResponse.class, "");
                if (response.getStatus() == 200) {
                    System.out.println("Ok : raccrochage du poste effectué : " + response.getEntity(String.class));
                } else {
                    System.out.println("Erreur " + response.getStatus() + " : poste non raccroché : " + response.getEntity(String.class));
                }

                // Récupération de l'adresse du serveur de push
                webResource = client.resource(webServiceServer + "/demo/push_server_url");
                response = webResource.
                        accept("application/json").
                        cookie(new Cookie(cookieStr.split("=")[0], cookieStr.split("=")[1])).
                        get(ClientResponse.class);
                if (response.getStatus() == 200) {
                    System.out.println("Ok : url du push_server récupérée : " + response.getEntity(String.class));
                } else {
                    System.out.println("Erreur " + response.getStatus() + " : url du push_server non récupérée : " + response.getEntity(String.class));
                }

                // Envoi d'un fax
                File file = new File("/root/ws_client/document.pdf");
                webResource = client.resource(webServiceServer + "/demo/send_fax?from=0123456789&to=9876543210");
                FormDataMultiPart fdmp = new FormDataMultiPart();
                if (file != null) {
                    fdmp.bodyPart(new FileDataBodyPart("file", file, new MediaType("application", "pdf")));
                }
                fdmp.bodyPart(new FormDataBodyPart("name", "ingredientName"));
                fdmp.bodyPart(new FormDataBodyPart("description", "ingredientDesc"));
                response = webResource.
                        type(MediaType.MULTIPART_FORM_DATA_TYPE).
                        header("Content-Type", "application/pdf").
                        accept("application/json").
                        cookie(new Cookie(cookieStr.split("=")[0], cookieStr.split("=")[1])).
                        post(ClientResponse.class, fdmp);
                if (response.getStatus() == 200) {
                    String responseStr = response.getEntity(String.class);
                    System.out.println("Ok : fax envoyé : " + responseStr);
                    String fax_id = responseStr.split(":")[1].replace("}", "");

                    // Récupération de l'état du fax envoyé
                    webResource = client.resource(webServiceServer + "/demo/get_fax_status?fax_id=" + fax_id);
                    response = webResource.
                            accept("application/json").
                            cookie(new Cookie(cookieStr.split("=")[0], cookieStr.split("=")[1])).
                            get(ClientResponse.class);
                    if (response.getStatus() == 200) {
                        System.out.println("Ok : statut du fax récupéré : " + response.getEntity(String.class));
                    } else {
                        System.out.println("Erreur " + response.getStatus() + " : statut du fax non récupérée : " + response.getEntity(String.class));
                    }
                } else {
                    System.out.println("Erreur " + response.getStatus() + " : fax non envoyé : " + response.getEntity(String.class));
                }
            } else {
                System.out.println("Erreur " + response.getStatus() + " : identification échouée : " + response.getEntity(String.class));
            }
        } catch (Exception e) {
            System.out.println("Erreur : identification échouée : " + e.getMessage());
        }
    }

    private static void setFakeTrustManager(){
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { /* Trust always */ }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { /* Trust always */ }
            }
        };
        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            // Create empty HostnameVerifier
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String arg0, SSLSession arg1) { return true; }
            };
            try {
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
            } catch (KeyManagementException ex) { ex.printStackTrace(); }
        } catch (NoSuchAlgorithmException ex) { ex.printStackTrace(); }
    }
}
