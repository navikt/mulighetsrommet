import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { TilsagnBeregning, TilsagnBeregningAFT, ValidationErrorResponse } from "@mr/api-client";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { HStack, TextField } from "@navikt/ds-react";
import { useEffect } from "react";
import { DeepPartial, FieldError, FieldErrorsImpl, Merge, useFormContext } from "react-hook-form";
import { NumericFormat } from "react-number-format";
import { InferredOpprettTilsagnSchema } from "./OpprettTilsagnSchema";

export function AFTBeregningSkjema() {
  const beregnTilsagn = useBeregnTilsagn();
  const {
    setError,
    clearErrors,
    watch,
    setValue,
    formState: { errors },
    register,
  } = useFormContext<DeepPartial<InferredOpprettTilsagnSchema>>();

  const periodeStart = watch("periodeStart");
  const periodeSlutt = watch("periodeSlutt");

  const beregning = watch("beregning") as TilsagnBeregningAFT | undefined;

  useEffect(() => {
    if (periodeStart && periodeSlutt && beregning?.antallPlasser) {
      beregnTilsagn.mutate(
        {
          type: "AFT",
          periodeStart,
          periodeSlutt,
          antallPlasser: beregning.antallPlasser,
        },
        {
          onSuccess: setBeregning,
          onError: (error) => {
            if (isValidationError(error.body)) {
              handleBeregningValidationErrors(error.body);
            }
          },
        },
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [beregning?.antallPlasser, periodeStart, periodeSlutt, setValue]);

  const setBeregning = (response: TilsagnBeregning) => {
    clearErrors();
    setValue("beregning", response);
  };

  const handleBeregningValidationErrors = (validation: ValidationErrorResponse) => {
    setValue("beregning", undefined);

    validation.errors.forEach((error) => {
      const name = error.name as keyof InferredOpprettTilsagnSchema;
      setError(name, { type: "custom", message: error.message });
    });
  };

  return (
    <HStack gap="2" align="start">
      <TextField
        size="small"
        type="number"
        label="Antall plasser"
        error={
          (errors.beregning as Merge<FieldError, FieldErrorsImpl<NonNullable<TilsagnBeregningAFT>>>)
            ?.antallPlasser?.message
        }
        {...register("beregning.antallPlasser", { valueAsNumber: true })}
      />
      <NumericFormat
        readOnly
        size="small"
        label="Sats"
        customInput={TextField}
        value={beregning?.sats}
        valueIsNumericString
        thousandSeparator=" "
        suffix=" kr"
      />
    </HStack>
  );
}
