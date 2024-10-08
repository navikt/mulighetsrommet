import { HStack, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { DeepPartial, FieldError, FieldErrorsImpl, Merge, useFormContext } from "react-hook-form";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { NumericFormat } from "react-number-format";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { useHandleApiUpsertResponse } from "@/api/effects";
import { useAFTSatser } from "@/api/tilsagn/useAFTSatser";
import { AFTSats, TilsagnBeregningAFT } from "@mr/api-client";

interface Props {
  defaultAntallPlasser?: number;
}

export function AFTBeregningSkjema({ defaultAntallPlasser }: Props) {
  const { data: satser } = useAFTSatser();
  const mutation = useBeregnTilsagn();
  const {
    setError,
    clearErrors,
    watch,
    setValue,
    formState: { errors },
  } = useFormContext<DeepPartial<InferredOpprettTilsagnSchema>>();

  const [antallPlasser, setAntallPlasser] = useState<number>(defaultAntallPlasser ?? 0);

  const periode = watch("periode");
  const beregning = watch("beregning");

  function findSats(): number | undefined {
    if (!periode?.start) {
      return;
    }
    const periodeStart = new Date(periode.start);
    const filteredData =
      satser
        ?.filter((sats: AFTSats) => new Date(sats.startDato) <= periodeStart)
        ?.sort(
          (a: AFTSats, b: AFTSats) =>
            new Date(b.startDato).getTime() - new Date(a.startDato).getTime(),
        ) ?? [];

    return filteredData[0]?.belop;
  }

  useEffect(() => {
    const sats = findSats();
    if (sats && periode?.start && periode.slutt && mutation) {
      mutation.mutate({
        type: "AFT",
        periodeStart: periode.start,
        periodeSlutt: periode.slutt,
        sats,
        antallPlasser,
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [antallPlasser, periode?.start, periode?.slutt, setValue]);

  useHandleApiUpsertResponse(
    mutation,
    (response) => {
      clearErrors();
      setValue("beregning", response);
    },
    (validation) => {
      setValue("beregning", undefined);
      validation.errors.forEach((error) => {
        const name = mapErrorToSchemaPropertyName(error.name);
        setError(name, { type: "custom", message: error.message });
      });

      function mapErrorToSchemaPropertyName(name: string) {
        const mapping: { [name: string]: string } = {
          periodeStart: "periode.start",
          periodeSlutt: "periode.slutt",
          antallPlasser: "beregning",
        };
        return (mapping[name] ?? name) as keyof InferredOpprettTilsagnSchema;
      }
    },
  );

  return (
    <HStack gap="2" align="start">
      <NumericFormat
        size="small"
        error={errors.beregning?.message}
        label="Antall plasser"
        customInput={TextField}
        value={antallPlasser}
        valueIsNumericString
        thousandSeparator
        onValueChange={(e) => {
          const n = Number.parseInt(e.value);
          setAntallPlasser(isNaN(n) ? 0 : n);
        }}
      />
      <NumericFormat
        readOnly
        size="small"
        label="Sats"
        error={
          (errors.beregning as Merge<FieldError, FieldErrorsImpl<NonNullable<TilsagnBeregningAFT>>>)
            ?.sats?.message
        }
        customInput={TextField}
        value={findSats()}
        valueIsNumericString
        thousandSeparator=" "
        suffix=" kr"
      />
      <NumericFormat
        readOnly
        size="small"
        label="Beløp"
        customInput={TextField}
        value={beregning?.belop ?? ""}
        valueIsNumericString
        thousandSeparator=" "
        suffix=" kr"
      />
    </HStack>
  );
}
