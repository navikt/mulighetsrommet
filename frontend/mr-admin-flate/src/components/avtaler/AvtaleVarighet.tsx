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
import { formaterDato, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
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
  } = useFormContext<AvtaleFormValues>();

  const startDato = watch("startDato");
  const sluttDato = watch("sluttDato");

  const sluttDatoFraDato = useMemo(
    () => (startDato ? new Date(startDato) : MIN_START_DATO_FOR_AVTALER),
    [startDato],
  );
  const sluttDatoTilDato = useMemo(
    () => addYear(startDato ? new Date(startDato) : new Date(), MAKS_AAR_FOR_AVTALER),
    [startDato],
  );

  const watchedAvtaletype = watch("avtaletype");
  const watchedOpsjonsmodell = watch("opsjonsmodell");
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
          yyyyMMddFormatting(
            kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.initialSluttdatoEkstraAar),
          ) ?? "",
        );
      }
      if (opsjonsmodell.maksVarighetAar) {
        setValue(
          "opsjonsmodell.opsjonMaksVarighet",
          yyyyMMddFormatting(kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.maksVarighetAar)),
        );
      }
    }
  }, [antallOpsjonerUtlost, opsjonsmodell, startDato, sluttDatoFraDato, setValue]);

  return (
    <>
      {!forhandsgodkjent && (
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
      )}

      {opsjonsmodell?.value === "ANNET" && (
        <TextField
          label="Opsjonsnavn"
          readOnly={readonly}
          hideLabel
          error={errors.opsjonsmodell?.customOpsjonsmodellNavn?.message}
          placeholder="Beskriv opsjonsmodellen"
          size="small"
          {...register("opsjonsmodell.customOpsjonsmodellNavn")}
        />
      )}

      {opsjonsmodell && opsjonsmodell.kreverMaksVarighet ? (
        <HGrid columns={3} gap="5" align="end">
          <ControlledDateInput
            label={avtaletekster.startdatoLabel}
            readOnly={skalIkkeKunneRedigereOpsjoner}
            fromDate={MIN_START_DATO_FOR_AVTALER}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("startDato", val)}
            defaultSelected={startDato}
            error={errors.startDato?.message}
          />
          <TextField
            size="small"
            label={avtaletekster.sluttdatoLabel(false)}
            readOnly={readonly}
            error={
              errors.sluttDato &&
              `Avtaleperioden kan ikke vare lenger enn ${MAKS_AAR_FOR_AVTALER} år`
            }
            className="max-w-fit"
            value={formaterDato(sluttDato)}
            {...register("sluttDato")}
          />
          {watchedOpsjonsmodell.opsjonMaksVarighet && (
            <TextField
              size="small"
              label={avtaletekster.maksVarighetLabel}
              readOnly={readonly}
              error={errors.opsjonsmodell?.opsjonMaksVarighet?.message}
              className="max-w-fit"
              value={formaterDato(watchedOpsjonsmodell.opsjonMaksVarighet)}
              {...register("opsjonsmodell.opsjonMaksVarighet")}
            />
          )}
        </HGrid>
      ) : (
        <HGrid columns={3} gap="10">
          <ControlledDateInput
            label={avtaletekster.startdatoLabel}
            fromDate={MIN_START_DATO_FOR_AVTALER}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("startDato", val)}
            defaultSelected={startDato}
            error={errors.startDato?.message}
          />
          <ControlledDateInput
            label={
              forhandsgodkjent
                ? avtaletekster.valgfriSluttdatoLabel(watchedAvtaletype)
                : avtaletekster.sluttdatoLabel(false)
            }
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("sluttDato", val)}
            defaultSelected={sluttDato}
            error={errors.sluttDato?.message}
          />
        </HGrid>
      )}
      {antallOpsjonerUtlost > 0 && <RegistrerteOpsjoner readOnly />}
    </>
  );
}

function kalkulerMaksDato(date: Date, addYears: number): Date {
  const resultDate = new Date(date.getTime());
  resultDate.setFullYear(resultDate.getFullYear() + addYears);
  const daysInMilliseconds = 1 * 24 * 60 * 60 * 1000;
  return new Date(resultDate.getTime() - daysInMilliseconds);
}
