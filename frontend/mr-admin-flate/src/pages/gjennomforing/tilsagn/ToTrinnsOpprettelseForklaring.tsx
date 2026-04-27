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
  heading: string;
  opprettelse: TotrinnskontrollDto;
};

export function ToTrinnsOpprettelsesForklaring({ heading, opprettelse }: Props) {
  if (!isBesluttet(opprettelse) || opprettelse.besluttelse !== Besluttelse.AVVIST) {
    return null;
  }

  return (
    <AarsakerOgForklaring
      heading={heading}
      tekster={[
        `${opprettelse.besluttetAv.navn} returnerte den ${formaterDato(
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
