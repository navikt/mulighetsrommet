import { TilsagnBeregningPrisPerManedsverk } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BodyShort, HStack } from "@navikt/ds-react";

interface Props {
  beregning: TilsagnBeregningPrisPerManedsverk;
}

export default function PrisPerManedsverkBeregning({ beregning }: Props) {
  return (
    <HStack align="center" gap="2">
      <BodyShort>
        {beregning.antallPlasser} plasser × {formaterNOK(beregning.sats)} ×{" "}
        {beregning.antallManeder} måneder = {formaterNOK(beregning.belop)}
      </BodyShort>
    </HStack>
  );
}
