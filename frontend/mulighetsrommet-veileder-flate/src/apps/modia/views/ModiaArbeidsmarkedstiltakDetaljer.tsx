import {
  isTiltakAktivt,
  isTiltakGruppe,
  useModiaArbeidsmarkedstiltakById,
} from "@/api/queries/useArbeidsmarkedstiltakById";
import { useRegioner } from "@/api/queries/useRegioner";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";
import { useDelMedBrukerStatus } from "@/apps/modia/hooks/useDelMedbrukerStatus";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useVeilederdata } from "@/apps/modia/hooks/useVeilederdata";
import { BrukerKvalifisererIkkeVarsel } from "@/apps/modia/varsler/BrukerKvalifisererIkkeVarsel";
import { DetaljerJoyride } from "@/components/joyride/DetaljerJoyride";
import { OpprettAvtaleJoyride } from "@/components/joyride/OpprettAvtaleJoyride";
import { PameldingForGruppetiltak } from "@/components/pamelding/PameldingForGruppetiltak";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { LenkeListe } from "@/components/sidemeny/Lenker";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { VisibleWhenToggledOn } from "@/components/toggles/VisibleWhenToggledOn";
import {
  PORTEN_URL_FOR_TILBAKEMELDING,
  TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL,
} from "@/constants";
import { paginationAtom } from "@/core/atoms";
import { ArbeidsmarkedstiltakErrorBoundary } from "@/ErrorBoundary";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { ViewTiltakDetaljer } from "@/layouts/ViewTiltakDetaljer";
import {
  Bruker,
  Innsatsgruppe,
  NavVeileder,
  Tiltakskode,
  TiltakskodeArena,
  Toggles,
  VeilederflateTiltakstype,
} from "@mr/api-client-v2";
import { TilbakemeldingsLenke } from "@mr/frontend-common";
import { Chat2Icon } from "@navikt/aksel-icons";
import { Alert, Button } from "@navikt/ds-react";
import { useAtomValue } from "jotai";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import { PameldingKometApnerSnart } from "../pamelding/PameldingKometApnerSnart";

const TEAM_TILTAK_OPPRETT_AVTALE_URL = `${TEAM_TILTAK_TILTAKSGJENNOMFORING_APP_URL}/opprett-avtale`;

export function ModiaArbeidsmarkedstiltakDetaljer() {
  const { fnr } = useModiaContext();
  const id = useTiltakIdFraUrl();
  const { data: delMedBrukerInfo } = useDelMedBrukerStatus(fnr, id);

  const { data: veilederdata } = useVeilederdata();
  const { data: brukerdata } = useBrukerdata();
  const { data: tiltak } = useModiaArbeidsmarkedstiltakById();
  const { data: regioner } = useRegioner();

  const pagination = useAtomValue(paginationAtom);

  const tiltakstype = tiltak.tiltakstype;
  const kanOppretteAvtaleForTiltak =
    isIndividueltTiltak(tiltakstype) && brukerdata.erUnderOppfolging;
  const brukerHarRettPaaValgtTiltak = harBrukerRettPaaValgtTiltak(brukerdata, tiltakstype);

  const dialogRoute = delMedBrukerInfo
    ? resolveModiaRoute({
        route: ModiaRoute.DIALOG,
        dialogId: delMedBrukerInfo.dialogId,
      })
    : null;

  const tiltaksnummer = "tiltaksnummer" in tiltak ? tiltak.tiltaksnummer : undefined;
  const fylke = regioner.find((r) => tiltak.fylker.includes(r.enhetsnummer))?.navn;

  return (
    <>
      <title>{`Arbeidsmarkedstiltak - Detaljer ${tiltak.tiltakstype.navn}`}</title>
      <BrukerKvalifisererIkkeVarsel
        brukerdata={brukerdata}
        brukerHarRettPaaTiltak={brukerHarRettPaaValgtTiltak}
      />
      <ViewTiltakDetaljer
        tiltak={tiltak}
        knapperad={
          <>
            <Tilbakeknapp
              tilbakelenke={`/arbeidsmarkedstiltak/oversikt#pagination=${encodeURIComponent(
                JSON.stringify({ ...pagination }),
              )}`}
              tekst="Gå til oversikt over aktuelle tiltak"
            />
            <div>
              <DetaljerJoyride opprettAvtale={kanOppretteAvtaleForTiltak} />
              {kanOppretteAvtaleForTiltak ? (
                <OpprettAvtaleJoyride opprettAvtale={kanOppretteAvtaleForTiltak} />
              ) : null}
            </div>
          </>
        }
        brukerActions={
          <>
            {kanOppretteAvtaleForTiltak && (
              <Button
                onClick={() => {
                  window.open(TEAM_TILTAK_OPPRETT_AVTALE_URL, "_blank");
                }}
                variant="primary"
                aria-label="Opprett avtale"
                data-testid="opprettavtaleknapp"
                disabled={!brukerHarRettPaaValgtTiltak}
              >
                Opprett avtale
              </Button>
            )}

            {isTiltakAktivt(tiltak) ? <PameldingKometApnerSnart tiltak={tiltak} /> : null}

            {isTiltakGruppe(tiltak) && isTiltakAktivt(tiltak) ? (
              <PameldingForGruppetiltak
                brukerHarRettPaaValgtTiltak={brukerHarRettPaaValgtTiltak}
                tiltak={tiltak}
              />
            ) : null}

            {brukerdata.erUnderOppfolging && isTiltakAktivt(tiltak) ? (
              <DelMedBruker
                delMedBrukerInfo={delMedBrukerInfo ?? undefined}
                veiledernavn={resolveName(veilederdata)}
                tiltak={tiltak}
                bruker={brukerdata}
              />
            ) : null}

            {!brukerdata.manuellStatus && (
              <Alert
                title="Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert seg mot digital kommunikasjon"
                key="alert-innsatsgruppe"
                data-testid="alert-innsatsgruppe"
                size="small"
                variant="error"
              >
                Vi kunne ikke opprette kontakt med KRR og vet derfor ikke om brukeren har reservert
                seg mot digital kommunikasjon
              </Alert>
            )}

            {dialogRoute && brukerdata.erUnderOppfolging && (
              <Button
                className="flex"
                size="small"
                variant="tertiary"
                onClick={dialogRoute.navigate}
              >
                Åpne i dialogen
                <Chat2Icon className="inline-flex" aria-label="Åpne i dialogen" />
              </Button>
            )}

            {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet ? (
              <ArbeidsmarkedstiltakErrorBoundary>
                <PersonvernContainer tiltak={tiltak} />
              </ArbeidsmarkedstiltakErrorBoundary>
            ) : null}

            <LenkeListe lenker={tiltak.faneinnhold?.lenker} />
            <VisibleWhenToggledOn toggle={Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_TILBAKEMELDING}>
              <TilbakemeldingsLenke
                url={PORTEN_URL_FOR_TILBAKEMELDING(tiltaksnummer, fylke)}
                tekst="Gi tilbakemelding via Porten"
              />
            </VisibleWhenToggledOn>
          </>
        }
      />
    </>
  );
}

const whiteListOpprettAvtaleKnapp: TiltakskodeArena[] = [
  TiltakskodeArena.MIDLONTIL,
  TiltakskodeArena.ARBTREN,
  TiltakskodeArena.VARLONTIL,
  TiltakskodeArena.MENTOR,
  TiltakskodeArena.INKLUTILS,
  TiltakskodeArena.TILSJOBB,
];

function resolveName(ansatt: NavVeileder) {
  return [ansatt.fornavn, ansatt.etternavn].filter((part) => part !== "").join(" ");
}

function isIndividueltTiltak(tiltakstype: VeilederflateTiltakstype): boolean {
  return (
    tiltakstype.arenakode !== undefined &&
    whiteListOpprettAvtaleKnapp.includes(tiltakstype.arenakode)
  );
}

function harBrukerRettPaaValgtTiltak(
  bruker: Bruker,
  tiltakstype: VeilederflateTiltakstype,
): boolean {
  if (!bruker.erUnderOppfolging || !bruker.innsatsgruppe) {
    return false;
  }

  return (
    (tiltakstype.innsatsgrupper ?? []).includes(bruker.innsatsgruppe) ||
    (bruker.erSykmeldtMedArbeidsgiver &&
      tiltakstype.tiltakskode === Tiltakskode.ARBEIDSRETTET_REHABILITERING &&
      bruker.innsatsgruppe === Innsatsgruppe.TRENGER_VEILEDNING)
  );
}
