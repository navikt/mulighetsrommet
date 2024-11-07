import {
  isTiltakGruppe,
  usePreviewArbeidsmarkedstiltakById,
} from "@/api/queries/useArbeidsmarkedstiltakById";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { ViewTiltakDetaljer } from "@/layouts/ViewTiltakDetaljer";
import { Alert } from "@navikt/ds-react";
import { Innsatsgruppe, NavEnhetStatus, NavEnhetType } from "@mr/api-client";
import { InlineErrorBoundary } from "@/ErrorBoundary";
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
              bruker={{
                innsatsgruppe: Innsatsgruppe.VARIG_TILPASSET_INNSATS,
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
                    status: NavEnhetStatus.AKTIV,
                  },
                ],
              }}
            />
            {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet ? (
              <InlineErrorBoundary>
                <PersonvernContainer tiltak={tiltak} />
              </InlineErrorBoundary>
            ) : null}
            <LenkeListe lenker={tiltak.faneinnhold?.lenker} />
          </>
        }
      />
    </>
  );
}
