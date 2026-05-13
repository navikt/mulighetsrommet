import {
  isTiltakAktivt,
  isTiltakEnkeltplass,
  isTiltakGruppe,
  useModiaArbeidsmarkedstiltakById,
} from "@/api/queries/useArbeidsmarkedstiltakById";
import { DelMedBruker } from "@/apps/modia/delMedBruker/DelMedBruker";
import { useBrukerdata } from "@/apps/modia/hooks/useBrukerdata";
import { useDeltMedBruker } from "@/apps/modia/hooks/useDeltMedBruker";
import { useModiaContext } from "@/apps/modia/hooks/useModiaContext";
import { useVeilederdata } from "@/apps/modia/hooks/useVeilederdata";
import { BrukerKvalifisererIkkeVarsel } from "@/apps/modia/varsler/BrukerKvalifisererIkkeVarsel";
import { DetaljerJoyride } from "@/components/joyride/DetaljerJoyride";
import { PameldingForGruppetiltak } from "@/components/pamelding/PameldingForGruppetiltak";
import { PersonvernContainer } from "@/components/personvern/PersonvernContainer";
import { SidemenyLenker } from "@/components/sidemeny/SidemenyLenker";
import { Tilbakeknapp } from "@/components/tilbakeknapp/Tilbakeknapp";
import { paginationAtom } from "@/core/atoms";
import { ArbeidsmarkedstiltakErrorBoundary } from "@/ErrorBoundary";
import { useTiltakIdFraUrl } from "@/hooks/useTiltakIdFraUrl";
import { ViewTiltakDetaljer } from "@/layouts/ViewTiltakDetaljer";
import {
  Brukerdata,
  Innsatsgruppe,
  NavVeilederDto,
  Tiltakskode,
  VeilederflateTiltakstype,
} from "@api-client";
import { Chat2Icon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import { useAtomValue } from "jotai";
import { ModiaRoute, resolveModiaRoute } from "../ModiaRoute";
import { OpprettAvtale } from "@/components/pamelding/OpprettAvtale";
import { StartRegistreringEnkeltplass } from "@/components/pamelding/StartRegistreringEnkeltplass";
import { isProduction } from "@/environment";

export function ModiaArbeidsmarkedstiltakDetaljer() {
  const { fnr } = useModiaContext();
  const id = useTiltakIdFraUrl();
  const { data: deltMedBruker } = useDeltMedBruker(fnr, id);
  const { enhet } = useModiaContext();

  const { data: veileder } = useVeilederdata();
  const { data: brukerdata } = useBrukerdata();
  const { data: tiltak } = useModiaArbeidsmarkedstiltakById();

  const pagination = useAtomValue(paginationAtom);

  const tiltakstype = tiltak.tiltakstype;
  const brukerHarRettPaaValgtTiltak = harBrukerRettPaaValgtTiltak(brukerdata, tiltakstype);

  const dialogRoute = deltMedBruker
    ? resolveModiaRoute({
        route: ModiaRoute.DIALOG,
        dialogId: deltMedBruker.deling.dialogId,
      })
    : null;

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
              <DetaljerJoyride />
            </div>
          </>
        }
        brukerActions={
          <>
            {isTiltakEnkeltplass(tiltak) && (
              <OpprettAvtale
                tiltakstype={tiltakstype}
                harRettPaaTiltak={brukerHarRettPaaValgtTiltak}
              />
            )}

            {!isProduction && isTiltakEnkeltplass(tiltak) && (
              <StartRegistreringEnkeltplass
                tiltakstype={tiltakstype}
                harRettPaaTiltak={brukerHarRettPaaValgtTiltak}
              />
            )}

            {isTiltakGruppe(tiltak) && isTiltakAktivt(tiltak) && (
              <PameldingForGruppetiltak
                brukerHarRettPaaValgtTiltak={brukerHarRettPaaValgtTiltak}
                tiltak={tiltak}
              />
            )}

            {brukerdata.erUnderOppfolging && isTiltakAktivt(tiltak) && (
              <DelMedBruker
                deltMedBruker={deltMedBruker ?? undefined}
                veiledernavn={resolveName(veileder)}
                tiltak={tiltak}
                bruker={brukerdata}
                veilederEnhet={enhet}
              />
            )}

            {brukerdata.erUnderOppfolging && dialogRoute && (
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

            {isTiltakGruppe(tiltak) && tiltak.personvernBekreftet && (
              <ArbeidsmarkedstiltakErrorBoundary>
                <PersonvernContainer tiltak={tiltak} />
              </ArbeidsmarkedstiltakErrorBoundary>
            )}

            <SidemenyLenker tiltak={tiltak} />
          </>
        }
      />
    </>
  );
}

function resolveName(ansatt: NavVeilederDto) {
  return [ansatt.fornavn, ansatt.etternavn].filter((part) => part !== "").join(" ");
}

function harBrukerRettPaaValgtTiltak(
  bruker: Brukerdata,
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
