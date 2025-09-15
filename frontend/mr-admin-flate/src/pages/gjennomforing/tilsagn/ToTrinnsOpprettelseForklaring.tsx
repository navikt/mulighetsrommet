import {
  Besluttelse,
  TilsagnStatusAarsak,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { AarsakerOgForklaring } from "./AarsakerOgForklaring";
import { tilsagnAarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { isBesluttet } from "@/utils/totrinnskontroll";

type Props = {
  opprettelse: TotrinnskontrollDto;
};

export function ToTrinnsOpprettelsesForklaring({ opprettelse }: Props) {
  if (!isBesluttet(opprettelse) || opprettelse.besluttelse !== Besluttelse.AVVIST) {
    return null;
  }

  return (
    <AarsakerOgForklaring
      heading="Tilsagnet ble returnert"
      tekster={[
        `${opprettelse.besluttetAv.navn} returnerte tilsagnet den ${formaterDato(
          opprettelse.besluttetTidspunkt,
        )}.`,
      ]}
      aarsaker={opprettelse.aarsaker.map((aarsak) =>
        tilsagnAarsakTilTekst(aarsak as TilsagnStatusAarsak),
      )}
      forklaring={opprettelse.forklaring}
    />
  );
}
