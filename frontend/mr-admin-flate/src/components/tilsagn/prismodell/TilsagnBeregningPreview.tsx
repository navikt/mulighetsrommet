import { ApiError, TilsagnBeregningInput, TilsagnBeregningOutput } from "@mr/api-client";
import { formaterNOK, isValidationError } from "@mr/frontend-common/utils/utils";
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

  function handleTilsagnBeregnet(beregning: TilsagnBeregningOutput) {
    setBeregning(beregning);
    onTilsagnBeregnet?.(beregning);
  }

  function setValidationErrors(error: ApiError) {
    if (isValidationError(error.body)) {
      error.body.errors.forEach((error) => {
        const name = error.name as keyof InferredTilsagn;
        setError(name, { type: "custom", message: error.message });
      });
    }
  }

  useEffect(() => {
    beregnTilsagn(input, { onSuccess: handleTilsagnBeregnet, onError: setValidationErrors });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [beregnTilsagn, ...Object.values(input)]);

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
