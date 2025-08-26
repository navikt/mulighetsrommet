import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import {
  TilsagnBeregningDto,
  TilsagnBeregningFastSatsPerTiltaksplassPerManed,
  TilsagnBeregningFri,
  TilsagnBeregningPrisPerManedsverk,
  TilsagnBeregningPrisPerUkesverk,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { VStack } from "@navikt/ds-react";
import { PrisOgBetaingsbetingelser } from "@/components/detaljside/PrisOgBetaingsbetingelser";

interface Props {
  beregning: TilsagnBeregningDto;
}

export function TilsagnPrismodell({ beregning }: Props) {
  switch (beregning.type) {
    case "FRI":
      return <FriPrismodell beregning={beregning} />;
    case "FAST_SATS_PER_TILTAKSPLASS_PER_MANED":
      return <FastSatsPerTiltaksplassPerManedPrismodell beregning={beregning} />;
    case "PRIS_PER_UKESVERK":
    case "PRIS_PER_MANEDSVERK":
      return <PrisPerManedsUkesverkPrismodell beregning={beregning} />;
  }
}

function FriPrismodell({ beregning }: { beregning: TilsagnBeregningFri }) {
  return (
    <VStack gap="4">
      <MetadataHorisontal
        header={tilsagnTekster.prismodell.label}
        value={tilsagnTekster.prismodell.sats.label(beregning.type)}
      />
      <PrisOgBetaingsbetingelser prisbetingelser={beregning.prisbetingelser} />
    </VStack>
  );
}

function FastSatsPerTiltaksplassPerManedPrismodell({
  beregning,
}: {
  beregning: TilsagnBeregningFastSatsPerTiltaksplassPerManed;
}) {
  return (
    <VStack gap="4">
      <MetadataHorisontal
        header={tilsagnTekster.prismodell.label}
        verdi={tilsagnTekster.prismodell.sats.label(beregning.type)}
      />
      <MetadataHorisontal
        header={tilsagnTekster.antallPlasser.label}
        verdi={beregning.antallPlasser}
      />
      <MetadataHorisontal header={tilsagnTekster.sats.label} verdi={formaterNOK(beregning.sats)} />
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
      <MetadataHorisontal
        header={tilsagnTekster.prismodell.label}
        value={tilsagnTekster.prismodell.sats.label(beregning.type)}
      />
      <MetadataHorisontal
        header={tilsagnTekster.antallPlasser.label}
        value={beregning.antallPlasser}
      />
      <MetadataHorisontal header={tilsagnTekster.pris.label} value={formaterNOK(beregning.sats)} />
      <PrisOgBetaingsbetingelser prisbetingelser={beregning.prisbetingelser} />
    </VStack>
  );
}
