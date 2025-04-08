import { Besluttelse, TilsagnAvvisningAarsak, TotrinnskontrollDto } from "@mr/api-client-v2";
import { AarsakerOgForklaring } from "./AarsakerOgForklaring";
import { formaterDato, navnEllerIdent, tilsagnAarsakTilTekst } from "@/utils/Utils";

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
      tekst={`${navnEllerIdent(opprettelse.besluttetAv)} returnerte tilsagnet den ${formaterDato(
        opprettelse.besluttetTidspunkt,
      )} med følgende årsaker:`}
      aarsaker={
        opprettelse.aarsaker?.map((aarsak) =>
          tilsagnAarsakTilTekst(aarsak as TilsagnAvvisningAarsak),
        ) ?? []
      }
      forklaring={opprettelse.forklaring}
    />
  );
}
