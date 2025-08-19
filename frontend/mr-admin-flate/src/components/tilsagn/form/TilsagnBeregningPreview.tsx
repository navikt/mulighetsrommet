import {
  ProblemDetail,
  TilsagnBeregningDto,
  TilsagnBeregningInput,
  ValidationError,
} from "@mr/api-client-v2";
import { formaterNOK, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { HStack, Label, VStack } from "@navikt/ds-react";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { useEffect, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredTilsagn, TilsagnBeregningSchema } from "./TilsagnSchema";
import { TilsagnBeregning } from "../beregning/TilsagnBeregning";

interface Props {
  input: TilsagnBeregningInput;
  onTilsagnBeregnet?: (output: TilsagnBeregningDto) => void;
}

export function TilsagnBeregningPreview(props: Props) {
  const { input, onTilsagnBeregnet } = props;
  const { mutate: beregnTilsagn } = useBeregnTilsagn();

  const [beregning, setBeregning] = useState<TilsagnBeregningDto | null>(null);

  const { setError } = useFormContext<DeepPartial<InferredTilsagn>>();

  function onSuccess(beregning: { data: TilsagnBeregningDto }) {
    setBeregning(beregning.data);
    onTilsagnBeregnet?.(beregning.data);
  }

  function onValidationError(error: ValidationError) {
    setBeregning(null);

    error.errors.forEach((error: { pointer: string; detail: string }) => {
      const name = jsonPointerToFieldPath(error.pointer) as keyof InferredTilsagn;
      setError(name, { type: "custom", message: error.detail });
    });
  }

  useEffect(() => {
    if (TilsagnBeregningSchema.safeParse(input).success) {
      beregnTilsagn(
        {
          ...input,
          prisbetingelser: input.prisbetingelser ?? null,
        },
        {
          onSuccess,
          onError: (error: ProblemDetail) => onValidationError(error as ValidationError),
        },
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [beregnTilsagn, flattendDepsToString(input)]);

  return (
    <>
      <VStack gap="4">
        <HStack gap="2" justify="space-between">
          <Label size="medium">Totalbeløp</Label>
          {beregning?.belop && <Label size="medium">{formaterNOK(beregning.belop)}</Label>}
        </HStack>
        {beregning && <TilsagnBeregning redigeringsModus beregning={beregning} />}
      </VStack>
    </>
  );
}

/**
 * Grunnet dynamisk input-felter for tilsagn med avtalt prismodell,
 * må vi kunne trigge ny beregning ved endringer i dypt nøstet objekter
 */
function flattendDepsToString(obj: any): string {
  if (!obj || typeof obj !== "object") return obj.toString();

  return Object.entries(obj).reduce((acc, curr) => {
    const value = curr[1] as any;
    if (!value) {
      return acc;
    }
    if (typeof value === "object") {
      return acc + flattendDepsToString(value); // Recursively flatten nested objects
    }
    return acc + value.toString(); // Keep primitive values
  }, "");
}
