import { HStack, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";
import { NumericFormat } from "react-number-format";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { useHandleApiUpsertResponse } from "@/api/effects";

interface Props {
  defaultAntallPlasser?: number;
}

// TODO: Hent fra backend gitt periodeStart. Eller hent liste fra backend
const sats = 20205;

export function AFTBeregningSkjema({ defaultAntallPlasser }: Props) {
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

  useEffect(() => {
    if (periode?.start && periode.slutt && mutation) {
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
        customInput={TextField}
        value={sats}
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
