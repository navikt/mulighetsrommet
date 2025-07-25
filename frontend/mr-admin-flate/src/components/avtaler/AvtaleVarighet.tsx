import { Avtaletype } from "@mr/api-client-v2";
import { HGrid, Select, TextField } from "@navikt/ds-react";
import { useEffect, useMemo } from "react";
import { FieldError, useFormContext } from "react-hook-form";
import { MAKS_AAR_FOR_AVTALER, MIN_START_DATO_FOR_AVTALER } from "@/constants";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { ControlledDateInput } from "@/components/skjema/ControlledDateInput";
import { RegistrerteOpsjoner } from "./opsjoner/RegistrerteOpsjoner";
import {
  hentGjeldendeOpsjonsmodeller,
  hentOpsjonsmodell,
} from "@/components/avtaler/opsjoner/opsjonsmodeller";
import { AvtaleFormValues } from "@/schemas/avtale";
import { addYear } from "@/utils/Utils";

interface Props {
  antallOpsjonerUtlost: number;
}

export function AvtaleVarighet({ antallOpsjonerUtlost }: Props) {
  const {
    register,
    setValue,
    watch,
    formState: { errors },
    control,
  } = useFormContext<AvtaleFormValues>();

  const startDato = watch("startDato");
  // Uten useMemo for sluttDatoFraDato så trigges rerendering av children hver gang sluttdato kalkuleres på nytt ved endring av startdato
  const sluttDatoFraDato = useMemo(
    () => (startDato ? new Date(startDato) : MIN_START_DATO_FOR_AVTALER),
    [startDato],
  );
  const sluttDatoTilDato = useMemo(
    () => addYear(startDato ? new Date(startDato) : new Date(), MAKS_AAR_FOR_AVTALER),
    [startDato],
  );

  const watchedAvtaletype = watch("avtaletype");
  const forhandsgodkjent = watchedAvtaletype === Avtaletype.FORHANDSGODKJENT;
  const gjeldendeOpsjonsmodeller = hentGjeldendeOpsjonsmodeller(watchedAvtaletype);

  const opsjonsmodellType = watch("opsjonsmodell.type");
  const opsjonsmodell = opsjonsmodellType ? hentOpsjonsmodell(opsjonsmodellType) : undefined;

  const skalIkkeKunneRedigereOpsjoner = antallOpsjonerUtlost > 0;

  const readonly = opsjonsmodell?.value !== "ANNET" || skalIkkeKunneRedigereOpsjoner;

  useEffect(() => {
    if (startDato && opsjonsmodell && antallOpsjonerUtlost === 0) {
      if (opsjonsmodell.initialSluttdatoEkstraAar) {
        setValue(
          "sluttDato",
          kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.initialSluttdatoEkstraAar).toISOString(),
        );
      }
      if (opsjonsmodell.maksVarighetAar) {
        setValue(
          "opsjonsmodell.opsjonMaksVarighet",
          kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.maksVarighetAar).toISOString(),
        );
      }
    }
  }, [antallOpsjonerUtlost, opsjonsmodell, startDato, sluttDatoFraDato, setValue]);
  const avtaletype = watch("avtaletype");
  if (!avtaletype) {
    return null;
  }

  return (
    <>
      {!forhandsgodkjent ? (
        <HGrid columns={2}>
          <Select
            readOnly={skalIkkeKunneRedigereOpsjoner}
            label="Avtalt mulighet for forlengelse"
            size="small"
            value={opsjonsmodell?.value}
            error={(errors.opsjonsmodell?.type as FieldError | undefined)?.message}
            onChange={(e) => {
              const opsjonsmodell = gjeldendeOpsjonsmodeller.find(
                (modell) => modell.value === e.target.value,
              );
              if (opsjonsmodell) {
                setValue("opsjonsmodell.type", opsjonsmodell.value);
                setValue("opsjonsmodell.customOpsjonsmodellNavn", undefined);
                setValue("opsjonsmodell.opsjonMaksVarighet", undefined);
              }
            }}
          >
            <option value={undefined}>Velg avtalt mulighet for forlengelse</option>
            {gjeldendeOpsjonsmodeller.map((modell) => (
              <option key={modell.value} value={modell.value}>
                {modell.label}
              </option>
            ))}
          </Select>
        </HGrid>
      ) : null}

      {opsjonsmodell?.value === "ANNET" ? (
        <TextField
          label="Opsjonsnavn"
          readOnly={readonly}
          hideLabel
          error={errors.opsjonsmodell?.customOpsjonsmodellNavn?.message}
          placeholder="Beskriv opsjonsmodellen"
          size="small"
          {...register("opsjonsmodell.customOpsjonsmodellNavn")}
        />
      ) : null}

      {opsjonsmodell && opsjonsmodell.kreverMaksVarighet ? (
        <HGrid columns={3} gap="5" align="end">
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            readOnly={skalIkkeKunneRedigereOpsjoner}
            fromDate={MIN_START_DATO_FOR_AVTALER}
            toDate={sluttDatoTilDato}
            {...register("startDato")}
            format={"iso-string"}
            control={control}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.sluttdatoLabel(false)}
            readOnly={readonly}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            {...register("sluttDato")}
            format={"iso-string"}
            invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${MAKS_AAR_FOR_AVTALER} år`}
            control={control}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.maksVarighetLabel}
            readOnly={readonly}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            {...register("opsjonsmodell.opsjonMaksVarighet")}
            format={"iso-string"}
            control={control}
          />
        </HGrid>
      ) : (
        <HGrid columns={3} gap="10">
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            fromDate={MIN_START_DATO_FOR_AVTALER}
            toDate={sluttDatoTilDato}
            {...register("startDato")}
            format={"iso-string"}
            control={control}
          />
          <ControlledDateInput
            size="small"
            label={
              forhandsgodkjent
                ? avtaletekster.valgfriSluttdatoLabel(watchedAvtaletype)
                : avtaletekster.sluttdatoLabel(false)
            }
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            {...register("sluttDato")}
            format={"iso-string"}
            control={control}
          />
        </HGrid>
      )}
      {antallOpsjonerUtlost && <RegistrerteOpsjoner readOnly />}
    </>
  );
}

function kalkulerMaksDato(date: Date, addYears: number): Date {
  const resultDate = new Date(date.getTime());
  resultDate.setFullYear(resultDate.getFullYear() + addYears);
  const daysInMilliseconds = 1 * 24 * 60 * 60 * 1000;
  return new Date(resultDate.getTime() - daysInMilliseconds);
}
