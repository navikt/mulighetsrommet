import { Heading, HGrid, Select } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../../redaksjonelt-innhold/AvtaleSchema";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";

interface Props {
  arenaOpphavOgIngenEierskap: boolean;
  startDato?: string;
  sluttDatoFraDato: Date;
  sluttDatoTilDato: Date;
  minStartDato: Date;
  maksSluttDato: number;
}

type AvtaleModellKey = "2+1" | "2+1+1" | "2+1+1+1" | "Annet";
interface Avtalemodell {
  value: AvtaleModellKey;
  label: string;
  maksVarighetAar: number;
  additionalAarSluttdato?: number;
}

export function AvtaleVarighet({
  arenaOpphavOgIngenEierskap,
  startDato,
  sluttDatoFraDato,
  sluttDatoTilDato,
  minStartDato,
  maksSluttDato,
}: Props) {
  const { register, watch, setValue } = useFormContext<InferredAvtaleSchema>();
  const tiltakstype = watch("tiltakstype");
  const [avtalemodell, setAvtalemodell] = useState<Avtalemodell | undefined>();

  const avtalemodeller: Avtalemodell[] = [
    {
      value: "2+1",
      label: "2 år + 1 år",
      maksVarighetAar: 3,
      additionalAarSluttdato: 2,
    },
    {
      value: "2+1+1",
      label: "2 år + 1 år + 1 år",
      maksVarighetAar: 4,
      additionalAarSluttdato: 2,
    },
    {
      value: "2+1+1+1",
      label: "2 år + 1 år + 1 år + 1 år",
      maksVarighetAar: 5,
      additionalAarSluttdato: 2,
    },
    {
      value: "Annet",
      label: "Annet",
      maksVarighetAar: 5,
      additionalAarSluttdato: undefined,
    },
  ];

  useEffect(() => {
    setValue("startOgSluttDato.startDato", "");
    setValue("startOgSluttDato.sluttDato", undefined);
    setValue("maksVarighet", undefined);
  }, [avtalemodell]);

  useEffect(() => {
    if (avtalemodell?.additionalAarSluttdato && avtalemodell?.maksVarighetAar && startDato) {
      setValue(
        "startOgSluttDato.sluttDato",
        regnUtMaksDato(new Date(startDato), avtalemodell.additionalAarSluttdato).toISOString(),
      );
      setValue("maksVarighet", regnUtMaksDato(new Date(startDato), avtalemodell.maksVarighetAar));
    }
  }, [avtalemodell, startDato, tiltakstype]);

  return (
    <>
      <Heading size="small" as="h3">
        Avtalens varighet
      </Heading>

      <pre>{JSON.stringify(watch("startOgSluttDato"), null, 2)}</pre>

      <HGrid columns={2}>
        <Select
          label="Avtalemodell"
          size="small"
          onChange={(e) => {
            const value = e.currentTarget.value;
            const modell = avtalemodeller.find((modell) => modell.value === value);
            setAvtalemodell(modell);
          }}
        >
          <option value={undefined}>Velg avtalemodell</option>
          {avtalemodeller.map((modell) => (
            <option value={modell.value}>{modell.label}</option>
          ))}
        </Select>
      </HGrid>

      {avtalemodell ? (
        <HGrid columns={3}>
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            readOnly={arenaOpphavOgIngenEierskap}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.startDato")}
            format={"iso-string"}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.sluttdatoLabel}
            readOnly={avtalemodell?.value !== "Annet"}
            fromDate={sluttDatoFraDato}
            toDate={
              startDato && avtalemodell.additionalAarSluttdato
                ? regnUtMaksDato(new Date(startDato), avtalemodell.additionalAarSluttdato)
                : sluttDatoTilDato
            }
            {...register("startOgSluttDato.sluttDato")}
            format={"iso-string"}
            invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${maksSluttDato} år`}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.maksVarighetLabel}
            readOnly={avtalemodell?.value !== "Annet"}
            fromDate={sluttDatoFraDato}
            toDate={
              startDato && avtalemodell.maksVarighetAar
                ? regnUtMaksDato(new Date(startDato), avtalemodell.maksVarighetAar)
                : sluttDatoTilDato
            }
            {...register("maksVarighet")}
            format={"iso-string"}
          />
        </HGrid>
      ) : null}
    </>
  );
}

function regnUtMaksDato(date: Date, leggTilAar: number): Date {
  const sluttdato = new Date(date.getTime());
  sluttdato.setFullYear(sluttdato.getFullYear() + leggTilAar);
  const daysInMilliseconds = 1 * 24 * 60 * 60 * 1000;
  return new Date(sluttdato.getTime() - daysInMilliseconds);
}
