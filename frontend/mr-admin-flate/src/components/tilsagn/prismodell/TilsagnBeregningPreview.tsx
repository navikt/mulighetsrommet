import { TilsagnBeregningInput, TilsagnBeregningOutput, ValidationError } from "@mr/api-client-v2";
import { formaterNOK, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Heading, Label } from "@navikt/ds-react";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { useEffect, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";

interface Props {
  input: TilsagnBeregningInput;
  onTilsagnBeregnet?: (output: TilsagnBeregningOutput) => void;
}

export function TilsagnBeregningPreview(props: Props) {
  const { input, onTilsagnBeregnet } = props;
  const { mutate: beregnTilsagn } = useBeregnTilsagn();

  const [beregning, setBeregning] = useState<TilsagnBeregningOutput | null>(null);

  const { setError } = useFormContext<DeepPartial<InferredTilsagn>>();

  function onSuccess(beregning: { data: TilsagnBeregningOutput }) {
    setBeregning(beregning.data);
    onTilsagnBeregnet?.(beregning.data);
  }

  function onValidationError(error: ValidationError) {
    error.errors.forEach((error: { pointer: string; detail: string }) => {
      const name = jsonPointerToFieldPath(error.pointer) as keyof InferredTilsagn;
      setError(name, { type: "custom", message: error.detail });
    });
  }

  useEffect(() => {
    beregnTilsagn(input, {
      onSuccess,
      onError: (error) => onValidationError(error as ValidationError),
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [beregnTilsagn, input]);

  return (
    <>
      <Heading size="small">Beløp</Heading>
      <div className="flex justify-between">
        <Label size="medium">Totalbeløp</Label>
        {beregning?.belop && <Label size="medium">{formaterNOK(beregning.belop)}</Label>}
      </div>
    </>
  );
}

// function extractRelevantDeps(obj: any): any[] {
//   if (!obj || typeof obj !== "object") return [obj];

//   return Object.entries(obj).flatMap(([, value]) => {
//     if (typeof value === "object" && value !== null) {
//       return extractRelevantDeps(value); // Recursively flatten nested objects
//     }
//     return value; // Keep primitive values
//   });
// }
