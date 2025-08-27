import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import {
  TilsagnBeregningDto,
  TilsagnBeregningFastSatsPerTiltaksplassPerManed,
  TilsagnBeregningFri,
  TilsagnBeregningPrisPerManedsverk,
  TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker,
  TilsagnBeregningPrisPerUkesverk,
} from "@mr/api-client-v2";
import { formaterNOK, formaterTall } from "@mr/frontend-common/utils/utils";
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
    case "PRIS_PER_TIME_OPPFOLGING":
      return <PrisPerTimeOppfolging beregning={beregning} />;
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
        value={tilsagnTekster.prismodell.sats.label(beregning.type)}
      />
      <MetadataHorisontal
        header={tilsagnTekster.antallPlasser.label}
        value={beregning.antallPlasser}
      />
      <MetadataHorisontal
        header={tilsagnTekster.sats.label(beregning.type)}
        value={formaterNOK(beregning.sats)}
      />
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
      <MetadataHorisontal
        header={tilsagnTekster.sats.label(beregning.type)}
        value={formaterNOK(beregning.sats)}
      />
      <PrisOgBetaingsbetingelser prisbetingelser={beregning.prisbetingelser} />
    </VStack>
  );
}

function PrisPerTimeOppfolging({
  beregning,
}: {
  beregning: TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker;
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
      <MetadataHorisontal
        header={tilsagnTekster.sats.label(beregning.type)}
        value={formaterNOK(beregning.sats)}
      />
      <MetadataHorisontal
        header={tilsagnTekster.antallTimerOppfolgingPerDeltaker.label}
        value={formaterTall(beregning.antallTimerOppfolgingPerDeltaker)}
      />
      <PrisOgBetaingsbetingelser prisbetingelser={beregning.prisbetingelser} />
    </VStack>
  );
}
