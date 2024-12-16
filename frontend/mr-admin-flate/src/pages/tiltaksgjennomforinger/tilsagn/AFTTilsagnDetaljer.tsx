import { TilsagnDto } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { HStack, Heading } from "@navikt/ds-react";
import { useFindAFTSatsForPeriode } from "../../../api/tilsagn/useFindAFTSatsForPeriode";
import { Bolk } from "../../../components/detaljside/Bolk";
import { Metadata } from "../../../components/detaljside/Metadata";
import { formaterDato } from "../../../utils/Utils";
import { DetaljerInfoContainer } from "../../DetaljerInfoContainer";
import { TilsagnTag } from "./TilsagnTag";
import { isAftBeregning } from "./tilsagnUtils";

interface Props {
  tilsagn: TilsagnDto;
}
export function AFTTilsagnDetaljer({ tilsagn }: Props) {
  const { findSats } = useFindAFTSatsForPeriode();

  const sats = findSats(new Date(tilsagn.periodeStart));

  return (
    <>
      <HStack justify={"space-between"} align={"baseline"} padding={"5"}>
        <DetaljerInfoContainer>
          <Heading size="medium" level="3">
            Tilsagn
          </Heading>
        </DetaljerInfoContainer>
      </HStack>
      <HStack padding="5">
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
