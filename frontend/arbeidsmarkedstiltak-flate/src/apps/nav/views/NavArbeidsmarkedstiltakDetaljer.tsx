import {
  isTiltakGruppe,
  useNavArbeidsmarkedstiltakById,
} from "@/api/queries/useArbeidsmarkedstiltakById";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltakDetaljer } from "@/layouts/ViewTiltakDetaljer";
import { ArbeidsmarkedstiltakErrorBoundary } from "@/ErrorBoundary";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { SidemenyLenker } from "@/components/sidemeny/SidemenyLenker";

export function NavArbeidsmarkedstiltakDetaljer() {
  const { data: tiltak } = useNavArbeidsmarkedstiltakById();

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
          <SidemenyLenker tiltak={tiltak} skjulKunForVeileder={true} />
        </>
      }
    />
  );
}
