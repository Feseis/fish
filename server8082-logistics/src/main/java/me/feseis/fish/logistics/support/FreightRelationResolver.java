package me.feseis.fish.logistics.support;

/**
 * 寄收关系判定：只看 adcode 前缀，不落表。
 *
 * <p>前 2 位为省、前 4 位为市。直辖市（11 北京 / 12 天津 / 31 上海 / 50 重庆）的区县
 * 前 4 位不同但仍属同一城市，因此前 2 位属于直辖市即判同城。
 */
public final class FreightRelationResolver {

    public static final int SAME_CITY = 1;
    public static final int SAME_PROVINCE = 2;
    public static final int CROSS_PROVINCE = 3;

    private static final java.util.Set<String> MUNICIPALITIES = java.util.Set.of("11", "12", "31", "50");

    private FreightRelationResolver() {
    }

    public static int resolve(String senderAdcode, String receiverAdcode) {
        String senderProvince = senderAdcode.substring(0, 2);
        String receiverProvince = receiverAdcode.substring(0, 2);
        String senderCity = senderAdcode.substring(0, 4);
        String receiverCity = receiverAdcode.substring(0, 4);

        if (!senderProvince.equals(receiverProvince)) {
            return CROSS_PROVINCE;
        }
        if (senderCity.equals(receiverCity)) {
            return SAME_CITY;
        }
        return MUNICIPALITIES.contains(senderProvince) ? SAME_CITY : SAME_PROVINCE;
    }
}
