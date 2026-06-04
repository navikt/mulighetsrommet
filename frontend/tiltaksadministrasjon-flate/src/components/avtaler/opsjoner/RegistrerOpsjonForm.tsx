import { Alert, Radio } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { FormDateInput } from "@/components/skjema/FormDateInput";
import { FormRadioGroup } from "@/components/skjema/FormRadioGroup";
import { addDuration, formaterDato } from "@mr/frontend-common/utils/date";
import {
  AvtaleDto,
  OpprettOpsjonLoggRequest,
  OpprettOpsjonLoggRequestType,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  avtale: AvtaleDto;
}

export function RegistrerOpsjonForm({ avtale }: Props) {
  const maksVarighetForOpsjon = avtale.opsjonsmodell.opsjonMaksVarighet;
  const sluttDatoSisteOpsjon = avtale.opsjonerRegistrert.at(-1)?.sluttDato;
  const sluttdato = avtale.sluttDato;
  const { watch } = useFormContext<OpprettOpsjonLoggRequest>();

  if (!maksVarighetForOpsjon || !sluttdato) {
    return (
      <Alert variant="error">
        Kunne ikke hente maks varighet for opsjon og kan derfor ikke utløse opsjon.
      </Alert>
    );
  }

  return (
    <div className="bg-ax-bg-neutral-soft p-4 rounded-lg">
      <FormRadioGroup<OpprettOpsjonLoggRequest> legend="Registrer opsjon" hideLegend name="type">
        <Radio value={OpprettOpsjonLoggRequestType.SKAL_IKKE_UTLOSE_OPSJON}>
          Avklart at opsjon ikke skal utløses
        </Radio>
        <Radio value={OpprettOpsjonLoggRequestType.ETT_AAR}>
          + 1 år (Forleng til: {formaterDato(addDuration(new Date(sluttdato), { years: 1 }))})
        </Radio>
        <Radio value={OpprettOpsjonLoggRequestType.CUSTOM_LENGDE}>
          Annen lengde (maks dato: {formaterDato(maksVarighetForOpsjon)})
        </Radio>
      </FormRadioGroup>
      {watch("type") === OpprettOpsjonLoggRequestType.CUSTOM_LENGDE && (
        <FormDateInput<OpprettOpsjonLoggRequest>
          name="nySluttDato"
          label="Velg ny sluttdato"
          fromDate={
            sluttDatoSisteOpsjon
              ? addDuration(new Date(sluttDatoSisteOpsjon), { days: 1 })
              : new Date()
          }
          toDate={new Date(maksVarighetForOpsjon)}
        />
      )}
    </div>
  );
}
