import {
  isTiltakGruppe,
  useNavArbeidsmarkedstiltakById,
} from "@/api/queries/useArbeidsmarkedstiltakById";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltakDetaljer } from "@/layouts/ViewTiltakDetaljer";
import { ArbeidsmarkedstiltakErrorBoundary } from "@/ErrorBoundary";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { Lenke, LenkeListe } from "@/components/sidemeny/Lenker";

export function NavArbeidsmarkedstiltakDetaljer() {
  const { data: tiltak } = useNavArbeidsmarkedstiltakById();

  const lenker = tiltak.faneinnhold?.lenker?.filter((lenke) => !lenke.visKunForVeileder);

  return (
    <ViewTiltakDetaljer
      tiltak={tiltak}
      knapperad={<Tilbakeknapp tilbakelenke=".." tekst="Gå til oversikt over aktuelle tiltak" />}
      brukerActions={
        <>
          {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet ? (
            <ArbeidsmarkedstiltakErrorBoundary>
              <PersonvernContainer tiltak={tiltak} />
            </ArbeidsmarkedstiltakErrorBoundary>
          ) : null}
          {/* TODO: fix hacky types */}
          {lenker && <LenkeListe lenker={lenker as unknown as Lenke[]} />}
        </>
      }
    />
  );
}
