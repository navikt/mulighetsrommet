import {
  isTiltakGruppe,
  useNavArbeidsmarkedstiltakById,
} from "@/api/queries/useArbeidsmarkedstiltakById";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltaksgjennomforingDetaljer } from "@/layouts/ViewTiltaksgjennomforingDetaljer";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { LenkeListe } from "@/components/sidemeny/Lenker";

export function NavArbeidsmarkedstiltakDetaljer() {
  const { data: tiltak } = useNavArbeidsmarkedstiltakById();

  return (
    <ViewTiltaksgjennomforingDetaljer
      tiltak={tiltak}
      knapperad={<Tilbakeknapp tilbakelenke=".." tekst="Gå til oversikt over aktuelle tiltak" />}
      brukerActions={
        <>
          {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet ? (
            <InlineErrorBoundary>
              <PersonvernContainer tiltak={tiltak} />
            </InlineErrorBoundary>
          ) : null}
          <LenkeListe
            lenker={tiltak.faneinnhold?.lenker?.filter((lenke) => !lenke.visKunForVeileder)}
          />
        </>
      }
    />
  );
}
