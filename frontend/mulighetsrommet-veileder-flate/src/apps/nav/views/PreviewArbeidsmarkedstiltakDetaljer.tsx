import {
  isTiltakGruppe,
  usePreviewArbeidsmarkedstiltakById,
} from "@/api/queries/useArbeidsmarkedstiltakById";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltakDetaljer } from "@/layouts/ViewTiltakDetaljer";
import { Alert } from "@navikt/ds-react";
import { Innsatsgruppe, NavEnhetType } from "@api-client";
import { ArbeidsmarkedstiltakErrorBoundary } from "@/ErrorBoundary";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { LenkeListe } from "@/components/sidemeny/Lenker";

export function PreviewArbeidsmarkedstiltakDetaljer() {
  const { data: tiltak } = usePreviewArbeidsmarkedstiltakById();

  return (
    <>
      <Alert style={{ marginBottom: "2rem" }} variant="warning">
        Forhåndsvisning av informasjon
      </Alert>
      <ViewTiltakDetaljer
        tiltak={tiltak}
        knapperad={<Tilbakeknapp tilbakelenke=".." tekst="Gå til oversikt over aktuelle tiltak" />}
        brukerActions={
          <>
            <DelMedBruker
              tiltak={tiltak}
              veiledernavn="{Veiledernavn}"
              veilederEnhet="{Enhet}"
              bruker={{
                innsatsgruppe: Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                fnr: "12345678910",
                fornavn: "{NAVN}",
                manuellStatus: {
                  erUnderManuellOppfolging: false,
                  krrStatus: { kanVarsles: true, erReservert: false },
                },
                erUnderOppfolging: true,
                erSykmeldtMedArbeidsgiver: false,
                varsler: [],
                enheter: [
                  {
                    navn: "{GEOGRAFISK_ENHET}",
                    enhetsnummer: "0",
                    overordnetEnhet: "0100",
                    type: NavEnhetType.LOKAL,
                  },
                ],
              }}
            />
            {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet ? (
              <ArbeidsmarkedstiltakErrorBoundary>
                <PersonvernContainer tiltak={tiltak} />
              </ArbeidsmarkedstiltakErrorBoundary>
            ) : null}
            {tiltak.faneinnhold?.lenker && <LenkeListe lenker={tiltak.faneinnhold.lenker} />}
          </>
        }
      />
    </>
  );
}
