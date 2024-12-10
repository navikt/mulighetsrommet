import { useAFTSatser } from "@/api/tilsagn/useAFTSatser";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import {
  AFTSats,
  TilsagnBeregning,
  TilsagnBeregningAFT,
  ValidationErrorResponse,
} from "@mr/api-client";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { HStack, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { DeepPartial, FieldError, FieldErrorsImpl, Merge, useFormContext } from "react-hook-form";
import { NumericFormat } from "react-number-format";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";

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

  const [antallPlasser, setAntallPlasser] = useState<number | null>(defaultAntallPlasser ?? null);

  const periode = watch("periode");

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
    if (sats && periode?.start && periode.slutt && antallPlasser && mutation) {
      mutation.mutate(
        {
          type: "AFT",
          periodeStart: periode.start,
          periodeSlutt: periode.slutt,
          sats,
          antallPlasser,
        },
        {
          onSuccess: handleSuccess,
          onError: (error) => {
            if (isValidationError(error)) {
              handleValidationErrors(error);
            }
          },
        },
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [antallPlasser, periode?.start, periode?.slutt, setValue]);

  const handleSuccess = (response: TilsagnBeregning) => {
    clearErrors();
    setValue("beregning", response);
  };

  const handleValidationErrors = (validation: ValidationErrorResponse) => {
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
  };

  return (
    <HStack gap="2" align="start">
      <TextField
        size="small"
        value={antallPlasser ?? ""}
        type="number"
        label="Antall plasser"
        error={
          (errors.beregning as Merge<FieldError, FieldErrorsImpl<NonNullable<TilsagnBeregningAFT>>>)
            ?.antallPlasser?.message
        }
        onChange={(e) => {
          const n = Number.parseInt(e.target.value);
          const a = isNaN(n) ? null : n;
          setAntallPlasser(a);
          setValue("beregning.antallPlasser", a ?? undefined);
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
    </HStack>
  );
}
