import { TilsagnBeregningPrisPerUkesverk } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BodyShort, HStack } from "@navikt/ds-react";

interface Props {
  beregning: TilsagnBeregningPrisPerUkesverk;
}

export default function PrisPerUkesverkBeregning({ beregning }: Props) {
  return (
    <HStack align="center" gap="2">
      <BodyShort>
        {beregning.antallPlasser} plasser × {beregning.sats} × {beregning.antallUker} uker ={" "}
        {formaterNOK(beregning.belop)}
      </BodyShort>
    </HStack>
  );
}
