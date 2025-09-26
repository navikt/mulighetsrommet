import { Avtaletype, OpsjonsmodellType } from "@mr/api-client-v2";
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
import { addDuration, subDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { AvtaleFormValues } from "@/schemas/avtale";
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
  const initialStartDato = useRef(getValues("detaljer.startDato"));
  const startDato = watch("detaljer.startDato");
  const sluttDato = watch("detaljer.sluttDato");

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

  const watchedAvtaletype = watch("detaljer.avtaletype");
  const watchedOpsjonsmodell = watch("detaljer.opsjonsmodell");
  const forhandsgodkjent = watchedAvtaletype === Avtaletype.FORHANDSGODKJENT;
  const gjeldendeOpsjonsmodeller = hentGjeldendeOpsjonsmodeller(watchedAvtaletype);

  const opsjonsmodellType = watch("detaljer.opsjonsmodell.type");
  const opsjonsmodell = hentOpsjonsmodell(opsjonsmodellType);

  useEffect(() => {
    if (startDato && opsjonsmodell && !opsjonUtlost) {
      if (opsjonsmodell.initialSluttdatoEkstraAar) {
        setValue(
          "detaljer.sluttDato",
          yyyyMMddFormatting(
            kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.initialSluttdatoEkstraAar),
          ) ?? "",
        );
      }
      if (opsjonsmodell.maksVarighetAar) {
        setValue(
          "detaljer.opsjonsmodell.maksVarighet",
          yyyyMMddFormatting(kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.maksVarighetAar)) ??
            null,
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
            error={(errors.detaljer?.opsjonsmodell?.type as FieldError | undefined)?.message}
            onChange={(e) => {
              const opsjonsmodell = gjeldendeOpsjonsmodeller.find(
                (modell) => modell.type === e.target.value,
              );
              if (opsjonsmodell) {
                setValue("detaljer.opsjonsmodell.type", opsjonsmodell.type);
                setValue("detaljer.opsjonsmodell.customNavn", null);
                setValue("detaljer.opsjonsmodell.maksVarighet", null);
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
          error={errors.detaljer?.opsjonsmodell?.customNavn?.message}
          placeholder="Beskriv opsjonsmodellen"
          size="small"
          {...register("detaljer.opsjonsmodell.customNavn")}
        />
      )}
      {opsjonsmodell?.kreverMaksVarighet ? (
        <HGrid columns={3} gap="5" align="end">
          <ControlledDateInput
            label={avtaletekster.startdatoLabel}
            readOnly={opsjonUtlost}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("detaljer.startDato", val)}
            defaultSelected={startDato}
            error={errors.detaljer?.startDato?.message}
          />
          <ControlledDateInput
            key={sluttDato}
            label={avtaletekster.sluttdatoLabel(watchedAvtaletype, opsjonUtlost)}
            readOnly={opsjonUtlost || opsjonsmodell?.type !== OpsjonsmodellType.ANNET}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("detaljer.sluttDato", val)}
            defaultSelected={getValues("detaljer.sluttDato")}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
            error={errors.detaljer?.sluttDato?.message}
          />
          <ControlledDateInput
            key={watchedOpsjonsmodell.maksVarighet}
            onChange={(val) => setValue("detaljer.opsjonsmodell.maksVarighet", val)}
            defaultSelected={getValues("detaljer.opsjonsmodell.maksVarighet")}
            label={avtaletekster.maksVarighetLabel}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            readOnly={opsjonUtlost || opsjonsmodell?.type !== OpsjonsmodellType.ANNET}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
          />
        </HGrid>
      ) : (
        <HGrid columns={3} gap="10">
          <ControlledDateInput
            label={avtaletekster.startdatoLabel}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("detaljer.startDato", val)}
            defaultSelected={startDato}
            error={errors.detaljer?.startDato?.message}
          />
          <ControlledDateInput
            label={avtaletekster.sluttdatoLabel(watchedAvtaletype, opsjonUtlost)}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            onChange={(val) => setValue("detaljer.sluttDato", val)}
            defaultSelected={getValues("detaljer.sluttDato")}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
            error={errors.detaljer?.sluttDato?.message}
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
