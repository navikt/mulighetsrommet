import { Avtale, Avtaletype, OpsjonsmodellKey, OpsjonStatus } from "@mr/api-client";
import { Heading, HGrid, Select, TextField } from "@navikt/ds-react";
import { useEffect } from "react";
import { useFormContext } from "react-hook-form";
import { MIN_START_DATO_FOR_AVTALER } from "../../../constants";
import { avtaletekster } from "../../ledetekster/avtaleLedetekster";
import { InferredAvtaleSchema } from "../../redaksjoneltInnhold/AvtaleSchema";
import { ControlledDateInput } from "../../skjema/ControlledDateInput";
import { OpsjonerRegistrert } from "../opsjoner/OpsjonerRegistrert";
import { Opsjonsmodell, opsjonsmodeller } from "../opsjoner/opsjonsmodeller";

interface Props {
  avtale?: Avtale;
  avtaletype: Avtaletype;
  arenaOpphavOgIngenEierskap: boolean;
  minStartDato: Date;
  sluttDatoFraDato: Date;
  sluttDatoTilDato: Date;
  maksAar: number;
  opsjonsmodell?: Opsjonsmodell;
}

export function AvtaleVarighet({
  avtale,
  avtaletype,
  arenaOpphavOgIngenEierskap,
  minStartDato,
  sluttDatoFraDato,
  sluttDatoTilDato,
  maksAar,
  opsjonsmodell,
}: Props) {
  const {
    register,
    setValue,
    watch,
    formState: { errors },
  } = useFormContext<InferredAvtaleSchema>();

  const antallOpsjonerUtlost = (
    avtale?.opsjonerRegistrert?.filter((log) => log.status === OpsjonStatus.OPSJON_UTLØST) || []
  ).length;

  const skalIkkeKunneRedigereOpsjoner = antallOpsjonerUtlost > 0;

  const { startDato } = watch("startOgSluttDato");
  const readonly =
    opsjonsmodell?.value !== "ANNET" || arenaOpphavOgIngenEierskap || skalIkkeKunneRedigereOpsjoner;

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
          "opsjonsmodellData.opsjonMaksVarighet",
          kalkulerMaksDato(sluttDatoFraDato, opsjonsmodell.maksVarighetAar).toISOString(),
        );
      }
    }
  }, [antallOpsjonerUtlost, opsjonsmodell, startDato, sluttDatoFraDato, setValue]);

  const maksVarighetAar = opsjonsmodell?.maksVarighetAar ?? 5;
  const maksVarighetDato = kalkulerMaksDato(new Date(startDato!), maksVarighetAar);

  const gjeldendeOpsjonsmodeller = hentModeller(avtaletype);

  return (
    <>
      <Heading size="small" as="h3">
        Avtalens varighet
      </Heading>

      <HGrid columns={2}>
        {avtaletype !== Avtaletype.FORHAANDSGODKJENT ? (
          <Select
            readOnly={skalIkkeKunneRedigereOpsjoner}
            label="Avtalt mulighet for forlengelse"
            size="small"
            value={opsjonsmodell?.value}
            error={errors.opsjonsmodellData?.opsjonsmodell?.message}
            onChange={(e) => {
              const opsjonsmodell = opsjonsmodeller.find(
                (modell) => modell.value === e.target.value,
              );
              if (opsjonsmodell) {
                setValue("opsjonsmodellData.opsjonsmodell", opsjonsmodell?.value);
                setValue("opsjonsmodellData.customOpsjonsmodellNavn", undefined);
                setValue("opsjonsmodellData.opsjonMaksVarighet", undefined);
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
          error={errors.opsjonsmodellData?.customOpsjonsmodellNavn?.message}
          placeholder="Beskriv opsjonsmodellen"
          size="small"
          {...register("opsjonsmodellData.customOpsjonsmodellNavn")}
        />
      ) : null}

      {opsjonsmodell && opsjonsmodell.kreverMaksVarighet ? (
        <HGrid columns={3}>
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            readOnly={skalIkkeKunneRedigereOpsjoner}
            fromDate={minStartDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.startDato")}
            format={"iso-string"}
          />
          <ControlledDateInput
            size="small"
            label={avtaletekster.sluttdatoLabel(false)}
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
            {...register("opsjonsmodellData.opsjonMaksVarighet")}
            format={"iso-string"}
          />
        </HGrid>
      ) : opsjonsmodell && !opsjonsmodell.kreverMaksVarighet ? (
        <HGrid columns={3}>
          <ControlledDateInput
            size="small"
            label={avtaletekster.startdatoLabel}
            readOnly={arenaOpphavOgIngenEierskap}
            fromDate={MIN_START_DATO_FOR_AVTALER}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.startDato")}
            format={"iso-string"}
          />
          <ControlledDateInput
            size="small"
            label={
              avtaletype === Avtaletype.FORHAANDSGODKJENT
                ? avtaletekster.valgfriSluttdatoLabel(avtaletype)
                : avtaletekster.sluttdatoLabel(false)
            }
            readOnly={arenaOpphavOgIngenEierskap}
            fromDate={sluttDatoFraDato}
            toDate={sluttDatoTilDato}
            {...register("startOgSluttDato.sluttDato")}
            format={"iso-string"}
          />
        </HGrid>
      ) : null}
      {avtale && avtale.opsjonerRegistrert.length > 0 && (
        <OpsjonerRegistrert readOnly avtale={avtale} />
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

function hentModeller(avtaletype: Avtaletype | undefined): Opsjonsmodell[] {
  if (!avtaletype) {
    return [];
  }

  if (avtaletype === Avtaletype.FORHAANDSGODKJENT) {
    return opsjonsmodeller.filter(
      (modell) => modell.value === OpsjonsmodellKey.AVTALE_VALGFRI_SLUTTDATO,
    );
  }

  if (avtaletype !== Avtaletype.OFFENTLIG_OFFENTLIG) {
    return opsjonsmodeller.filter(
      (modell) => modell.value !== OpsjonsmodellKey.AVTALE_VALGFRI_SLUTTDATO,
    );
  }
  return opsjonsmodeller;
}
