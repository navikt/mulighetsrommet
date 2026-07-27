package no.nav.mulighetsrommet.api.domain.utdanning

interface UtdanningsprogramRepository {
    fun save(utdanningsprogram: Utdanningsprogram)

    fun findByProgramomradekode(programomradekode: String): Utdanningsprogram?
}
