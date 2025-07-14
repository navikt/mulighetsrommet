import { Alert, Radio } from "@navikt/ds-react";
import { AvtaleDto } from "@mr/api-client-v2";
import { useFormContext } from "react-hook-form";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { ControlledRadioGroup } from "../../skjema/ControlledRadioGroup";
import { InferredRegistrerOpsjonSchema } from "./RegistrerOpsjonSchema";
import { formaterDato, parseDate } from "@mr/frontend-common/utils/date";
import { addDays, addYears, isAfter } from "date-fns";

interface Props {
  avtale: AvtaleDto;
}

export function RegistrerOpsjonForm({ avtale }: Props) {
  const maksVarighetForOpsjon = parseDate(avtale.opsjonsmodell.opsjonMaksVarighet);
  const sluttDatoSisteOpsjon = parseDate(avtale.opsjonerRegistrert.at(-1)?.sluttDato);
  const sluttdato = parseDate(avtale.sluttDato);
  const { watch, register, control } = useFormContext<InferredRegistrerOpsjonSchema>();

  if (!maksVarighetForOpsjon || !sluttdato) {
    return (
      <Alert variant="error">
        Kunne ikke hente maks varighet for opsjon og kan derfor ikke utløse opsjon.
      </Alert>
    );
  }

  return (
    <div className="bg-surface-selected p-4 rounded-lg">
      <ControlledRadioGroup legend="Registrer opsjon" hideLegend {...register("opsjonsvalg")}>
        <Radio value="Opsjon_skal_ikke_utloses">Avklart at opsjon ikke skal utløses</Radio>
        <Radio value="1" disabled={isAfter(addYears(sluttdato, 1), maksVarighetForOpsjon)}>
          + 1 år (Forleng til: {formaterDato(addYears(sluttdato, 1))})
        </Radio>
        <Radio value="Annet">Annen lengde (maks dato: {formaterDato(maksVarighetForOpsjon)})</Radio>
      </ControlledRadioGroup>
      {watch("opsjonsvalg") === "Annet" && (
        <ControlledDateInput
          size="small"
          label={"Velg ny sluttdato"}
          fromDate={sluttDatoSisteOpsjon ? addDays(sluttDatoSisteOpsjon, 1) : new Date()}
          toDate={maksVarighetForOpsjon}
          {...register("opsjonsdatoValgt")}
          format={"iso-string"}
          control={control}
        />
      )}
    </div>
  );
}
