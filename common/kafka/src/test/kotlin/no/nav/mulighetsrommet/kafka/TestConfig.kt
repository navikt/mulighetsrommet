package no.nav.mulighetsrommet.kafka

import no.nav.mulighetsrommet.database.kotest.extensions.createDatabaseTestSchema

// TODO: Virker kanskje litt snålt å koble denne modulen mot arena-adapter sin db?
//       Burde muligens generalisere tilgang til en testdatabase i stedet..
fun createDatabaseTestConfig() = createDatabaseTestSchema("mulighetsrommet-arena-adapter-db", 5443)
