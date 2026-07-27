package no.nav.mulighetsrommet.api.domain.utdanning

/**
 * Nus-kodene har blitt bestemt av datavarehuset (DVH/Fag) og er ikke noe som følger med fra
 * utdanning.no sitt api. Fag/DVH må være involvert hvis/når det oppstår nye programområder som
 * skal støttes.
 */
object UtdanningsprogramNusKoder {
    private val nusKoderForProgramomradekode = mapOf(
        "BABAT1----" to listOf("3571"),
        "ELELE1----" to listOf("3551"),
        "FDFBI1----" to listOf("3165"),
        "HSHSF1----" to listOf("3699"),
        "DTDTH1----" to listOf("3169"),
        "IMIKM1----" to listOf("3541"),
        "NANAB1----" to listOf("3799"),
        "RMRMF1----" to listOf("3581"),
        "SRSSR1----" to listOf("3429"),
        "TPTIP1----" to listOf("3559"),
    )

    fun forProgramomradekode(programomradekode: String): List<String>? = nusKoderForProgramomradekode[programomradekode]
}
