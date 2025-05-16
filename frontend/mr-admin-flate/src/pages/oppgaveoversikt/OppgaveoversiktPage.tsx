import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Tiltakskode, Toggles } from "@mr/api-client-v2";
import { Tabs } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { Outlet, useLocation, useNavigate } from "react-router";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { OppgaveoversiktIkon } from "@/components/ikoner/OppgaveoversiktIkon";
import { ulesteNotifikasjonerQuery } from "./notifikasjoner/notifikasjonerQueries";

export function OppgaveoversiktPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  const { data: enableOppgaveoversikt } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN,
    [Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET],
  );
  const { data: ulesteNotifikasjoner } = useSuspenseQuery({ ...ulesteNotifikasjonerQuery });
  const antallNotifikasjoner = ulesteNotifikasjoner.data.pagination.totalCount;

  return (
    <main>
      <title>Oppgaveoversikt</title>
      <HeaderBanner heading="Oppgaveoversikt" harUndermeny ikon={<OppgaveoversiktIkon />} />
      <Tabs
        value={pathname.includes("notifikasjoner") ? "notifikasjoner" : "oppgaver"}
        selectionFollowsFocus
      >
        <Tabs.List id="fane_liste">
          {enableOppgaveoversikt && (
            <Tabs.Tab
              value="oppgaver"
              label={`Oppgaver`}
              onClick={() => navigate("/oppgaveoversikt/oppgaver")}
              aria-controls="panel"
            />
          )}
          <Tabs.Tab
            value="notifikasjoner"
            label={`Notifikasjoner ${antallNotifikasjoner ? `(${antallNotifikasjoner})` : ""}`}
            onClick={() => navigate("/oppgaveoversikt/notifikasjoner")}
            aria-controls="panel"
            data-testid="notifikasjoner"
          />
        </Tabs.List>
        <ContentBox>
          <Outlet />
        </ContentBox>
      </Tabs>
    </main>
  );
}
