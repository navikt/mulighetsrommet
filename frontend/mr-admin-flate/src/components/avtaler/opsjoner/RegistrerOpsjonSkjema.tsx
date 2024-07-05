import { Alert, Radio } from "@navikt/ds-react";
import { Avtale } from "mulighetsrommet-api-client";
import { useEffect } from "react";
import { useFormContext } from "react-hook-form";
import { addYear, formaterDato, formaterDatoSomYYYYMMDD } from "../../../utils/Utils";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { ControlledRadioGroup } from "../../skjema/ControlledRadioGroup";
import { InferredRegistrerOpsjonSchema } from "./RegistrerOpsjonSchema";
import styles from "./RegistrerOpsjonSkjema.module.scss";

interface Props {
  avtale: Avtale;
}

export function RegistrerOpsjonSkjema({ avtale }: Props) {
  const maksVarighetForOpsjon = avtale?.opsjonsmodellData?.opsjonMaksVarighet;
  const sluttdato = avtale?.sluttDato;
  const { watch, setValue, register } = useFormContext<InferredRegistrerOpsjonSchema>();

  const watchedOpsjonsvalg = watch("opsjonsvalg");

  useEffect(() => {
    function settNySluttdato() {
      if (watchedOpsjonsvalg && watchedOpsjonsvalg !== "Annet" && sluttdato) {
        setValue("opsjonsdatoValgt", formaterDatoSomYYYYMMDD(addYear(new Date(sluttdato), 1)));
      }
    }
    settNySluttdato();
  }, [watchedOpsjonsvalg]);

  if (!maksVarighetForOpsjon || !sluttdato) {
    return (
      <Alert variant="error">
        Kunne ikke hente maks varighet for opsjon og kan derfor ikke utløse opsjon.
      </Alert>
    );
  }

  return (
    <div className={styles.container}>
      <ControlledRadioGroup legend="Registrer opsjon" hideLegend {...register("opsjonsvalg")}>
        <Radio value="1">
          + 1 år (Forleng til: {formaterDato(addYear(new Date(sluttdato), 1))})
        </Radio>
        <Radio value="Annet">Annen lengde (maks dato: {formaterDato(maksVarighetForOpsjon)})</Radio>
      </ControlledRadioGroup>
      {watch("opsjonsvalg") === "Annet" && (
        <ControlledDateInput
          size="small"
          label={"Velg ny sluttdato"}
          fromDate={new Date()}
          toDate={new Date(maksVarighetForOpsjon)}
          {...register("opsjonsdatoValgt")}
          format={"iso-string"}
        />
      )}
    </div>
  );
}
