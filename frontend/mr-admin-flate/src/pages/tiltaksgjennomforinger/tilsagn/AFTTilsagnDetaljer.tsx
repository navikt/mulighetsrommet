import { TilsagnBeregningAft, TilsagnDto } from "@mr/api-client";
import { formaterNOK } from "@mr/frontend-common/utils/utils";
import { Heading, HStack } from "@navikt/ds-react";
import { Bolk } from "@/components/detaljside/Bolk";
import { Metadata } from "@/components/detaljside/Metadata";
import { formaterDato } from "@/utils/Utils";
import { DetaljerInfoContainer } from "../../DetaljerInfoContainer";
import { TilsagnTag } from "./TilsagnTag";

interface Props {
  tilsagn: TilsagnDto;
}

export function AFTTilsagnDetaljer({ tilsagn }: Props) {
  if (tilsagn.beregning.type !== "AFT") {
    throw new Error("Forventet AFT-tilsagn");
  }

  const beregning: TilsagnBeregningAft = tilsagn.beregning;

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
            <Metadata
              header="Tilsagnsstatus"
              verdi={<TilsagnTag expandable status={tilsagn.status} />}
            />
          </Bolk>
          <Bolk>
            <Metadata header="Antall plasser" verdi={beregning.input.antallPlasser} />
            <Metadata header="Sats per plass per måned" verdi={formaterNOK(beregning.input.sats)} />
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
            <Metadata header="Totalbeløp" verdi={formaterNOK(tilsagn.beregning.output.belop)} />
          </Bolk>
        </DetaljerInfoContainer>
      </HStack>
    </>
  );
}
