package no.nav.mulighetsrommet.api.plugins

enum class IdPortenAmr {
    MinIdPin,
    MinIdOtc,
    MinIdApp,
    MinIdTotp,
    MinidWebAuthn,
    BankID,
    BankIDMobil,
    Buypass,
    Commfides,
    Eidas,
    SelfregisteredEmail,
    TestID,
    ;

    override fun toString() = when (this) {
        MinIdPin -> "Minid-PIN"
        MinIdOtc -> "Minid-OTC"
        MinIdApp -> "Minid-APP"
        MinIdTotp -> "Minid-TOTP"
        MinidWebAuthn -> "Minid-WEBAUTHN"
        BankID -> "BankID"
        BankIDMobil -> "BankID Mobil"
        Buypass -> "Buypass"
        Commfides -> "Commfides"
        Eidas -> "eIDAS"
        SelfregisteredEmail -> "Selfregistered-email"
        TestID -> "TestID"
    }

    companion object {
        fun fromString(value: String) = when (value) {
            "Minid-PIN" -> MinIdPin
            "Minid-OTC" -> MinIdOtc
            "Minid-APP" -> MinIdApp
            "Minid-TOTP" -> MinIdTotp
            "Minid-WEBAUTHN" -> MinidWebAuthn
            "BankID" -> BankID
            "BankID Mobil" -> BankIDMobil
            "Buypass" -> Buypass
            "Commfides" -> Commfides
            "eIDAS" -> Eidas
            "Selfregistered-email" -> SelfregisteredEmail
            "TestID" -> TestID
            else -> throw IllegalArgumentException("Unknown id-porten amr value: $value")
        }
    }
}
