package com.antest1.kcanotify.h5;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;

import static com.antest1.kcanotify.h5.KcaConstants.PREF_KCA_LANGUAGE;
import static com.antest1.kcanotify.h5.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.h5.LocaleUtils.getKcWikiLocaleCode;

public class SubTitleUtils {
    private static final int[] VOICE_KEYS = new int[]{2475, 6547, 1471, 8691, 7847, 3595, 1767, 3311, 2507, 9651, 5321, 4473, 7117, 5947, 9489, 2669, 8741, 6149, 1301, 7297, 2975, 6413, 8391, 9705, 2243, 2091, 4231, 3107, 9499, 4205, 6013, 3393, 6401, 6985, 3683, 9447, 3287, 5181, 7587, 9353, 2135, 4947, 5405, 5223, 9457, 5767, 9265, 8191, 3927, 3061, 2805, 3273, 7331};

    private static ArrayList<SparseIntArray> voiceList = new ArrayList<>();
    private static HashMap<String, Integer> shipGraph = new HashMap<>();
    private static SparseArray<String> shipName = new SparseArray<>();
    private static JSONObject subTitleJson = new JSONObject();
    public static void initVoiceMap(){
        for(int no = 1; no<= 1000; no++){
            SparseIntArray shipMap = new SparseIntArray();
            for(int vno = 1; vno<= VOICE_KEYS.length; vno++){
                Integer key = encodeSoundFilename(no, vno);
                shipMap.put(key, vno);
            }
            shipMap.put(141, 141);
            shipMap.put(241, 241);
            voiceList.add(shipMap);
        }
    }
    public static void initShipGraph(String respData){
        try {
            JSONObject respJson = new JSONObject(respData.substring(7));
            JSONObject apiDataJson = respJson.getJSONObject("api_data");
            if(apiDataJson.has("api_mst_shipgraph")){
                JSONArray jsonArray = apiDataJson.getJSONArray("api_mst_shipgraph");
                JSONArray jsonShipArray = apiDataJson.getJSONArray("api_mst_ship");
                for(int i = 0; i<jsonArray.length(); i++){
                    JSONObject shipJson = jsonArray.getJSONObject(i);
                    shipGraph.put(shipJson.getString("api_filename"), shipJson.getInt("api_id"));
                    JSONObject shipNameJson = jsonShipArray.getJSONObject(i);
                    shipName.put(shipNameJson.getInt("api_id"), shipNameJson.getString("api_name"));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void initSubTitle(){
        Context context = KcaApplication.getInstance();
        KcaDownloader downloader = KcaUtils.getSubTitleInfoDownloader(context);
        String locale = getStringPreferences(context, PREF_KCA_LANGUAGE);
        String localCode = getKcWikiLocaleCode(locale);
        String subTitleFileName = "subTitle-" + localCode;
        try {
            String filePath = context.getFilesDir().getPath();
            File file = new File(filePath + File.separator + subTitleFileName);
            if(!file.exists()){
                file.createNewFile();
            }
            FileInputStream in= context.openFileInput(subTitleFileName);
            BufferedReader reader= new BufferedReader(new InputStreamReader(in));
            String str = null;
            StringBuffer oriSubTitle = new StringBuffer();
            while((str=reader.readLine())!=null){
                oriSubTitle.append(str);
            }
            reader.close();
            in.close();
            if(oriSubTitle.toString().equals("")){
                oriSubTitle = oriSubTitle.append("{}");
            }
            final JSONObject oriSubTitleJson = new JSONObject(oriSubTitle.toString());
            if(!oriSubTitleJson.has("version")) {
                Thread t = new Thread(() -> {
                    final Call<String> rv_data = downloader.getSubTitle(localCode);
                    String response = getResultFromCall(rv_data);
                    try {
                        if (response != null) {
                            subTitleJson = new JSONObject(response);
                            FileOutputStream out = context.openFileOutput(subTitleFileName, Context.MODE_PRIVATE);
                            BufferedWriter  writer=new BufferedWriter(new OutputStreamWriter(out));
                            writer.write(response);
                            writer.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                t.start();
            } else {
                String version = oriSubTitleJson.getString("version");
                Thread t = new Thread(() -> {
                    final Call<String> rv_data = downloader.getSubTitleDiff(localCode, version);
                    String response = getResultFromCall(rv_data);
                    try {
                        if (response != null) {
                            if(!response.equals("[]")) {
                                JSONObject updateSubTitleJson = new JSONObject(response);

                                Iterator<String> sIterator = updateSubTitleJson.keys();
                                while(sIterator.hasNext()){
                                    String key = sIterator.next();
                                    if(key.equals("version")){
                                        Log.d("KCVA", "update subtitle：：" + key);
                                        String newVersion = updateSubTitleJson.getString(key);
                                        oriSubTitleJson.put("version", newVersion);
                                    } else {
                                        JSONObject value = updateSubTitleJson.getJSONObject(key);
                                        if(oriSubTitleJson.has(key)) {
                                            JSONObject oriValue = oriSubTitleJson.getJSONObject(key);
                                            Iterator<String> subIterator = value.keys();
                                            while (subIterator.hasNext()) {
                                                String subKey = subIterator.next();
                                                String subValue = value.getString(subKey);
                                                oriValue.put(subKey, subValue);
                                            }
                                            oriSubTitleJson.put(key, oriValue);
                                        } else {
                                            oriSubTitleJson.put(key, value);
                                        }
                                    }
                                }
                                subTitleJson = oriSubTitleJson;
                                FileOutputStream out = context.openFileOutput(subTitleFileName, Context.MODE_PRIVATE);
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
                                writer.write(subTitleJson.toString());
                                writer.close();
                            } else {
                                subTitleJson = oriSubTitleJson;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                t.start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getSubTitle(String path){
        String result = null;
        try {
            String pattern = "/kcs/sound/(.*?)/(.*?).mp3";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(path);
            if (m.find()) {
                String shipCode = m.group(1);
                switch (shipCode) {
                    case "kc9998":
                    case "kc9999":
                    case "titlecall":
                        break;
                    default:
                        String voiceFileName = m.group(2);
                        int apiId = shipGraph.get(shipCode.substring(2));
                        SparseIntArray voiceArr = voiceList.get(apiId - 1);
                        int voiceId = voiceArr.get(Integer.parseInt(voiceFileName));
                        String shipNameStr = shipName.get(apiId);
                        String quote = subTitleJson.getJSONObject(String.valueOf(apiId)).getString(String.valueOf(voiceId));
                        result = shipNameStr + ":" + quote;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    private static String getResultFromCall(Call<String> call) {
        KcaRequestThread thread = new KcaRequestThread(call);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return thread.getResult();
    }


    private static Integer encodeSoundFilename(Integer shipId, Integer voiceId){
        return (shipId + 7) * 17 * VOICE_KEYS[voiceId - 1] % 99173 + 100000;
    }

    public static HashMap<String, String> initServiceHost(){
        HashMap<String, String> serverMap = new HashMap<>();
        serverMap.put("203.104.209.71", "横須賀鎮守府");
        serverMap.put("203.104.209.87", "呉鎮守府");
        serverMap.put("125.6.184.215", "佐世保鎮守府");
        serverMap.put("203.104.209.183", "舞鶴鎮守府");
        serverMap.put("203.104.209.150", "大湊警備府");
        serverMap.put("203.104.209.134", "トラック泊地");
        serverMap.put("203.104.209.167", "リンガ泊地");
        serverMap.put("203.104.209.199", "ラバウル基地");
        serverMap.put("125.6.189.7", "ショートランド泊地");
        serverMap.put("125.6.189.39", "ブイン基地");
        serverMap.put("125.6.189.71", "タウイタウイ泊地");
        serverMap.put("125.6.189.103", "パラオ泊地");
        serverMap.put("125.6.189.135", "ブルネイ泊地");
        serverMap.put("125.6.189.167", "単冠湾泊地");
        serverMap.put("125.6.189.215", "幌筵泊地");
        serverMap.put("125.6.189.247", "宿毛湾泊地");
        serverMap.put("203.104.209.23", "鹿屋基地");
        serverMap.put("203.104.209.39", "岩川基地");
        serverMap.put("203.104.209.55", "佐伯湾泊地");
        serverMap.put("203.104.209.102", "柱島泊地");
        return serverMap;
    }
}
