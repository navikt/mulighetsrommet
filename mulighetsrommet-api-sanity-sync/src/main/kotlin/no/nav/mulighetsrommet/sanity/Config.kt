package no.nav.mulighetsrommet.sanity

import no.nav.mulighetsrommet.database.DatabaseConfig

data class Config(
    val db: DatabaseConfig,
    val sanity: SanityClient.Config,
)
