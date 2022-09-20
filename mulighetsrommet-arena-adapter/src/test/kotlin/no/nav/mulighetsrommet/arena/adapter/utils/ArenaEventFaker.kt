package no.nav.mulighetsrommet.arena.adapter.utils

import net.datafaker.Faker
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaOperation
import no.nav.mulighetsrommet.domain.arena.*
import java.util.*
import java.util.concurrent.TimeUnit

object EventFaker {

    private const val DEFAULT_NUM_OF_EVENTS = 10
    private val faker = Faker(Locale("nb", "NO"))

    fun generateFakeEventDataSet(numOfEvents: Int = DEFAULT_NUM_OF_EVENTS): Array<List<Any>> {
        val tiltakstyper = createFakeTiltakstypeEvents(numOfEvents)
        val tiltaksgjennomforing =
            createFakeTiltaksgjennomforingEvents(numOfEvents, tiltakstyper.map { it.TILTAKSKODE })
        val tiltaksdeltakere = createFakeTiltaksdeltakerEvents(numOfEvents)
        val saker = createFakeSakEvents(numOfEvents)
        return arrayOf(tiltakstyper, tiltaksgjennomforing, tiltaksdeltakere, saker)
    }

    fun createFakeTiltakstypeEvents(numOfEvents: Int = DEFAULT_NUM_OF_EVENTS): List<ArenaTiltak> {
        return (0..numOfEvents).map {
            ArenaTiltak(
                "tiltaksnavn$it",
                "tiltakskode$it",
                faker.date().past(365 * 10, TimeUnit.DAYS).toString(),
                faker.date().future(365 * 10, TimeUnit.DAYS).toString()
            )
        }
    }

    fun createFakeTiltaksgjennomforingEvents(
        numOfEvents: Int = DEFAULT_NUM_OF_EVENTS,
        tiltakskoder: List<String>
    ): List<ArenaTiltaksgjennomforing> {
        return (0..numOfEvents).map {
            ArenaTiltaksgjennomforing(
                it + 1,
                it + 1,
                tiltakskoder.random(),
                faker.date().past(365 * 10, TimeUnit.DAYS).toString(),
                faker.date().future(365 * 10, TimeUnit.DAYS).toString(),
                "tiltaksgjennomforingnavn$it",
                it + 1,
                JaNeiStatus.Ja,
                faker.random().nextInt(20)
            )
        }
    }

    fun createFakeTiltaksdeltakerEvents(numOfevents: Int = DEFAULT_NUM_OF_EVENTS): List<ArenaTiltakdeltaker> {
        return (0..numOfevents).map {
            ArenaTiltakdeltaker(
                it + 1,
                it + 1,
                it + 1,
                ProcessingUtils.ArenaDeltakerstauts.values().toList().random().toString(),
                faker.date().past(365 * 10, TimeUnit.DAYS).toString(),
                faker.date().future(365 * 10, TimeUnit.DAYS).toString(),
            )
        }
    }

    fun createFakeSakEvents(numOfEvents: Int = DEFAULT_NUM_OF_EVENTS): List<ArenaSak> {
        return (0..numOfEvents).map {
            ArenaSak(
                it+1,
                "TILT",
                faker.random().nextInt(2000, 2022),
                faker.random().nextInt(0, 1000)
            )
        }
    }

}
