import no.nav.mulighetsrommet.model.Personopplysning

data class PersonvernDbo(
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
)
