package com.guentours.payment.gateway.flutterwave;

/**
 * ⚠️ Inférence best-effort du réseau mobile à partir du préfixe du numéro.
 * Cette table n'est PAS garantie exhaustive ni à jour (portabilité du numéro, nouveaux préfixes...).
 * À valider/remplacer par une vraie source de vérité si le taux d'échec est significatif en prod.
 */
class MobileNetworkInferrer {

    static String infer(MobileMoneyRegion region, String mobileNumber) {
        String digits = mobileNumber.replaceAll("[^0-9]", "");
        return switch (region) {
            case GHANA -> inferGhana(digits);
            case UGANDA -> inferUganda(digits);
            case ZAMBIA -> inferZambia(digits);
            default -> throw new IllegalArgumentException("Réseau non requis pour " + region);
        };
    }

    private static String inferGhana(String digits) {
        // ⚠️ table non confirmée — à valider
        if (digits.matches(".*(24|25|53|54|55|59).*")) return "MTN";
        if (digits.matches(".*(20|50).*")) return "VODAFONE";
        if (digits.matches(".*(26|27|56|57).*")) return "AIRTELTIGO";
        return "MTN"; // fallback par défaut
    }

    private static String inferUganda(String digits) {
        if (digits.matches(".*(77|78|76|39).*")) return "MTN";
        if (digits.matches(".*(70|75|74).*")) return "AIRTEL";
        return "MTN";
    }

    private static String inferZambia(String digits) {
        if (digits.matches(".*(96|76).*")) return "MTN";
        if (digits.matches(".*(97|77).*")) return "AIRTEL";
        return "MTN";
    }
}