import {
  TilsagnStatusAarsak,
  TilskuddBehandlingStatusAarsak,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { AarsakerOgForklaring } from "@/components/totrinnskontroll/AarsakerOgForklaring";
import { aarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { erBesluttet, erReturnert } from "@/utils/totrinnskontroll";

type Props = {
  heading: string;
  opprettelse: TotrinnskontrollDto;
};

export function TotrinnskontrollReturnert({ heading, opprettelse }: Props) {
  if (!erBesluttet(opprettelse) || !erReturnert(opprettelse)) {
    return null;
  }

  return (
    <AarsakerOgForklaring
      heading={heading}
      tekster={[
        `${opprettelse.besluttetAv.navn} returnerte den ${formaterDato(opprettelse.besluttetTidspunkt)}.`,
      ]}
      aarsaker={opprettelse.besluttetAarsaker.map((aarsak) =>
        aarsakTilTekst(aarsak as TilsagnStatusAarsak | TilskuddBehandlingStatusAarsak),
      )}
      forklaring={opprettelse.besluttetBegrunnelse}
    />
  );
}
