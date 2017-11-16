import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {
    private static String ROOT; //output directory
    private static String FILE_NAME;
    private static String baseUrl = "https://graph.facebook.com/v2.8/{groupId}/feed";
    private static String auth;
    private static final String earliestDateString = "2017-10-01";
    private static JSONArray searchParamsPos;
    private static JSONArray searchParamsNeg;
    public static void main(String[] args) {
        loadParams();
        writeResponse();
        System.out.println("File " + FILE_NAME + " outputted to directory " + ROOT);
    }

    /**
     * loads params from params.json file in resources folder
     */
    private static void loadParams() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try(
                InputStream is = classLoader.getResourceAsStream("params.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line = reader.readLine();
            StringBuilder sb = new StringBuilder();
            while(line != null){
                sb.append(line).append("\n");
                line = reader.readLine();
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            auth = "OAuth " + jsonObject.get("auth_token");
            ROOT =  jsonObject.getString("file_path");
            FILE_NAME = jsonObject.getString("file_name");
            baseUrl = baseUrl.replace("{groupId}",jsonObject.getString("group_id"));
            searchParamsPos = jsonObject.getJSONArray("search_params_pos");
            searchParamsNeg = jsonObject.getJSONArray("search_params_neg");
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("File params.json not found in resources, please add");
        }
    }
    private static FBResponse getResponse(String uri) throws URISyntaxException {
        String json = "";
        int responseCode = 0;
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(uri);
            request.addHeader("Authorization",auth);
            HttpResponse response = client.execute(request);
            responseCode = response.getStatusLine().getStatusCode();
            if(responseCode != 200 && responseCode != 400 && responseCode != 401) {
                throw new RuntimeException("Network Error");
            }
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            json = result.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject obj = new JSONObject(json);
        if(responseCode != 200) {
            String errorMessage = obj.getJSONObject("error").getString("message");
            throw new RuntimeException(errorMessage);
        }
        JSONArray array = obj.getJSONArray("data");
        StringBuilder sb = new StringBuilder();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for(int i = 0; i < array.length(); i++) {
            JSONObject post = array.getJSONObject(i);
            try {
                String message = post.getString("message");
                Date datePosted;
                try {
                    datePosted = dateFormat.parse(post.getString("updated_time"));
                } catch (ParseException e) {
                    e.printStackTrace();
                    continue;
                }
                if(include(message)) {
                    sb.append(message);
                    sb.append("\n");
                    sb.append("Date Posted: ");
                    sb.append(dateFormat.format(datePosted));
                    String id = post.getString("id");
                    String[] ids = id.split("_");
                    sb.append("\nhttps://www.facebook.com/");
                    sb.append(ids[0]);
                    sb.append("/posts/");
                    sb.append(ids[1]);
                    sb.append("\n----------------------------------------\n");
                }
            } catch (JSONException e) {
                // skip this one, as it is not a post
            }
        }
        Date date = null;
        try {
            date = dateFormat.parse( array.getJSONObject(array.length()-1).getString("updated_time"));
            System.out.println(dateFormat.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new FBResponse(sb.toString(), date, obj.getJSONObject("paging").getString("next"));
    }
    private static void writeResponse() {
        File root = new File(ROOT);
        File outPath = new File(root,FILE_NAME);
        FBResponse pageResponse;
        Date earliestDate;
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            earliestDate = df.parse(earliestDateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
        try {
            pageResponse = getResponse(baseUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        try(BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(outPath))) {
            while(pageResponse.date.compareTo(earliestDate) > 0 ) {
                outStream.write(pageResponse.messageResponse.getBytes());
                try {
                    pageResponse = getResponse(pageResponse.nextUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean include(String string) {
        String message = string.toLowerCase();

        for(Object s : searchParamsNeg) {
            if(message.contains(s.toString())) {
                return false;
            }
        }

        for(Object s : searchParamsPos) {
            if(message.contains(s.toString())) {
                return true;
            }
        }

        return false;
    }

    private static class FBResponse {
        String messageResponse;
        Date date;
        String nextUri;
        FBResponse(String messageResponse, Date date, String nextUri) {
            this.date = date;
            this.messageResponse = messageResponse;
            this.nextUri = nextUri;
        }
    }
}
