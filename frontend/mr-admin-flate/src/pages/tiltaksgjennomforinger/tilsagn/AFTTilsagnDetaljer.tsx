import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { HStack, Heading, VStack } from "@navikt/ds-react";
import { Bolk } from "../../../components/detaljside/Bolk";
import { Metadata } from "../../../components/detaljside/Metadata";
import { formaterDato } from "../../../utils/Utils";
import { TilsagnDto } from "@mr/api-client";
import { useFindAFTSatsForPeriode } from "../../../api/tilsagn/useFindAFTSatsForPeriode";
import { TilsagnTag } from "./TilsagnTag";
import { isAftBeregning } from "./tilsagnUtils";
import { DetaljerInfoContainer } from "../../DetaljerInfoContainer";

interface Props {
  tilsagn: TilsagnDto;
}
export function AFTTilsagnDetaljer({ tilsagn }: Props) {
  const { findSats } = useFindAFTSatsForPeriode();

  const sats = findSats(new Date(tilsagn.periodeStart));

  return (
    <>
      <HStack justify={"space-between"} align={"baseline"} padding={"5"}>
        <Heading size="medium" level="3" spacing>
          Tilsagn
        </Heading>
      </HStack>
      <HStack padding="5">
        <VStack>
          <DetaljerInfoContainer withBorderRight>
            <Heading size="small" level="4">
              Periode og plasser
            </Heading>
            <Bolk>
              <Metadata header="Dato fra" verdi={formaterDato(tilsagn.periodeStart)} />
              <Metadata header="Dato til" verdi={formaterDato(tilsagn.periodeSlutt)} />
              <Metadata header="Tilsagnsstatus" verdi={<TilsagnTag tilsagn={tilsagn} />} />
            </Bolk>
            <Bolk>
              <Metadata
                header="Antall plasser"
                verdi={isAftBeregning(tilsagn.beregning) ? tilsagn.beregning.antallPlasser : 0}
              />
              <Metadata
                header="Sats per plass per måned"
                verdi={sats ? formaterNOK(sats) : "Fant ingen sats per plass per måned"}
              />
            </Bolk>
            <Bolk>
              <Metadata
                header="Kostnadssted"
                verdi={`${tilsagn.kostnadssted.enhetsnummer} ${tilsagn.kostnadssted.navn}`}
              />
            </Bolk>
          </DetaljerInfoContainer>
        </VStack>
        <DetaljerInfoContainer withBorderRight={false}>
          <Heading size="small" level="4">
            Beløp
          </Heading>
          <Bolk>
            <Metadata header="Totalbeløp" verdi={formaterNOK(tilsagn.beregning.belop)} />
          </Bolk>
        </DetaljerInfoContainer>
      </HStack>
    </>
  );
}
