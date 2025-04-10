import { Bolk } from "@/components/detaljside/Bolk";
import { MetadataHorisontal } from "@/components/detaljside/Metadata";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";
import { formaterPeriodeSlutt, formaterPeriodeStart } from "@/utils/Utils";
import {
  TilsagnBeregningForhandsgodkjent,
  TilsagnDto,
  TotrinnskontrollDto,
} from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Heading, VStack } from "@navikt/ds-react";

interface Props {
  tilsagn: TilsagnDto & { beregning: TilsagnBeregningForhandsgodkjent };
  annullering?: TotrinnskontrollDto;
  oppgjor?: TotrinnskontrollDto;
}

export function TilsagnDetaljerForhandsgodkjent({ tilsagn, annullering, oppgjor }: Props) {
  return (
    <>
      <Heading size="medium" level="3">
        Tilsagn
      </Heading>
      <TwoColumnGrid separator>
        <VStack>
          <Bolk>
            <MetadataHorisontal
              header={tilsagnTekster.bestillingsnummer.label}
              verdi={tilsagn.bestillingsnummer}
            />
            <MetadataHorisontal
              header={tilsagnTekster.type.label}
              verdi={avtaletekster.tilsagn.type(tilsagn.type)}
            />
          </Bolk>
          <Bolk>
            <MetadataHorisontal
              header={tilsagnTekster.periode.start.label}
              verdi={formaterPeriodeStart(tilsagn.periode)}
            />
            <MetadataHorisontal
              header={tilsagnTekster.periode.slutt.label}
              verdi={formaterPeriodeSlutt(tilsagn.periode)}
            />
            <MetadataHorisontal
              header={tilsagnTekster.status.label}
              verdi={
                <TilsagnTag
                  visAarsakerOgForklaring
                  status={tilsagn.status}
                  annullering={annullering}
                  oppgjor={oppgjor}
                />
              }
            />
          </Bolk>
          <Bolk>
            <MetadataHorisontal
              header={tilsagnTekster.antallPlasser.label}
              verdi={tilsagn.beregning.input.antallPlasser}
            />
            <MetadataHorisontal
              header={tilsagnTekster.sats.label}
              verdi={formaterNOK(tilsagn.beregning.input.sats)}
            />
          </Bolk>
          <Bolk>
            <MetadataHorisontal
              header={tilsagnTekster.kostnadssted.label}
              verdi={`${tilsagn.kostnadssted.enhetsnummer} ${tilsagn.kostnadssted.navn}`}
            />
          </Bolk>
        </VStack>
        <VStack>
          <Heading size="small" level="4">
            Beløp
          </Heading>
          <Bolk>
            <MetadataHorisontal
              header={tilsagnTekster.beregning.belop.label}
              verdi={formaterNOK(tilsagn.beregning.output.belop)}
            />
            <MetadataHorisontal
              header={tilsagnTekster.belopGjenstaende.label}
              verdi={formaterNOK(tilsagn.belopGjenstaende)}
            />
          </Bolk>
        </VStack>
      </TwoColumnGrid>
    </>
  );
}
