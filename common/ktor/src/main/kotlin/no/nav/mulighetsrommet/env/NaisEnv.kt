package no.nav.mulighetsrommet.env

enum class NaisEnv(val clusterName: String) {

    Local("local"),
    DevGCP("dev-gcp"),
    ProdGCP("prod-gcp"),
    ;

    companion object {
        fun current(): NaisEnv = when (System.getenv("NAIS_CLUSTER_NAME")) {
            DevGCP.clusterName -> DevGCP
            ProdGCP.clusterName -> ProdGCP
            else -> Local
        }
    }

    fun isLocal(): Boolean = this === Local

    fun isDevGCP(): Boolean = this === DevGCP

    fun isProdGCP(): Boolean = this === ProdGCP
}
