import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata } from "@/components/detaljside/Metadata";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";
import { formaterPeriodeSlutt, formaterPeriodeStart } from "@/utils/Utils";
import { TilsagnDto } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Heading, VStack } from "@navikt/ds-react";
import { avtaletekster } from "@/components/ledetekster/avtaleLedetekster";
import { tilsagnTekster } from "@/components/tilsagn/TilsagnTekster";

interface Props {
  tilsagn: TilsagnDto;
}

export function TilsagnDetaljerFri({ tilsagn }: Props) {
  return (
    <>
      <Heading size="medium" level="3">
        Tilsagn
      </Heading>
      <TwoColumnGrid separator>
        <VStack>
          <Bolk>
            <Metadata
              header={tilsagnTekster.type.label}
              verdi={avtaletekster.tilsagn.type(tilsagn.type)}
            />
          </Bolk>
          <Bolk>
            <Metadata
              header={tilsagnTekster.periode.start.label}
              verdi={formaterPeriodeStart(tilsagn.periode)}
            />
            <Metadata
              header={tilsagnTekster.periode.slutt.label}
              verdi={formaterPeriodeSlutt(tilsagn.periode)}
            />
            <Metadata
              header={tilsagnTekster.status.label}
              verdi={<TilsagnTag expandable status={tilsagn.status} />}
            />
          </Bolk>
          <Bolk>
            <Metadata
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
            <Metadata
              header={tilsagnTekster.beregning.belop.label}
              verdi={formaterNOK(tilsagn.beregning.output.belop)}
            />
            <Metadata
              header={tilsagnTekster.gjenstaendeBelop.label}
              verdi={formaterNOK(tilsagn.gjenstaendeBelop)}
            />
          </Bolk>
        </VStack>
      </TwoColumnGrid>
    </>
  );
}
