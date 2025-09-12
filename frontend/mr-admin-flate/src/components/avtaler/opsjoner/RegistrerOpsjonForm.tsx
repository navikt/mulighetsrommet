import { Alert, Radio } from "@navikt/ds-react";
import { AvtaleDto } from "@mr/api-client-v2";
import { useFormContext } from "react-hook-form";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { ControlledRadioGroup } from "../../skjema/ControlledRadioGroup";
import { addDuration, formaterDato } from "@mr/frontend-common/utils/date";
import {
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
  const {
    watch,
    register,
    setValue,
    formState: { errors },
  } = useFormContext<OpprettOpsjonLoggRequest>();

  if (!maksVarighetForOpsjon || !sluttdato) {
    return (
      <Alert variant="error">
        Kunne ikke hente maks varighet for opsjon og kan derfor ikke utløse opsjon.
      </Alert>
    );
  }

  return (
    <div className="bg-surface-subtle p-4 rounded-lg">
      <ControlledRadioGroup legend="Registrer opsjon" hideLegend {...register("type")}>
        <Radio value={OpprettOpsjonLoggRequestType.SKAL_IKKE_UTLOSE_OPSJON}>
          Avklart at opsjon ikke skal utløses
        </Radio>
        <Radio value={OpprettOpsjonLoggRequestType.ETT_AAR}>
          + 1 år (Forleng til: {formaterDato(addDuration(new Date(sluttdato), { years: 1 }))})
        </Radio>
        <Radio value={OpprettOpsjonLoggRequestType.CUSTOM_LENGDE}>
          Annen lengde (maks dato: {formaterDato(maksVarighetForOpsjon)})
        </Radio>
      </ControlledRadioGroup>
      {watch("type") === OpprettOpsjonLoggRequestType.CUSTOM_LENGDE && (
        <ControlledDateInput
          size="small"
          label={"Velg ny sluttdato"}
          fromDate={
            sluttDatoSisteOpsjon
              ? addDuration(new Date(sluttDatoSisteOpsjon), { days: 1 })
              : new Date()
          }
          toDate={new Date(maksVarighetForOpsjon)}
          {...register("nySluttDato")}
          error={errors.nySluttDato?.message}
          onChange={(val) => setValue("nySluttDato", val)}
        />
      )}
    </div>
  );
}
