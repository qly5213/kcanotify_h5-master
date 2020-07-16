package com.antest1.kcanotify.h5;

import android.text.TextUtils;

import org.json.JSONObject;

public class KanCollUtils {
	private static final int[] resource = new int[] { 6657, 5699, 3371, 8909, 7719, 6229, 5449, 8561, 2987, 5501, 3127, 9319, 4365, 9811, 9927, 2423, 3439, 1865, 5925, 4409, 5509, 1517, 9695, 9255, 5325, 3691, 5519, 6949, 5607, 9539, 4133, 7795, 5465, 2659, 6381, 6875, 4019, 9195, 5645, 2887, 1213, 1815, 8671, 3015, 3147, 2991, 7977, 7045, 1619, 7909, 4451, 6573, 4545, 8251, 5983, 2849, 7249,
			7449, 9477, 5963, 2711, 9019, 7375, 2201, 5631, 4893, 7653, 3719, 8819, 5839, 1853, 9843, 9119, 7023, 5681, 2345, 9873, 6349, 9315, 3795, 9737, 4633, 4173, 7549, 7171, 6147, 4723, 5039, 2723, 7815, 6201, 5999, 5339, 4431, 2911, 4435, 3611, 4423, 9517, 3243 };
	static String apiData = null;
	static JSONObject apiJson = null;
	public static void main(String[] args) {
//		System.out.println(getShipImgName("card", "405"));
	}
	
	public static String getShipImgName(String type, String apiId, String apiShipName) {

		String ntype = type;
		String seed = "ship_" + ntype;
		String cipherNum = createCipherNum(apiId, seed);
		String padApiId = String.format("%04d", Integer.parseInt(apiId));
		if(TextUtils.isEmpty(apiShipName)){
            return padApiId + "_" + cipherNum;
        } else {
            return padApiId + "_" + cipherNum + "_" + apiShipName;
        }
	}

	private static String createCipherNum(String apiId, String seed) {

		int id = Integer.parseInt(apiId);
		int seedKey = createCipherNumKey(seed);
		int seedLength = null == seed || 0 == seed.length() ? 1 : seed.length();
		return String.valueOf(17 * (id + 7) * resource[(seedKey + id * seedLength) % 100] % 8973 + 1000);
	}

	private static int createCipherNumKey(String seed) {
		int seedKey = 0;
		for (int i = 0; i < seed.length(); i++) {
			int tmp = seed.charAt(i);
			seedKey += tmp;
		}
		return seedKey;
	}
}
