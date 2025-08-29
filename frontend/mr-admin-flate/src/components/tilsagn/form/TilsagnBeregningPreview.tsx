import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { HStack, Label, Loader, VStack } from "@navikt/ds-react";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { TilsagnBeregning } from "../beregning/TilsagnBeregning";
import { useFormContext } from "react-hook-form";
import { InferredTilsagn } from "./TilsagnSchema";

export function TilsagnBeregningPreview() {
  const { watch } = useFormContext<InferredTilsagn>();
  const values = watch();
  const { data: beregning, isLoading } = useBeregnTilsagn({
    periodeStart: values.periodeStart,
    periodeSlutt: values.periodeSlutt,
    beregning: values.beregning,
    gjennomforingId: values.gjennomforingId,
  });

  if (isLoading) {
    return <Loader />;
  }

  if (!beregning) {
    return null;
  }

  return (
    <>
      <VStack gap="4">
        <HStack gap="2" justify="space-between">
          <Label size="medium">Totalbeløp</Label>
          {beregning.belop && <Label size="medium">{formaterNOK(beregning.belop)}</Label>}
        </HStack>
        <TilsagnBeregning redigeringsModus beregning={beregning} />
      </VStack>
    </>
  );
}
