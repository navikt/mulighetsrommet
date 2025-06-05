package no.nav.mulighetsrommet.api.avtale

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.avtale.model.OpsjonLoggStatus
import no.nav.mulighetsrommet.api.avtale.model.OpsjonsmodellData
import no.nav.mulighetsrommet.api.responses.FieldError

object OpsjonLoggValidator {
    fun validate(entry: OpsjonLoggEntry, avtale: AvtaleDto): Either<List<FieldError>, OpsjonLoggEntry> = either {
        val opsjonsmodellData = avtale.opsjonsmodellData
            ?: raise(
                FieldError.of(
                    OpsjonsmodellData::opsjonsmodell,
                    "Kan ikke registrer opsjon uten en opsjonsmodell",
                ).nel(),
            )

        val errors = buildList {
            if (entry.status == OpsjonLoggStatus.OPSJON_UTLOST) {
                val skalIkkeUtloseOpsjonerForAvtale =
                    avtale.opsjonerRegistrert?.any { it.status === OpsjonLoggStatus.SKAL_IKKE_UTLOSE_OPSJON }
                if (skalIkkeUtloseOpsjonerForAvtale == true) {
                    add(
                        FieldError.of(
                            OpsjonLoggEntry::status,
                            "Kan ikke utløse opsjon for avtale som har en opsjon som ikke skal utløses",
                        ),
                    )
                    return@buildList
                }

                val maksVarighet = opsjonsmodellData.opsjonMaksVarighet
                if (entry.sluttdato != null && entry.sluttdato.isAfter(maksVarighet)) {
                    add(
                        FieldError.of(
                            OpsjonLoggEntry::sluttdato,
                            "Ny sluttdato er forbi maks varighet av avtalen",
                        ),
                    )
                }

                if (entry.forrigeSluttdato == null) {
                    add(FieldError.of(OpsjonLoggEntry::forrigeSluttdato, "Forrige sluttdato må være satt"))
                }
            }
        }
        return errors.takeIf { it.isNotEmpty() }?.left() ?: entry.right()
    }
}
