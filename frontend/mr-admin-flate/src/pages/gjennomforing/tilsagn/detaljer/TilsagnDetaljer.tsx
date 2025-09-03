import {
  MetadataFritekstfelt,
  MetadataHorisontal,
  Separator,
} from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import {
  TilsagnBeregningDto,
  TilsagnDto,
  TilsagnStatus,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { TilsagnStatusAarsak } from "@tiltaksadministrasjon/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Box, Heading, HGrid, HStack, Spacer, VStack } from "@navikt/ds-react";
import { ReactNode } from "react";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { TilsagnTag } from "@/components/tilsagn/TilsagnTag";
import { TilsagnRegnestykke } from "@/components/tilsagn/beregning/TilsagnRegnestykke";
import { formaterPeriode } from "@mr/frontend-common/utils/date";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
import { DataDetails } from "@/components/data-element/DataDetails";

interface Props {
  tilsagn: TilsagnDto;
  beregning: TilsagnBeregningDto;
  opprettelse: TotrinnskontrollDto;
  annullering: TotrinnskontrollDto | null;
  oppgjor: TotrinnskontrollDto | null;
  meny?: ReactNode;
}

export function TilsagnDetaljer({ tilsagn, beregning, meny, annullering, oppgjor }: Props) {
  const { bestillingsnummer, status, periode, type, kostnadssted, kommentar } = tilsagn;

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
        <HGrid columns={1} gap="2">
          <HGrid columns={{ xl: 2 }} gap="4">
            <VStack gap="4" className="flex-1">
              <MetadataHorisontal
                header={tilsagnTekster.bestillingsnummer.label}
                value={bestillingsnummer}
              />
              <MetadataHorisontal
                header={tilsagnTekster.kostnadssted.label}
                value={`${kostnadssted.enhetsnummer} ${kostnadssted.navn}`}
              />
              <MetadataHorisontal
                header={tilsagnTekster.periode.label}
                value={formaterPeriode(periode)}
              />
            </VStack>
            <VStack gap="4" className="flex-1">
              <MetadataHorisontal
                header={tilsagnTekster.status.label}
                value={<TilsagnTag status={status} />}
              />
              <MetadataHorisontal
                header={tilsagnTekster.type.label}
                value={avtaletekster.tilsagn.type(type)}
              />
            </VStack>
            <MetadataFritekstfelt header={tilsagnTekster.kommentar.label} value={kommentar} />
          </HGrid>
          <Separator />
          <DataDetails {...beregning.prismodell} />
        </HGrid>
        <HGrid columns={1} gap="2" align="center">
          <VStack gap="4">
            <MetadataHorisontal
              header={tilsagnTekster.beregning.belop.label}
              value={formaterNOK(beregning.belop)}
            />
            <MetadataHorisontal
              header={tilsagnTekster.belopBrukt.label}
              value={formaterNOK(tilsagn.belopBrukt)}
            />
            <MetadataHorisontal
              header={tilsagnTekster.belopGjenstaende.label}
              value={formaterNOK(tilsagn.belopGjenstaende)}
            />
          </VStack>
          <Separator />
          <Box>
            <Heading size="small" spacing>
              Beregning
            </Heading>
            <TilsagnRegnestykke regnestykke={beregning.regnestykke} />
          </Box>
          {(status.type === TilsagnStatus.ANNULLERT || status.type === TilsagnStatus.OPPGJORT) && (
            <>
              <Separator />
              <Heading level="4" spacing size="small">
                {status.type === TilsagnStatus.ANNULLERT
                  ? "Begrunnelse for annullering"
                  : "Begrunnelse for oppgjør"}
              </Heading>
              <MetadataHorisontal
                header={"Årsaker"}
                value={arsaker
                  ?.map((arsak) => tilsagnAarsakTilTekst(arsak as TilsagnStatusAarsak))
                  .join(", ")}
              />
              <MetadataFritekstfelt header={"Forklaring"} value={oppgjor?.forklaring} />
            </>
          )}
        </HGrid>
      </TwoColumnGrid>
    </>
  );
}
