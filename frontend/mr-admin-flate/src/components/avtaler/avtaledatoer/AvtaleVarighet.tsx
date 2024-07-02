import { HGrid, Select, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../../redaksjonelt-innhold/AvtaleSchema";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";

type AvtaleOpsjonsnokkel = "2+1" | "2+1+1" | "2+1+1+1" | "Annet";

export interface Opsjonsmodell {
  value: AvtaleOpsjonsnokkel;
  label: string;
  maksVarighetAar: number;
  initialSluttdatoEkstraAar?: number;
}

interface Props {
  arenaOpphavOgIngenEierskap: boolean;
  minStartDato: Date;
  sluttDatoFraDato: Date;
  sluttDatoTilDato: Date;
  maksAar: number;
}

export function AvtaleVarighet({
  arenaOpphavOgIngenEierskap,
  minStartDato,
  sluttDatoFraDato,
  sluttDatoTilDato,
  maksAar,
}: Props) {
  const { register, setValue, watch } = useFormContext<InferredAvtaleSchema>();
  const [opsjonsmodell, setOpsjonsmodell] = useState<Opsjonsmodell | undefined>(undefined);
  const { startDato } = watch("startOgSluttDato") ?? {};

  const opsjonsmodeller: Opsjonsmodell[] = [
    { value: "2+1", label: "2 år + 1 år", maksVarighetAar: 3, initialSluttdatoEkstraAar: 2 },
    {
      value: "2+1+1",
      label: "2 år + 1 år + 1 år",
      maksVarighetAar: 4,
      initialSluttdatoEkstraAar: 2,
    },
    {
      value: "2+1+1+1",
      label: "2 år + 1 år + 1 år + 1 år",
      maksVarighetAar: 5,
      initialSluttdatoEkstraAar: 2,
    },
    { value: "Annet", label: "Annet", maksVarighetAar: 5, initialSluttdatoEkstraAar: undefined },
  ];

  const readonly = opsjonsmodell?.value !== "Annet" || arenaOpphavOgIngenEierskap;

  useEffect(() => {
    if (startDato && opsjonsmodell && opsjonsmodell.initialSluttdatoEkstraAar) {
      setValue(
        "startOgSluttDato.sluttDato",
        kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.initialSluttdatoEkstraAar).toISOString(),
      );
      setValue(
        "maksVarighet",
        kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.maksVarighetAar).toISOString(),
      );
    }
  }, [opsjonsmodell, startDato]);

  useEffect(() => {
    // Reset verdier når opsjonsmodell endres
    setValue("startOgSluttDato.startDato", "");
    setValue("startOgSluttDato.sluttDato", "");
    setValue("maksVarighet", "");
    setValue("custom_opsjonsmodellnavn", "");
  }, [opsjonsmodell]);

  const maksVarighetAar = opsjonsmodell?.maksVarighetAar ?? 5;
  const maksVarighetDato = kalkulerMaksDato(new Date(startDato), maksVarighetAar);

  return (
    <>
      <HGrid columns={2}>
        <Select
          label="Opsjonsmodell"
          size="small"
          onChange={(e) => {
            const opsjonsmodel = opsjonsmodeller.find((modell) => modell.value === e.target.value);
            setOpsjonsmodell(opsjonsmodel);
            setValue("opsjonsmodell", opsjonsmodel?.value);
          }}
        >
          <option value={undefined}>Velg opsjonsmodell</option>
          {opsjonsmodeller.map((modell) => (
            <option key={modell.value} value={modell.value}>
              {modell.label}
            </option>
          ))}
        </Select>
      </HGrid>

      {opsjonsmodell?.value === "Annet" ? (
        <TextField
          label="Opsjonsnavn"
          hideLabel
          placeholder="Skriv inn eget navn på opsjonsmodellen"
          size="small"
          {...register("custom_opsjonsmodellnavn")}
        />
      ) : null}

      {opsjonsmodell ? (
        <HGrid columns={3}>
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.startDato")}
            format={"iso-string"}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.sluttdatoLabel}
            readOnly={readonly}
            fromDate={sluttDatoFraDato}
            toDate={maksVarighetDato}
            {...register("startOgSluttDato.sluttDato")}
            format={"iso-string"}
            invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${maksAar} år`}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.maksVarighetLabel}
            readOnly={readonly}
            fromDate={sluttDatoFraDato}
            toDate={maksVarighetDato}
            {...register("maksVarighet")}
            format={"iso-string"}
          />
        </HGrid>
      ) : null}
    </>
  );
}

function kalkulerMaksDato(date: Date, addYears: number): Date {
  const resultDate = new Date(date.getTime());
  resultDate.setFullYear(resultDate.getFullYear() + addYears);
  const daysInMilliseconds = 1 * 24 * 60 * 60 * 1000;
  return new Date(resultDate.getTime() - daysInMilliseconds);
}
