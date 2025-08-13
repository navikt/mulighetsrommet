import { MetadataHorisontal, Separator } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
import {
  TilsagnDto,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TotrinnskontrollDto,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Heading, HStack, Spacer, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { TilsagnTag } from "@/components/tilsagn/TilsagnTag";
import { TilsagnBeregning } from "@/components/tilsagn/beregning/TilsagnBeregning";
import { TilsagnPrismodell } from "@/components/tilsagn/prismodell/TilsagnPrismodell";
import { formaterPeriodeSlutt, formaterPeriodeStart } from "@mr/frontend-common/utils/date";

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
            <VStack gap="4" className="flex-1">
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
            <VStack gap="4" className="flex-1">
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
          <Separator />
          <TilsagnPrismodell beregning={beregning} />
        </VStack>
        <VStack gap="4" className="lg:px-4 flex-1">
          <MetadataHorisontal
            header={tilsagnTekster.status.label}
            verdi={<TilsagnTag status={status} />}
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
            verdi={formaterNOK(beregning.belop)}
          />
          <MetadataHorisontal
            header={tilsagnTekster.belopBrukt.label}
            verdi={formaterNOK(tilsagn.belopBrukt)}
          />
          <MetadataHorisontal
            header={tilsagnTekster.belopGjenstaende.label}
            verdi={formaterNOK(tilsagn.belopGjenstaende)}
          />
          <Separator />
          <VStack gap="2">
            <Heading size="small">Beregning</Heading>
            <TilsagnBeregning beregning={beregning} />
          </VStack>
        </VStack>
      </TwoColumnGrid>
    </>
  );
}
