import { HGrid, Select, TextField, VStack } from "@navikt/ds-react";
import { useEffect, useMemo, useRef } from "react";
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
import { addDuration, subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { Avtaletype, OpsjonsmodellType } from "@tiltaksadministrasjon/api-client";

interface Props {
  opsjonUtlost: boolean;
}

export function AvtaleVarighet({ opsjonUtlost }: Props) {
  const {
    register,
    setValue,
    watch,
    getValues,
    formState: { errors },
  } = useFormContext<AvtaleFormValues>();
  const initialStartDato = useRef(getValues("startDato"));
  const startDato = watch("startDato");
  const sluttDato = watch("sluttDato");

  const minStartDato = initialStartDato.current
    ? new Date(initialStartDato.current)
    : MIN_START_DATO_FOR_AVTALER;

  const sluttDatoFraDato = useMemo(
    () => (startDato ? new Date(startDato) : MIN_START_DATO_FOR_AVTALER),
    [startDato],
  );

  const sluttDatoTilDato = useMemo(
    () =>
      addDuration(startDato ? new Date(startDato) : new Date(), { years: MAKS_AAR_FOR_AVTALER }),
    [startDato],
  );

  const watchedAvtaletype = watch("avtaletype");
  const watchedOpsjonsmodell = watch("opsjonsmodell");
  const forhandsgodkjent = watchedAvtaletype === Avtaletype.FORHANDSGODKJENT;
  const gjeldendeOpsjonsmodeller = hentGjeldendeOpsjonsmodeller(watchedAvtaletype);

  const opsjonsmodellType = watch("opsjonsmodell.type");
  const opsjonsmodell = hentOpsjonsmodell(opsjonsmodellType);

  useEffect(() => {
    if (startDato && opsjonsmodell && !opsjonUtlost) {
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
  }, [opsjonUtlost, opsjonsmodell, startDato, sluttDatoFraDato, setValue]);

  return (
    <VStack gap="4">
      {!forhandsgodkjent && (
        <HGrid columns={2}>
          <Select
            readOnly={opsjonUtlost}
            label="Avtalt mulighet for forlengelse"
            size="small"
            value={opsjonsmodell?.type}
            error={(errors.opsjonsmodell?.type as FieldError | undefined)?.message}
            onChange={(e) => {
              const opsjonsmodell = gjeldendeOpsjonsmodeller.find(
                (modell) => modell.type === e.target.value,
              );
              if (opsjonsmodell) {
                setValue("opsjonsmodell.type", opsjonsmodell.type);
                setValue("opsjonsmodell.customOpsjonsmodellNavn", undefined);
                setValue("opsjonsmodell.opsjonMaksVarighet", undefined);
              }
            }}
          >
            <option value={undefined}>Velg avtalt mulighet for forlengelse</option>
            {gjeldendeOpsjonsmodeller.map((modell) => (
              <option key={modell.type} value={modell.type}>
                {modell.label}
              </option>
            ))}
          </Select>
        </HGrid>
      )}

      {opsjonsmodell?.type === "ANNET" && (
        <TextField
          label="Opsjonsnavn"
          readOnly={opsjonUtlost}
          hideLabel
          error={errors.opsjonsmodell?.customOpsjonsmodellNavn?.message}
          placeholder="Beskriv opsjonsmodellen"
          size="small"
          {...register("opsjonsmodell.customOpsjonsmodellNavn")}
        />
      )}
      {opsjonsmodell?.kreverMaksVarighet ? (
        <HGrid columns={3} gap="5" align="end">
          <ControlledDateInput
            label={avtaletekster.startdatoLabel}
            readOnly={opsjonUtlost}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("startDato", val)}
            defaultSelected={startDato}
            error={errors.startDato?.message}
          />
          <ControlledDateInput
            key={sluttDato}
            label={avtaletekster.sluttdatoLabel(watchedAvtaletype, opsjonUtlost)}
            readOnly={opsjonUtlost || opsjonsmodell.type !== OpsjonsmodellType.ANNET}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("sluttDato", val)}
            defaultSelected={getValues("sluttDato")}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
            error={errors.sluttDato?.message}
          />
          <ControlledDateInput
            key={watchedOpsjonsmodell.opsjonMaksVarighet}
            onChange={(val) => setValue("opsjonsmodell.opsjonMaksVarighet", val)}
            defaultSelected={getValues("opsjonsmodell.opsjonMaksVarighet")}
            label={avtaletekster.maksVarighetLabel}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            readOnly={opsjonUtlost || opsjonsmodell.type !== OpsjonsmodellType.ANNET}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
          />
        </HGrid>
      ) : (
        <HGrid columns={3} gap="10">
          <ControlledDateInput
            label={avtaletekster.startdatoLabel}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("startDato", val)}
            defaultSelected={startDato}
            error={errors.startDato?.message}
          />
          <ControlledDateInput
            label={avtaletekster.sluttdatoLabel(watchedAvtaletype, opsjonUtlost)}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("sluttDato", val)}
            defaultSelected={getValues("sluttDato")}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
            error={errors.sluttDato?.message}
          />
        </HGrid>
      )}
      {opsjonUtlost && <RegistrerteOpsjoner readOnly />}
    </VStack>
  );
}

function kalkulerMaksDato(date: Date, addYears: number): Date {
  return subDuration(addDuration(date, { years: addYears }), { days: 1 });
}
