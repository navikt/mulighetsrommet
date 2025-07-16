import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import {
  TilsagnBeregningDto,
  TilsagnBeregningFri,
  TilsagnBeregningPrisPerManedsverk,
  TilsagnBeregningPrisPerUkesverk,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Heading, VStack } from "@navikt/ds-react";
import { isBeregningPrisPerManedsverk } from "@/pages/gjennomforing/tilsagn/tilsagnUtils";

interface Props {
  beregning: TilsagnBeregningDto;
}

export function TilsagnPrismodell({ beregning }: Props) {
  switch (beregning.type) {
    case "FRI":
      return <FriPrismodell beregning={beregning} />;
    case "PRIS_PER_UKESVERK":
    case "PRIS_PER_MANEDSVERK":
      return <PrisPerManedsUkesverkPrismodell beregning={beregning} />;
  }
}

function FriPrismodell({ beregning }: { beregning: TilsagnBeregningFri }) {
  const paragraphs = beregning.prisbetingelser?.split("\n") || [];

  return (
    <VStack gap="4">
      <Heading size="small">Prismodell - Annen avtalt pris</Heading>
      <div>
        {paragraphs.map((i: string) => (
          <p key={i}>{i}</p>
        ))}
        {paragraphs.length === 0 && "-"}
      </div>
    </VStack>
  );
}

function PrisPerManedsUkesverkPrismodell({
  beregning,
}: {
  beregning: TilsagnBeregningPrisPerManedsverk | TilsagnBeregningPrisPerUkesverk;
}) {
  return (
    <VStack gap="4">
      <Heading size="small">
        Prismodell - Pris per {isBeregningPrisPerManedsverk(beregning) ? "månedsverk" : "ukesverk"}
      </Heading>
      <VStack gap="4">
        <MetadataHorisontal
          header={tilsagnTekster.antallPlasser.label}
          verdi={beregning.antallPlasser}
        />
        <MetadataHorisontal
          header={tilsagnTekster.sats.label}
          verdi={formaterNOK(beregning.sats)}
        />
      </VStack>
    </VStack>
  );
}
