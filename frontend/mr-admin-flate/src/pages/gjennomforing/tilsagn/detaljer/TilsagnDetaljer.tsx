import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  TilsagnBeregning,
  TilsagnBeregningFri,
  TilsagnBeregningPrisPerManedsverk,
  TilsagnBeregningPrisPerUkesverk,
  TilsagnDto,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TotrinnskontrollDto,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { BodyShort, Box, ExpansionCard, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { isBeregningPrisPerManedsverk } from "@/pages/gjennomforing/tilsagn/tilsagnUtils";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { TilsagnTag } from "@/components/tilsagn/TilsagnTag";
import { FriBeregningTable } from "@/components/tilsagn/beregning/FriBeregningTable";

interface Props {
  tilsagn: TilsagnDto;
  opprettelse: TotrinnskontrollDto;
  annullering?: TotrinnskontrollDto;
  oppgjor?: TotrinnskontrollDto;
  meny?: ReactNode;
}

export function TilsagnDetaljer({ tilsagn, meny, annullering, oppgjor }: Props) {
  const { beregning, bestillingsnummer, status, periode, type, kostnadssted } = tilsagn;

  const arsaker = oppgjor?.aarsaker || annullering?.aarsaker;

  return (
    <>
      <HStack className="mb-2">
        <Heading size="medium" level="3">
          Tilsagn
        </Heading>
        <Spacer />
        {meny}
      </HStack>
      <TwoColumnGrid separator>
        <VStack
          gap={{ xs: "6 16", lg: "8", xl: "6 16" }}
          justify="start"
          className="mb-6 lg:m-0 flex-1"
        >
          <HStack gap="6">
            <VStack gap="6" className="flex-1">
              <MetadataHorisontal
                header={tilsagnTekster.bestillingsnummer.label}
                verdi={bestillingsnummer}
              />
              <MetadataHorisontal
                header={tilsagnTekster.periode.start.label}
                verdi={formaterPeriodeStart(periode)}
              />
              <MetadataHorisontal
                header={tilsagnTekster.kostnadssted.label}
                verdi={`${kostnadssted.enhetsnummer} ${kostnadssted.navn}`}
              />
            </VStack>
            <VStack gap="6" className="flex-1">
              <MetadataHorisontal
                header={tilsagnTekster.type.label}
                verdi={avtaletekster.tilsagn.type(type)}
              />
              <MetadataHorisontal
                header={tilsagnTekster.periode.slutt.label}
                verdi={formaterPeriodeSlutt(periode)}
              />
            </VStack>
          </HStack>
          <TilsagnPrismodellCard beregning={beregning} />
        </VStack>
        <VStack gap="6" className="lg:px-4 flex-1">
          <MetadataHorisontal
            header={tilsagnTekster.status.label}
            verdi={<TilsagnTag visAarsakerOgForklaring status={status} />}
          />
          {(status === TilsagnStatus.ANNULLERT || status === TilsagnStatus.OPPGJORT) && (
            <>
              <MetadataHorisontal
                header={"Årsaker"}
                verdi={arsaker
                  ?.map((arsak) => tilsagnAarsakTilTekst(arsak as TilsagnTilAnnulleringAarsak))
                  .join(", ")}
              />
              <MetadataHorisontal
                header={"Forklaring"}
                verdi={annullering?.forklaring ?? oppgjor?.forklaring}
              />
            </>
          )}
          <MetadataHorisontal
            header={tilsagnTekster.beregning.belop.label}
            verdi={formaterNOK(beregning.output.belop)}
          />
          <MetadataHorisontal
            header={tilsagnTekster.belopBrukt.label}
            verdi={formaterNOK(tilsagn.belopBrukt)}
          />
          <MetadataHorisontal
            header={tilsagnTekster.belopGjenstaende.label}
            verdi={formaterNOK(tilsagn.belopGjenstaende)}
          />
          <TilsagnBeregningCard beregning={beregning} />
        </VStack>
      </TwoColumnGrid>
    </>
  );
}

function TilsagnPrismodellCard({ beregning }: { beregning: TilsagnBeregning }) {
  switch (beregning.type) {
    case "FRI":
      return <FriPrismodell beregning={beregning} />;
    case "PRIS_PER_UKESVERK":
    case "PRIS_PER_MANEDSVERK":
      return <PrisPerManedsUkesverkPrismodell beregning={beregning} />;
  }
}

function FriPrismodell({ beregning }: { beregning: TilsagnBeregningFri }) {
  const paragraphs = beregning.input.prisbetingelser?.split("\n") || [];

  return (
    <ExpansionCard size="small" aria-label="Annen avtalt pris" className="flex-1" defaultOpen>
      <ExpansionCard.Header>
        <ExpansionCard.Title size="small">Prismodell - Annen avtalt pris</ExpansionCard.Title>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        {paragraphs.map((i: string) => (
          <p key={i}>{i}</p>
        ))}
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

function PrisPerManedsUkesverkPrismodell({
  beregning,
}: {
  beregning: TilsagnBeregningPrisPerManedsverk | TilsagnBeregningPrisPerUkesverk;
}) {
  return (
    <Box borderColor="border-subtle" padding="4" borderWidth="1" borderRadius="large">
      <VStack gap="4">
        <Heading size="small">
          Prismodell - Pris per{" "}
          {isBeregningPrisPerManedsverk(beregning) ? "månedsverk" : "ukesverk"}
        </Heading>
        <VStack>
          <MetadataHorisontal
            header={tilsagnTekster.antallPlasser.label}
            verdi={beregning.input.antallPlasser}
          />
          <MetadataHorisontal
            header={tilsagnTekster.sats.label}
            verdi={formaterNOK(beregning.input.sats)}
          />
        </VStack>
      </VStack>
    </Box>
  );
}

function TilsagnBeregningCard({ beregning }: { beregning: TilsagnBeregning }) {
  switch (beregning.type) {
    case "FRI":
      return <FriBeregning beregning={beregning} />;
    case "PRIS_PER_UKESVERK":
    case "PRIS_PER_MANEDSVERK":
      return <PrisPerManedsUkesverkBeregning beregning={beregning} />;
  }
}

function FriBeregning({ beregning }: { beregning: TilsagnBeregningFri }) {
  return (
    <ExpansionCard size="small" aria-label={tilsagnTekster.beregning.input.label} defaultOpen>
      <ExpansionCard.Header>
        <ExpansionCard.Title size="small">Beregning</ExpansionCard.Title>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <FriBeregningTable linjer={beregning.input.linjer} />
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

function PrisPerManedsUkesverkBeregning({
  beregning,
}: {
  beregning: TilsagnBeregningPrisPerManedsverk | TilsagnBeregningPrisPerUkesverk;
}) {
  return (
    <Box borderColor="border-subtle" padding="4" borderWidth="1" borderRadius="large">
      <VStack gap="4">
        <Heading size="small">Beregning</Heading>
        <HStack align="center" gap="2">
          <BodyShort>
            antall plasser × sats × antall{" "}
            {isBeregningPrisPerManedsverk(beregning) ? "måneder" : "uker"} ={" "}
            {formaterNOK(beregning.output.belop)}
          </BodyShort>
        </HStack>
      </VStack>
    </Box>
  );
}
