package no.nav.tiltak.okonomi.avstemming

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.okonomi.db.OkonomiDatabase
import no.nav.tiltak.okonomi.model.Bestilling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Skriver filer til server med navnet Adra Match. Oebs leser disse daglig.
 *
 *
 * Hva er Adra Match?
 * Adra Match er et system for identifisering av og registrering av innbetalinger
 * og andre banktransaksjoner, kryssing mot reskontroposter, og for å foreta
 * bankavstemminger (matching av innbetalinger og avstemming av bank på inn- eller
 * utbetalt fra etatens bankkonti.)

 * Løsningen består av tre applikasjoner som er publisert på NAV Skrivebord for
 * NAV-ansatte og på Skatteetaten sin egen partner-løsning.

 * Baksystemene består av en applikasjonsserver med skedulerte jobber, èn SQL-database
 * per klient og en rekke TWS jobber som overfører data via FTP og filområder.
 * https://confluence.adeo.no/display/DA/Adra+Match
 *
 * Filnavn:
 * ååååmmdd_A_tiltak_avstemming_dag. A (valp) for å skille disse fra Arena-filene.
 *
 * Første kolonne sier om det er tilsagn eller refusjonskrav,
 * andre er tilsagnsnummer eller refusjonskravnummer,
 * tredje er godkjenningsdato for transaksjonen,
 * fjerde skiller egentlig bare på om tilsagnet er godkjent eller annullert, men
 * refusjonskravet har også fått «sin verdi» for at det skal ligge informasjon der.
 * Så er det beløp, valutakode, bedriftsnummer og så orgnummer.
 * Den siste viser bankkonto, men den bruker vi ikke.
 */
class AvstemmingService(
    private val db: OkonomiDatabase,
    private val sftpClient: SftpClient,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun avstem(alternativFtpPort: Int? = null) {
        log.info("Avstemming starter...")
        val fakturaer = db.session { queries.faktura.getNotAvstemt() }
        val bestillinger = db.session { queries.bestilling.getNotAvstemt() }

        log.info("Fant {} bestillinger som kan avstemmes", bestillinger.size)
        log.info("Fant {} fakturaer som kan avstemmes", fakturaer.size)

        val rader = bestillinger.map { it.toCSVRad() } + fakturaer.map { it.toCSVRad() }
        val content = rader.joinToString(separator = "\n").toByteArray()
        val now = LocalDateTime.now()

        try {
            if (rader.isNotEmpty()) {
                db.transaction {
                    queries.faktura.setAvstemtTidspunkt(now, fakturaer.map { it.fakturanummer })
                    queries.bestilling.setAvstemtTidspunkt(now, bestillinger.map { it.bestillingsnummer })

                    val tidspunkt = LocalDateTime.now()
                    sftpClient.put(content, filename(tidspunkt), alternativFtpPort)

                    log.info("Ferdig avstemt {} tilsagn og refusjoner", rader.size)
                }
            }
        } catch (ex: Exception) {
            log.error("Feilet til AdraMatch FTP Server: ", ex)
        }
    }

    companion object {
        // TODO: Bli enig med oebs om et filnavn og mappe det skal laster opp på
        fun filename(tidspunkt: LocalDateTime) = "${tidspunkt.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}_A_tiltak_avstemming_dag.csv"
    }
}

fun Bestilling.toCSVRad(): String {
    require(this.belop > 0) { "Beløp i avstemming var ikke større enn 0" }

    val data = mutableListOf<String>()
    data.add("Bestilling")
    data.add(bestillingsnummer)
    data.add(opprettelse.besluttetTidspunkt.toLocalDate().toString())
    data.add(if (this.annullering != null) "Annullert" else "Godkjent")
    data.add(this.belop.toString())
    data.add("NOK")
    data.add(this.arrangorUnderenhet.value)
    data.add(this.arrangorHovedenhet.value)
    return data.joinToString(";")
}

fun FakturaCsvData.toCSVRad(): String {
    require(belop > 0) { "Beløp i avstemming var ikke større enn 0" }

    val data = mutableListOf<String>()
    data.add("Faktura")
    data.add(fakturanummer)
    data.add(besluttetTidspunkt.toLocalDate().toString())
    data.add("Godkjent")
    data.add(belop.toString())
    data.add("NOK")
    data.add(arrangorUnderenhet.value)
    data.add(arrangorHovedenhet.value)
    return data.joinToString(";")
}

data class FakturaCsvData(
    val fakturanummer: String,
    val belop: Int,
    val besluttetTidspunkt: LocalDateTime,
    val arrangorHovedenhet: Organisasjonsnummer,
    val arrangorUnderenhet: Organisasjonsnummer,
)
