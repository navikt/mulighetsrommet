import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata } from "@/components/detaljside/Metadata";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { TilsagnTag } from "@/pages/gjennomforing/tilsagn/TilsagnTag";
import { formaterDato } from "@/utils/Utils";
import { TilsagnBeregningForhandsgodkjent, TilsagnDto } from "@mr/api-client-v2";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Heading, VStack } from "@navikt/ds-react";
import { avtaletekster } from "../../../../components/ledetekster/avtaleLedetekster";

interface Props {
  tilsagn: TilsagnDto & { beregning: TilsagnBeregningForhandsgodkjent };
}

export function TilsagnDetaljerForhandsgodkjent({ tilsagn }: Props) {
  return (
    <>
      <Heading size="medium" level="3">
        Tilsagn
      </Heading>
      <TwoColumnGrid separator>
        <VStack>
          <Bolk>
            <Metadata header="Tilsagnstype" verdi={avtaletekster.tilsagn.type(tilsagn.type)} />
          </Bolk>
          <Bolk>
            <Metadata header="Dato fra" verdi={formaterDato(tilsagn.periodeStart)} />
            <Metadata header="Dato til" verdi={formaterDato(tilsagn.periodeSlutt)} />
            <Metadata
              header="Tilsagnsstatus"
              verdi={<TilsagnTag expandable status={tilsagn.status} />}
            />
          </Bolk>
          <Bolk>
            <Metadata header="Antall plasser" verdi={tilsagn.beregning.input.antallPlasser} />
            <Metadata
              header="Sats per plass per måned"
              verdi={formaterNOK(tilsagn.beregning.input.sats)}
            />
          </Bolk>
          <Bolk>
            <Metadata
              header="Kostnadssted"
              verdi={`${tilsagn.kostnadssted.enhetsnummer} ${tilsagn.kostnadssted.navn}`}
            />
          </Bolk>
        </VStack>
        <VStack>
          <Heading size="small" level="4">
            Beløp
          </Heading>
          <Bolk>
            <Metadata header="Totalbeløp" verdi={formaterNOK(tilsagn.beregning.output.belop)} />
          </Bolk>
        </VStack>
      </TwoColumnGrid>
    </>
  );
}
