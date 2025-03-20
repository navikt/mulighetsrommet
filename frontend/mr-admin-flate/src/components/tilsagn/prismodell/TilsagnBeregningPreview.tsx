import { ProblemDetail, TilsagnBeregningInput, TilsagnBeregningOutput } from "@mr/api-client-v2";
import { formaterNOK, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Heading, Label } from "@navikt/ds-react";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { useEffect, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredTilsagn } from "@/components/tilsagn/prismodell/TilsagnSchema";
import { isValidationError } from "@/utils/Utils";

interface Props {
  input: TilsagnBeregningInput;
  onTilsagnBeregnet?: (output: TilsagnBeregningOutput) => void;
}

export function TilsagnBeregningPreview(props: Props) {
  const { input, onTilsagnBeregnet } = props;
  const { mutate: beregnTilsagn } = useBeregnTilsagn();

  const [beregning, setBeregning] = useState<TilsagnBeregningOutput | null>(null);

  const { setError } = useFormContext<DeepPartial<InferredTilsagn>>();

  function handleTilsagnBeregnet(beregning: { data: TilsagnBeregningOutput }) {
    setBeregning(beregning.data);
    onTilsagnBeregnet?.(beregning.data);
  }

  function setValidationErrors(error: ProblemDetail) {
    if (isValidationError(error)) {
      error.errors.forEach((error: { pointer: string; detail: string }) => {
        const name = jsonPointerToFieldPath(error.pointer) as keyof InferredTilsagn;
        setError(name, { type: "custom", message: error.detail });
      });
    }
  }

  useEffect(() => {
    beregnTilsagn(input, { onSuccess: handleTilsagnBeregnet, onError: setValidationErrors });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [beregnTilsagn, ...extractRelevantDeps(input)]);

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

function extractRelevantDeps(obj: any): any[] {
  if (!obj || typeof obj !== "object") return [obj];

  return Object.entries(obj).flatMap(([, value]) => {
    if (typeof value === "object" && value !== null) {
      return extractRelevantDeps(value); // Recursively flatten nested objects
    }
    return value; // Keep primitive values
  });
}
