import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { TilsagnBeregningTable } from "@/components/tilsagn/prismodell/TilsagnBeregningTable";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";
import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  TilsagnBeregningFri,
  TilsagnDto,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TotrinnskontrollDto,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ExpansionCard, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import {
  isBeregningFri,
  isBeregningPrisPerManedsverk,
} from "@/pages/gjennomforing/tilsagn/tilsagnUtils";

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
      <HStack
        gap={{ xs: "8", lg: "14", xl: "16" }}
        align="start"
        justify={{ sm: "space-between", lg: "start" }}
      >
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
              {isBeregningPrisPerManedsverk(beregning) && (
                <MetadataHorisontal
                  header={tilsagnTekster.antallPlasser.label}
                  verdi={beregning.input.antallPlasser}
                />
              )}
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
              {isBeregningPrisPerManedsverk(beregning) && (
                <MetadataHorisontal
                  header={tilsagnTekster.sats.label}
                  verdi={formaterNOK(beregning.input.sats)}
                />
              )}
              {isBeregningFri(beregning) && !beregning.input.prisbetingelser && (
                <MetadataHorisontal
                  header={tilsagnTekster.beregning.prisbetingelser.label}
                  verdi="-"
                />
              )}
            </VStack>
          </HStack>
          {isBeregningFri(beregning) && beregning.input.prisbetingelser && (
            <PrisbetingelserFriModell
              id={tilsagn.id}
              tilsagnStatus={tilsagn.status}
              prisbetingelser={beregning.input.prisbetingelser}
            />
          )}
        </VStack>
        <VStack gap="6" className=" lg:border-l-1 border-gray-300 lg:px-4 flex-1">
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
          {isBeregningFri(beregning) && (
            <BeregningFriModell id={tilsagn.id} status={tilsagn.status} beregning={beregning} />
          )}
        </VStack>
      </HStack>
    </>
  );
}

interface PrisbetingelserFriModellProps {
  id: string;
  tilsagnStatus: TilsagnStatus;
  prisbetingelser: string | null;
}

function PrisbetingelserFriModell({
  id,
  tilsagnStatus,
  prisbetingelser,
}: PrisbetingelserFriModellProps) {
  const paragraphs = prisbetingelser?.split("\n") || [];
  const startOpenForStatus = [TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT].includes(
    tilsagnStatus,
  );
  const style = startOpenForStatus ? { backgroundColor: "var(--a-surface-warning-subtle)" } : {};

  return (
    <ExpansionCard
      key={id}
      size="small"
      style={style}
      aria-label={tilsagnTekster.beregning.prisbetingelser.label}
      defaultOpen={startOpenForStatus}
      className="flex-1"
    >
      <ExpansionCard.Header>
        <ExpansionCard.Title size="small">
          {tilsagnTekster.beregning.prisbetingelser.label}
        </ExpansionCard.Title>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        {paragraphs.map((i) => (
          <p key={i}>{i}</p>
        ))}
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

interface BeregningFriModellProps {
  id: string;
  status: TilsagnStatus;
  beregning: TilsagnBeregningFri;
}

function BeregningFriModell({ id, status, beregning }: BeregningFriModellProps) {
  const startOpenForStatus = [TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT].includes(
    status,
  );

  return (
    <ExpansionCard
      key={id}
      size="small"
      aria-label={tilsagnTekster.beregning.input.label}
      defaultOpen={startOpenForStatus}
    >
      <ExpansionCard.Header>
        <ExpansionCard.Title size="small">
          {tilsagnTekster.beregning.input.label}
        </ExpansionCard.Title>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <TilsagnBeregningTable linjer={beregning.input.linjer} />
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}
