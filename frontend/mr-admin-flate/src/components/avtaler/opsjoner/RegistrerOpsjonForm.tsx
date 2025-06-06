import { Alert, Radio } from "@navikt/ds-react";
import { AvtaleDto } from "@mr/api-client-v2";
import { useEffect } from "react";
import { useFormContext } from "react-hook-form";
import { addDays, addYear, formaterDato, formaterDatoSomYYYYMMDD } from "@/utils/Utils";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { ControlledRadioGroup } from "../../skjema/ControlledRadioGroup";
import { InferredRegistrerOpsjonSchema } from "./RegistrerOpsjonSchema";

interface Props {
  avtale: AvtaleDto;
}

export function RegistrerOpsjonForm({ avtale }: Props) {
  const maksVarighetForOpsjon = avtale.opsjonsmodell.opsjonMaksVarighet;
  const sluttDatoSisteOpsjon = avtale.opsjonerRegistrert.at(-1)?.sluttDato;
  const sluttdato = avtale.sluttDato;
  const { watch, setValue, register, control } = useFormContext<InferredRegistrerOpsjonSchema>();

  const watchedOpsjonsvalg = watch("opsjonsvalg");

  useEffect(() => {
    function settNySluttdato() {
      if (watchedOpsjonsvalg === "Opsjon_skal_ikke_utloses") return;

      if (watchedOpsjonsvalg && watchedOpsjonsvalg !== "Annet" && sluttdato) {
        setValue("opsjonsdatoValgt", formaterDatoSomYYYYMMDD(addYear(new Date(sluttdato), 1)));
      }
    }

    settNySluttdato();
  }, [setValue, sluttdato, watchedOpsjonsvalg]);

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
        <Radio
          value="1"
          disabled={addYear(new Date(sluttdato), 1) > new Date(maksVarighetForOpsjon)}
        >
          + 1 år (Forleng til: {formaterDato(addYear(new Date(sluttdato), 1))})
        </Radio>
        <Radio value="Annet">Annen lengde (maks dato: {formaterDato(maksVarighetForOpsjon)})</Radio>
      </ControlledRadioGroup>
      {watch("opsjonsvalg") === "Annet" && (
        <ControlledDateInput
          size="small"
          label={"Velg ny sluttdato"}
          fromDate={sluttDatoSisteOpsjon ? addDays(new Date(sluttDatoSisteOpsjon), 1) : new Date()}
          toDate={new Date(maksVarighetForOpsjon)}
          {...register("opsjonsdatoValgt")}
          format={"iso-string"}
          control={control}
        />
      )}
    </div>
  );
}
