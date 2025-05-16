import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";
import { formaterPeriodeSlutt, formaterPeriodeStart, tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  TilsagnDto,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TotrinnskontrollDto,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { ExpansionCard, Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";

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
        gap={{ lg: "14", xl: "32" }}
        align="start"
        justify={{ sm: "space-between", lg: "start" }}
      >
        <HStack gap={{ sm: "24", lg: "8", xl: "32" }} className="mb-6 lg:m-0">
          <VStack gap="6">
            <MetadataHorisontal
              header={tilsagnTekster.bestillingsnummer.label}
              verdi={bestillingsnummer}
            />
            <MetadataHorisontal
              header={tilsagnTekster.periode.start.label}
              verdi={formaterPeriodeStart(periode)}
            />
            {beregning.type === "FORHANDSGODKJENT" && (
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
          <VStack gap="6">
            <MetadataHorisontal
              header={tilsagnTekster.type.label}
              verdi={avtaletekster.tilsagn.type(type)}
            />
            <MetadataHorisontal
              header={tilsagnTekster.periode.slutt.label}
              verdi={formaterPeriodeSlutt(periode)}
            />
            {beregning.type === "FORHANDSGODKJENT" && (
              <MetadataHorisontal
                header={tilsagnTekster.sats.label}
                verdi={formaterNOK(beregning.input.sats)}
              />
            )}
            {beregning.type === "FRI" && !beregning.input.prisbetingelser && (
              <MetadataHorisontal
                header={tilsagnTekster.beregning.prisbetingelser.label}
                verdi="-"
              />
            )}
          </VStack>
        </HStack>
        <VStack gap="6" justify="start" className=" lg:border-l-1 border-gray-300 lg:px-4">
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
            header={tilsagnTekster.belopGjenstaende.label}
            verdi={formaterNOK(tilsagn.belopGjenstaende)}
          />
        </VStack>
      </HStack>
      {beregning.type === "FRI" && beregning.input.prisbetingelser && (
        <PrisbetingelserFriModell
          tilsagnStatus={tilsagn.status}
          prisbetingelser={beregning.input.prisbetingelser}
        />
      )}
    </>
  );
}

interface PrisbetingelserFriModellProps {
  tilsagnStatus: TilsagnStatus;
  prisbetingelser: string | null;
}

function PrisbetingelserFriModell({
  tilsagnStatus,
  prisbetingelser,
}: PrisbetingelserFriModellProps) {
  const paragraphs = prisbetingelser?.split("\n") || [];
  const startOpenForStatus = [TilsagnStatus.TIL_GODKJENNING, TilsagnStatus.RETURNERT].includes(
    tilsagnStatus,
  );
  const style = startOpenForStatus ? { backgroundColor: "var(--a-surface-warning-subtle)" } : {};

  return (
    <div className="mt-8 mb-4">
      <ExpansionCard
        size="small"
        style={style}
        aria-label={tilsagnTekster.beregning.prisbetingelser.label}
        defaultOpen={startOpenForStatus}
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
    </div>
  );
}
