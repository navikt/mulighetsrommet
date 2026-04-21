import { HGrid, VStack } from "@navikt/ds-react";
import { useEffect, useMemo, useRef } from "react";
import { useFormContext } from "react-hook-form";
import { MAKS_AAR_FOR_AVTALER, MIN_START_DATO_FOR_AVTALER } from "@/constants";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { RegistrerteOpsjoner } from "./opsjoner/RegistrerteOpsjoner";
import {
  hentGjeldendeOpsjonsmodeller,
  hentOpsjonsmodell,
} from "@/components/avtaler/opsjoner/opsjonsmodeller";
import { AvtaleFormValues } from "@/schemas/avtale";
import { addDuration, SafeSubDuration, yyyyMMddFormatting } from "@mr/frontend-common/utils/date";
import { Avtaletype, OpsjonsmodellType } from "@tiltaksadministrasjon/api-client";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormDateInput } from "@/components/skjema/FormDateInput";

interface Props {
  opsjonUtlost: boolean;
}

export function AvtaleVarighet({ opsjonUtlost }: Props) {
  const { setValue, watch, getValues } = useFormContext<AvtaleFormValues>();
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
          "detaljer.opsjonsmodell.opsjonMaksVarighet",
          yyyyMMddFormatting(kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.maksVarighetAar)),
        );
      }
    }
  }, [opsjonUtlost, opsjonsmodell, startDato, sluttDatoFraDato, setValue]);

  return (
    <VStack gap="space-16">
      {!forhandsgodkjent && (
        <HGrid columns={2}>
          <FormSelect
            readOnly={opsjonUtlost}
            label="Avtalt mulighet for forlengelse"
            size="small"
            name={"detaljer.opsjonsmodell.type"}
            rules={{
              onChange: (e) => {
                const opsjonsmodell = gjeldendeOpsjonsmodeller.find(
                  (modell) => modell.type === e.target.value,
                );
                if (opsjonsmodell) {
                  setValue("detaljer.opsjonsmodell.customOpsjonsmodellNavn", undefined);
                  setValue("detaljer.opsjonsmodell.opsjonMaksVarighet", undefined);
                }
              },
            }}
          >
            <option value={undefined}>Velg avtalt mulighet for forlengelse</option>
            {gjeldendeOpsjonsmodeller.map((modell) => (
              <option key={modell.type} value={modell.type}>
                {modell.label}
              </option>
            ))}
          </FormSelect>
        </HGrid>
      )}
      {opsjonsmodell?.type === "ANNET" && (
        <FormTextField
          label="Opsjonsnavn"
          readOnly={opsjonUtlost}
          hideLabel
          placeholder="Beskriv opsjonsmodellen"
          size="small"
          name={"detaljer.opsjonsmodell.customOpsjonsmodellNavn"}
        />
      )}
      {opsjonsmodell?.kreverMaksVarighet ? (
        <HGrid columns={3} gap="space-20" align="end">
          <FormDateInput
            label={avtaletekster.startdatoLabel}
            readOnly={opsjonUtlost}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            name={"detaljer.startDato"}
          />
          <FormDateInput
            key={sluttDato}
            name={"detaljer.sluttDato"}
            label={avtaletekster.sluttdatoLabel(watchedAvtaletype, opsjonUtlost)}
            readOnly={opsjonUtlost || opsjonsmodell.type !== OpsjonsmodellType.ANNET}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
          />
          <FormDateInput
            key={watchedOpsjonsmodell.opsjonMaksVarighet}
            name="detaljer.opsjonsmodell.opsjonMaksVarighet"
            label={avtaletekster.maksVarighetLabel}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            readOnly={opsjonUtlost || opsjonsmodell.type !== OpsjonsmodellType.ANNET}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
          />
        </HGrid>
      ) : (
        <HGrid columns={3} gap="space-40">
          <FormDateInput
            name={"detaljer.startDato"}
            label={avtaletekster.startdatoLabel}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
          />
          <FormDateInput
            name={"detaljer.sluttDato"}
            label={avtaletekster.sluttdatoLabel(watchedAvtaletype, opsjonUtlost)}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            invalidDatoEtterPeriode={`Sluttdato kan ikke settes lenger enn ${MAKS_AAR_FOR_AVTALER} år frem i tid`}
          />
        </HGrid>
      )}
      {opsjonUtlost && <RegistrerteOpsjoner readOnly />}
    </VStack>
  );
}

function kalkulerMaksDato(date: Date, addYears: number): Date {
  return SafeSubDuration(addDuration(date, { years: addYears }), { days: 1 });
}
