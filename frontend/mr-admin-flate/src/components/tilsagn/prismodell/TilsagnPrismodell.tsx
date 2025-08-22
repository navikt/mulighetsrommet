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
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { Definisjonsliste } from "@mr/frontend-common/components/definisjonsliste/Definisjonsliste";
import Prisbetingelser from "@/components/utbetaling/Prisbetingelser";

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
        verdi={tilsagnTekster.prismodell.sats.label(beregning.type)}
      />
      <Definisjonsliste
        columns={1}
        definitions={[
          {
            key: avtaletekster.prisOgBetalingLabel,
            value: beregning.prisbetingelser ? (
              <Prisbetingelser value={beregning.prisbetingelser} />
            ) : (
              "-"
            ),
          },
        ]}
      />
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
        verdi={tilsagnTekster.prismodell.sats.label(beregning.type)}
      />
      <Definisjonsliste
        columns={1}
        definitions={[
          {
            key: avtaletekster.prisOgBetalingLabel,
            value: beregning.prisbetingelser ? (
              <Prisbetingelser value={beregning.prisbetingelser} />
            ) : (
              "-"
            ),
          },
        ]}
      />
      <VStack gap="4">
        <MetadataHorisontal
          header={tilsagnTekster.antallPlasser.label}
          verdi={beregning.antallPlasser}
        />
        <MetadataHorisontal
          header={tilsagnTekster.pris.label}
          verdi={formaterNOK(beregning.sats)}
        />
      </VStack>
    </VStack>
  );
}
