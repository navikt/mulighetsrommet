import { Besluttelse, TilsagnAvvisningAarsak, TotrinnskontrollDto } from "@mr/api-client-v2";
import { AarsakerOgForklaring } from "./AarsakerOgForklaring";
import { navnEllerIdent, tilsagnAarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";

type Props = {
  opprettelse: TotrinnskontrollDto;
};

export function ToTrinnsOpprettelsesForklaring({ opprettelse }: Props) {
  if (opprettelse.type !== "BESLUTTET" || opprettelse.besluttelse !== Besluttelse.AVVIST) {
    return null;
  }
  return (
    <AarsakerOgForklaring
      heading="Tilsagnet ble returnert"
      tekster={[
        `${navnEllerIdent(opprettelse.besluttetAv)} returnerte tilsagnet den ${formaterDato(
          opprettelse.besluttetTidspunkt,
        )}.`,
      ]}
      aarsaker={
        opprettelse.aarsaker?.map((aarsak) =>
          tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak),
        ) ?? []
      }
      forklaring={opprettelse.forklaring}
    />
  );
}
