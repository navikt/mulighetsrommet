import {
  ProblemDetail,
  TilsagnBeregningInput,
  TilsagnBeregningOutput,
  ValidationError,
} from "@mr/api-client-v2";
import { formaterNOK, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Heading, HStack, Label, VStack } from "@navikt/ds-react";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { ReactNode, useEffect, useState } from "react";
import { DeepPartial, useFormContext } from "react-hook-form";
import { InferredTilsagn, TilsagnBeregningSchema } from "./TilsagnSchema";

interface Props {
  input: TilsagnBeregningInput;
  onTilsagnBeregnet?: (output: TilsagnBeregningOutput) => void;
  children?: ReactNode | ReactNode[];
}

export function TilsagnBeregningPreview(props: Props) {
  const { input, onTilsagnBeregnet, children } = props;
  const { mutate: beregnTilsagn } = useBeregnTilsagn();

  const [beregning, setBeregning] = useState<TilsagnBeregningOutput | null>(null);

  const { setError } = useFormContext<DeepPartial<InferredTilsagn>>();

  function onSuccess(beregning: { data: TilsagnBeregningOutput }) {
    setBeregning(beregning.data);
    onTilsagnBeregnet?.(beregning.data);
  }

  function onValidationError(error: ValidationError) {
    setBeregning({ type: beregning!.type, belop: 0 });

    error.errors.forEach((error: { pointer: string; detail: string }) => {
      const name = jsonPointerToFieldPath(error.pointer) as keyof InferredTilsagn;
      setError(name, { type: "custom", message: error.detail });
    });
  }

  useEffect(() => {
    if (TilsagnBeregningSchema.safeParse(input).success) {
      beregnTilsagn(input, {
        onSuccess,
        onError: (error: ProblemDetail) => onValidationError(error as ValidationError),
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [beregnTilsagn, flattendDepsToString(input)]);

  return (
    <>
      <Heading size="small">Beløp</Heading>
      <VStack gap="4">
        {children}
        <HStack gap="2" justify="space-between">
          <Label size="medium">Totalbeløp</Label>
          {beregning?.belop && <Label size="medium">{formaterNOK(beregning.belop)}</Label>}
        </HStack>
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
