package no.nav.mulighetsrommet.api.avtaler

import arrow.core.Either
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.right
import no.nav.mulighetsrommet.api.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.api.domain.dto.OpsjonLoggEntry
import no.nav.mulighetsrommet.api.routes.v1.OpsjonLoggRequest
import no.nav.mulighetsrommet.api.routes.v1.OpsjonsmodellData
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError

class OpsjonLoggValidator {
    fun validate(entry: OpsjonLoggEntry, avtale: AvtaleAdminDto): Either<List<ValidationError>, OpsjonLoggEntry> = either {
        val opsjonsmodellData = avtale.opsjonsmodellData
            ?: raise(ValidationError.of(OpsjonsmodellData::opsjonsmodell, "Kan ikke registrer opsjon uten en opsjonsmodell").nel())

        val errors = buildList {
            if (entry.status == OpsjonLoggRequest.OpsjonsLoggStatus.OPSJON_UTLØST) {
                val skalIkkeUtloseOpsjonerForAvtale = avtale.opsjonerRegistrert?.any { it.status === OpsjonLoggRequest.OpsjonsLoggStatus.SKAL_IKKE_UTLØSE_OPSJON }
                if (skalIkkeUtloseOpsjonerForAvtale == true) {
                    add(ValidationError.of(OpsjonLoggEntry::status, "Kan ikke utløse opsjon for avtale som har en opsjon som ikke skal utløses"))
                    return@buildList
                }

                val maksVarighet = opsjonsmodellData.opsjonMaksVarighet
                if (entry.sluttdato != null && entry.sluttdato.isAfter(maksVarighet)) {
                    add(ValidationError.of(OpsjonLoggEntry::sluttdato, "Ny sluttdato er forbi maks varighet av avtalen"))
                }
            }
        }
        return errors.takeIf { it.isNotEmpty() }?.left() ?: entry.right()
    }
}
