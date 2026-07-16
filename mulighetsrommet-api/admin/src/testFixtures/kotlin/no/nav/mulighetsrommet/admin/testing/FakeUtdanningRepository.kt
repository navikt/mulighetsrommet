package no.nav.mulighetsrommet.admin.testing

import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramRepository

class FakeUtdanningRepository : UtdanningsprogramRepository {
    val utdanningsprogrammer = mutableMapOf<String, Utdanningsprogram>()

    override fun save(utdanningsprogram: Utdanningsprogram) {
        utdanningsprogrammer[utdanningsprogram.programomradekode] = utdanningsprogram
    }

    override fun findByProgramomradekode(programomradekode: String): Utdanningsprogram? {
        return utdanningsprogrammer[programomradekode]
    }
}
