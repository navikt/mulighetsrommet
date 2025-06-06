import { AvtaleDto, Avtaletype, OpsjonStatus } from "@mr/api-client-v2";
import { Heading, HGrid, Select, TextField } from "@navikt/ds-react";
import { useEffect } from "react";
import { FieldError, useFormContext } from "react-hook-form";
import { MIN_START_DATO_FOR_AVTALER } from "@/constants";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../../redaksjoneltInnhold/AvtaleSchema";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { RegistrerteOpsjoner } from "../opsjoner/RegistrerteOpsjoner";
import {
  hentGjeldendeOpsjonsmodeller,
  hentOpsjonsmodell,
} from "@/components/avtaler/opsjoner/opsjonsmodeller";

interface Props {
  avtale?: AvtaleDto;
  avtaletype: Avtaletype;
  minStartDato: Date;
  sluttDatoFraDato: Date;
  sluttDatoTilDato: Date;
  maksAar: number;
}

export function AvtaleVarighet({
  avtale,
  avtaletype,
  minStartDato,
  sluttDatoFraDato,
  sluttDatoTilDato,
  maksAar,
}: Props) {
  const {
    register,
    setValue,
    watch,
    formState: { errors },
    control,
  } = useFormContext<InferredAvtaleSchema>();

  const gjeldendeOpsjonsmodeller = hentGjeldendeOpsjonsmodeller(avtaletype);

  const opsjonsmodellType = watch("opsjonsmodell.type");
  const opsjonsmodell = opsjonsmodellType ? hentOpsjonsmodell(opsjonsmodellType) : undefined;

  const antallOpsjonerUtlost = (
    avtale?.opsjonerRegistrert?.filter((log) => log.status === OpsjonStatus.OPSJON_UTLOST) || []
  ).length;

  const skalIkkeKunneRedigereOpsjoner = antallOpsjonerUtlost > 0;

  const { startDato } = watch("startOgSluttDato");
  const readonly = opsjonsmodell?.value !== "ANNET" || skalIkkeKunneRedigereOpsjoner;

  useEffect(() => {
    if (startDato && opsjonsmodell && antallOpsjonerUtlost === 0) {
      if (opsjonsmodell.initialSluttdatoEkstraAar) {
        setValue(
          "startOgSluttDato.sluttDato",
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

  return (
    <>
      <Heading size="small" as="h3">
        Avtalens varighet
      </Heading>

      <HGrid columns={2}>
        {avtaletype !== Avtaletype.FORHANDSGODKJENT ? (
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
        ) : null}
      </HGrid>

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
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.startDato")}
            format={"iso-string"}
            control={control}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.sluttdatoLabel(false)}
            readOnly={readonly}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.sluttDato")}
            format={"iso-string"}
            invalidDatoEtterPeriode={`Avtaleperioden kan ikke vare lenger enn ${maksAar} år`}
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
            {...register("startOgSluttDato.startDato")}
            format={"iso-string"}
            control={control}
          />
          <ControlledDateInput
            size="small"
            label={
              avtaletype === Avtaletype.FORHANDSGODKJENT
                ? avtaletekster.valgfriSluttdatoLabel(avtaletype)
                : avtaletekster.sluttdatoLabel(false)
            }
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.sluttDato")}
            format={"iso-string"}
            control={control}
          />
        </HGrid>
      )}
      {avtale && avtale.opsjonerRegistrert.length > 0 && (
        <RegistrerteOpsjoner readOnly avtale={avtale} />
      )}
    </>
  );
}

function kalkulerMaksDato(date: Date, addYears: number): Date {
  const resultDate = new Date(date.getTime());
  resultDate.setFullYear(resultDate.getFullYear() + addYears);
  const daysInMilliseconds = 1 * 24 * 60 * 60 * 1000;
  return new Date(resultDate.getTime() - daysInMilliseconds);
}
