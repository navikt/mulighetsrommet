package no.nav.mulighetsrommet.sanity

import no.nav.mulighetsrommet.database.DatabaseAdapter

data class Config(
    val db: DatabaseAdapter.Config,
    val sanity: SanityClient.Config,
)
