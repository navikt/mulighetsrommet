import {
  TilsagnStatusAarsak,
  TilskuddBehandlingStatusAarsak,
  TotrinnskontrollDto,
} from "@tiltaksadministrasjon/api-client";
import { AarsakerOgBegrunnelse } from "@/components/totrinnskontroll/AarsakerOgBegrunnelse";
import { aarsakTilTekst } from "@/utils/Utils";
import { formaterDato } from "@mr/frontend-common/utils/date";
import { erBesluttet, erReturnert, utledBesluttetAvNavn } from "@/utils/totrinnskontroll";

type Props = {
  heading: string;
  opprettelse: TotrinnskontrollDto;
};

export function TotrinnskontrollReturnert({ heading, opprettelse }: Props) {
  if (!erBesluttet(opprettelse) || !erReturnert(opprettelse)) {
    return null;
  }

  return (
    <AarsakerOgBegrunnelse
      heading={heading}
      tekster={[
        `${utledBesluttetAvNavn(opprettelse)} returnerte den ${formaterDato(opprettelse.beslutning.tidspunkt)}.`,
      ]}
      aarsaker={opprettelse.beslutning.aarsaker.map((aarsak) =>
        aarsakTilTekst(aarsak as TilsagnStatusAarsak | TilskuddBehandlingStatusAarsak),
      )}
      begrunnelse={opprettelse.beslutning.begrunnelse}
    />
  );
}
