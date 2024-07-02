import { HGrid, Select } from "@navikt/ds-react";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { useFormContext } from "react-hook-form";
import { InferredAvtaleSchema } from "../../redaksjonelt-innhold/AvtaleSchema";
import { useEffect, useState } from "react";

interface Avtalemodell {
  value: "2+1" | "2+1+1" | "2+1+1+1" | "Annet";
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
  const [avtalemodell, setAvtalemodell] = useState<Avtalemodell | undefined>(undefined);
  const { startDato } = watch("startOgSluttDato") ?? {};

  const avtalemodeller: Avtalemodell[] = [
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

  const readonly = avtalemodell?.value !== "Annet" || arenaOpphavOgIngenEierskap;

  useEffect(() => {
    setValue("startOgSluttDato.startDato", "");
    setValue("startOgSluttDato.sluttDato", "");
    setValue("maksVarighet", "");
  }, [avtalemodell]);

  useEffect(() => {
    if (startDato && avtalemodell && avtalemodell.initialSluttdatoEkstraAar) {
      setValue(
        "startOgSluttDato.sluttDato",
        kalkulerMaksDato(sluttDatoFraDato, avtalemodell.initialSluttdatoEkstraAar).toISOString(),
      );
      setValue(
        "maksVarighet",
        kalkulerMaksDato(sluttDatoFraDato, avtalemodell.maksVarighetAar).toISOString(),
      );
    }
  }, [avtalemodell, startDato]);

  return (
    <>
      <pre>{JSON.stringify(watch("startOgSluttDato"), null, 2)}</pre>
      <HGrid columns={2}>
        <Select
          label="Avtalemodell"
          size="small"
          onChange={(e) => {
            const avtalemodell = avtalemodeller.find((modell) => modell.value === e.target.value);
            setAvtalemodell(avtalemodell);
          }}
        >
          <option value={undefined}>Velg avtalemodell</option>
          {avtalemodeller.map((modell) => (
            <option key={modell.value} value={modell.value}>
              {modell.label}
            </option>
          ))}
        </Select>
      </HGrid>
      {avtalemodell ? (
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
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.sluttDato")}
            format={"iso-string"}
            invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${maksAar} år`}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.maksVarighetLabel}
            readOnly={readonly}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
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
