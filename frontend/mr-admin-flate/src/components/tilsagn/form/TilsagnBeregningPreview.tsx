import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { HStack, Label, Loader, VStack } from "@navikt/ds-react";
import { useBeregnTilsagn } from "@/api/tilsagn/useBeregnTilsagn";
import { TilsagnRegnestykke } from "../beregning/TilsagnRegnestykke";
import { useFormContext } from "react-hook-form";
import { TilsagnRequest } from "@tiltaksadministrasjon/api-client";

export function TilsagnBeregningPreview() {
  const { watch } = useFormContext<TilsagnRequest>();
  const values = watch();
  const { data, isLoading } = useBeregnTilsagn({
    periodeStart: values.periodeStart,
    periodeSlutt: values.periodeSlutt,
    beregning: values.beregning,
    gjennomforingId: values.gjennomforingId,
  });

  if (isLoading) {
    return <Loader />;
  }

  if (!data?.success || !data.beregning) {
    return null;
  }

  return (
    <>
      <VStack gap="4">
        <HStack gap="2" justify="space-between">
          <Label size="medium">Totalbeløp</Label>
          {data.beregning.belop && <Label size="medium">{formaterNOK(data.beregning.belop)}</Label>}
        </HStack>
        <TilsagnRegnestykke regnestykke={data.beregning.regnestykke} />
      </VStack>
    </>
  );
}
