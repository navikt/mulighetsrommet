package no.nav.mulighetsrommet.api.sanity

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityParam
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.veilederflate.models.Oppskrift
import no.nav.mulighetsrommet.model.NavIdent
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class SanityService(
    private val sanityClient: SanityClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val sanityTiltaksgjennomforingerCache: Cache<String, List<SanityTiltaksgjennomforing>> =
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .recordStats()
            .build()

    private val sanityTiltaksgjennomforingCache: Cache<UUID, SanityTiltaksgjennomforing> =
        Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .recordStats()
            .build()

    private val sanityTiltaksgjennomforingQuery = """
        {
              _id,
              tiltakstype->{
                _id,
                tiltakstypeNavn,
              },
              tiltaksgjennomforingNavn,
              "tiltaksnummer": tiltaksnummer.current,
              beskrivelse,
              stedForGjennomforing,
              redaktor[]->,
              kontaktpersoner[]{navKontaktperson->, "enheter": coalesce(enheter[]->nummer.current, [])},
              "fylke": fylke->nummer.current,
              "enheter": coalesce(enheter[]->nummer.current, []),
              arrangor -> {
                _id,
                navn,
                organisasjonsnummer,
                kontaktpersoner[] -> {
                  _id,
                  navn,
                  telefon,
                  epost,
                  beskrivelse,
                }
              },
              faneinnhold {
                forHvemInfoboks,
                forHvem,
                detaljerOgInnholdInfoboks,
                detaljerOgInnhold,
                pameldingOgVarighetInfoboks,
                pameldingOgVarighet,
                kontaktinfoInfoboks,
                kontaktinfo,
                lenker,
                oppskrift
              },
              delingMedBruker,
            }
        """

    suspend fun getTiltak(
        id: UUID,
        perspective: SanityPerspective,
        cacheUsage: CacheUsage,
    ): SanityTiltaksgjennomforing {
        sanityTiltaksgjennomforingCache.getIfPresent(id)?.let {
            if (cacheUsage == CacheUsage.UseCache) {
                return@getTiltak it
            }
        }

        val query = $$"""
            *[_type == "tiltaksgjennomforing" && _id == $id]
            $$sanityTiltaksgjennomforingQuery
            [0]
        """.trimIndent()

        val params = listOf(SanityParam.of("id", id))

        return when (val result = sanityClient.query(query, params, perspective)) {
            is SanityResponse.Result -> {
                if (result.result == null) {
                    throw NotFoundException("Fant ikke tiltak med id=$id")
                } else {
                    result.decode<SanityTiltaksgjennomforing>()
                        .also {
                            sanityTiltaksgjennomforingCache.put(id, it)
                        }
                }
            }

            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun getAllTiltak(search: String?, cacheUsage: CacheUsage): List<SanityTiltaksgjennomforing> {
        sanityTiltaksgjennomforingerCache.getIfPresent(search ?: "")?.let {
            if (cacheUsage == CacheUsage.UseCache) {
                return@getAllTiltak it
            }
        }

        val query = """
            *[_type == "tiltaksgjennomforing"
              ${if (search != null) $$"&& [tiltaksgjennomforingNavn, string(tiltaksnummer.current), tiltakstype->tiltakstypeNavn] match $search" else ""}
            ] {
              _id,
              tiltakstype->{
                _id,
                tiltakstypeNavn,
                innsatsgrupper,
              },
              tiltaksgjennomforingNavn,
              "tiltaksnummer": tiltaksnummer.current,
              stedForGjennomforing,
              "fylke": fylke->nummer.current,
              "enheter": coalesce(enheter[]->nummer.current, []),
              arrangor -> {
                _id,
                navn,
                organisasjonsnummer,
                kontaktpersoner[] -> {
                  _id,
                  navn,
                  telefon,
                  epost,
                  beskrivelse,
                }
              },
            }
        """.trimIndent()

        val params = buildList {
            if (search != null) {
                add(SanityParam.of("search", "*$search*"))
            }
        }

        return when (val result = sanityClient.query(query, params)) {
            is SanityResponse.Result -> result.decode<List<SanityTiltaksgjennomforing>>()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
            .also {
                sanityTiltaksgjennomforingerCache.put(search ?: "", it)
            }
    }

    suspend fun getOppskrifter(
        tiltakstypeId: UUID,
        perspective: SanityPerspective,
    ): List<Oppskrift> {
        val query = $$"""
              *[_type == "tiltakstype" && defined(oppskrifter) && _id == $id] {
               oppskrifter[] -> {
                  ...,
                  steg[] {
                    ...,
                    innhold[] {
                      ...,
                      _type == "image" => {
                      ...,
                      asset-> // For å hente ut url til bilder
                      }
                    }
                  }
               }
             }.oppskrifter[]
        """.trimIndent()

        val params = listOf(SanityParam.of("id", tiltakstypeId))

        return when (val result = sanityClient.query(query, params, perspective)) {
            is SanityResponse.Result -> result.decode()
            is SanityResponse.Error -> throw Exception(result.error.toString())
        }
    }

    suspend fun patchSanityTiltakstype(
        sanityId: UUID,
        navn: String,
    ) {
        val data = SanityTiltakstypeFields(
            tiltakstypeNavn = navn,
        )

        val response = sanityClient.mutate(
            listOf(Mutation.patch(id = sanityId.toString(), set = data)),
        )

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke patche tiltakstype med id=$sanityId: ${response.status}")
        } else {
            log.info("Patchet tiltakstype med id=$sanityId")
        }
    }

    suspend fun deleteSanityTiltaksgjennomforing(sanityId: UUID) {
        val response = sanityClient.mutate(
            listOf(
                // Deletes both drafts and published dokuments
                Mutation.delete(id = "drafts.$sanityId"),
                Mutation.delete(id = "$sanityId"),
            ),
        )

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke slette gjennomforing i sanity: ${response.status}")
        } else {
            log.info("Slettet gjennomforing i Sanity med id: $sanityId")
        }
    }

    suspend fun createSanityEnheter(
        sanityEnheter: List<SanityEnhet>,
    ): SanityClient.MutateResponse {
        val mutations = sanityEnheter.map { Mutation.createOrReplace(it) }
        return sanityClient.mutate(mutations)
    }

    suspend fun getTiltakByNavIdent(navIdent: NavIdent): List<SanityTiltaksgjennomforing> {
        val query =
            $$"""
            *[_type == "tiltaksgjennomforing" && ($navIdent in kontaktpersoner[].navKontaktperson->navIdent.current || $navIdent in redaktor[]->navIdent.current)]
            $$sanityTiltaksgjennomforingQuery
            """.trimIndent()

        val params = listOf(SanityParam.of("navIdent", navIdent.value))

        return when (val response = sanityClient.query(query, params)) {
            is SanityResponse.Result -> response.decode()
            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut tiltak fra Sanity: ${response.error}")
        }
    }

    suspend fun getNavKontaktperson(navIdent: NavIdent): SanityNavKontaktperson? {
        val query = $$""" *[_type == "navKontaktperson" && navIdent.current == $navIdent] """.trimIndent()
        val params = listOf(SanityParam.of("navIdent", navIdent.value))

        return when (val response = sanityClient.query(query, params)) {
            is SanityResponse.Result -> response.decode<List<SanityNavKontaktperson>>().firstOrNull()
            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut kontaktperson fra Sanity: ${response.error}")
        }
    }

    suspend fun getNavKontaktpersoner(): List<SanityNavKontaktperson> {
        val query = """ *[_type == "navKontaktperson"] """.trimIndent()

        return when (val response = sanityClient.query(query)) {
            is SanityResponse.Result -> response.decode<List<SanityNavKontaktperson>?>()
            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut kontaktpersoner fra Sanity: ${response.error}")
        } ?: emptyList()
    }

    suspend fun getRedaktorer(): List<SanityRedaktor> {
        val query = """ *[_type == "redaktor"] """.trimIndent()

        return when (val response = sanityClient.query(query)) {
            is SanityResponse.Result -> response.decode<List<SanityRedaktor>?>()
            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut redaktorer fra Sanity: ${response.error}")
        } ?: emptyList()
    }

    suspend fun getRedaktor(navIdent: NavIdent): SanityRedaktor? {
        val query = $$""" *[_type == "redaktor" && navIdent.current == $navIdent] """.trimIndent()
        val params = listOf(SanityParam.of("navIdent", navIdent.value))

        return when (val response = sanityClient.query(query, params)) {
            is SanityResponse.Result -> response.decode<List<SanityRedaktor>>().firstOrNull()
            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut kontaktperson fra Sanity: ${response.error}")
        }
    }

    suspend fun createRedaktorer(redaktorer: List<SanityRedaktor>) {
        val response = sanityClient.mutate(redaktorer.map { Mutation.createOrReplace(it) })

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke upserte redaktorer: Error: ${response.body} - Status: ${response.status}")
        }
    }

    suspend fun createNavKontaktpersoner(kontaktperson: List<SanityNavKontaktperson>) {
        val response = sanityClient.mutate(kontaktperson.map { Mutation.createOrReplace(it) })

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke upserte nav kontaktpersoner: Error: ${response.body} - Status: ${response.status}")
        }
    }

    suspend fun removeNavIdentFromTiltaksgjennomforinger(navIdent: NavIdent) {
        val kontaktpersonToDelete = getNavKontaktperson(navIdent)
        val redaktorToDelete = getRedaktor(navIdent)

        val mutations = getTiltakByNavIdent(navIdent)
            .map { tiltak ->
                Mutation.unsetPatch(
                    id = tiltak._id,
                    unset = listOfNotNull(
                        if (kontaktpersonToDelete != null) {
                            "kontaktpersoner[navKontaktperson._ref == \"${kontaktpersonToDelete._id}\"]"
                        } else {
                            null
                        },
                        if (redaktorToDelete != null) {
                            "redaktor[_ref == \"${redaktorToDelete._id}\"]"
                        } else {
                            null
                        },
                    ),
                )
            }

        val response = sanityClient.mutate(mutations)
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke upserte nav kontaktpersoner: Error: ${response.body} - Status: ${response.status}")
        }
    }

    suspend fun deleteNavIdent(navIdent: NavIdent) {
        val queryResponse = sanityClient.query(
            $$"""
            *[_type == "navKontaktperson" && navIdent.current == $navIdent || _type == "redaktor" && navIdent.current == $navIdent]._id
            """.trimIndent(),
            params = listOf(SanityParam.of("navIdent", navIdent.value)),
        )

        val ids = when (queryResponse) {
            is SanityResponse.Result -> queryResponse.decode<List<String>>()
            is SanityResponse.Error -> throw Exception("Klarte ikke hente ut id'er til sletting fra Sanity: ${queryResponse.error}")
        }

        if (ids.isEmpty()) {
            return
        }

        val result = sanityClient.mutate(mutations = ids.map { Mutation.delete(it) })
        if (result.status != HttpStatusCode.OK) {
            throw Exception("Klarte ikke slette navIdent from Sanity: ${result.body}")
        }
    }
}

enum class CacheUsage { UseCache, NoCache }
