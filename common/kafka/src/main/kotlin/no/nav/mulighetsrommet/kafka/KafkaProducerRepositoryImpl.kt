package no.nav.mulighetsrommet.kafka

import kotliquery.Row
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord

private fun toStoredProducerRecord(row: Row) = StoredProducerRecord(
    row.long("id"),
    row.string("topic"),
    row.bytes("key"),
    row.bytes("value"),
    row.string("headers_json"),
)
