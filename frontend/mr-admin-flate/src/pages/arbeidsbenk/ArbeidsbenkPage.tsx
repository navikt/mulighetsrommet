import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Tiltakskode, Toggles } from "@mr/api-client-v2";
import { Tabs } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { Outlet, useLocation, useNavigate } from "react-router";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { ArbeidsbenkIkon } from "../../components/ikoner/ArbeidsbenkIkon";
import { ulesteNotifikasjonerQuery } from "./notifikasjoner/notifikasjonerQueries";

export function ArbeidsbenkPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  const { data: enableArbeidsbenk } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
    [Tiltakskode.ARBEIDSFORBEREDENDE_TRENING],
  );
  const { data: ulesteNotifikasjoner } = useSuspenseQuery({ ...ulesteNotifikasjonerQuery });
  const antallNotifikasjoner = ulesteNotifikasjoner.data.pagination.totalCount;

  return (
    <main>
      <title>Arbeidsbenk</title>
      <HeaderBanner heading="Arbeidsbenk" harUndermeny ikon={<ArbeidsbenkIkon />} />
      <Tabs
        value={pathname.includes("notifikasjoner") ? "notifikasjoner" : "oppgaver"}
        selectionFollowsFocus
      >
        <Tabs.List id="fane_liste">
          {enableArbeidsbenk && (
            <Tabs.Tab
              value="oppgaver"
              label={`Oppgaver`}
              onClick={() => navigate("/arbeidsbenk/oppgaver")}
              aria-controls="panel"
            />
          )}
          <Tabs.Tab
            value="notifikasjoner"
            label={`Notifikasjoner ${antallNotifikasjoner ? `(${antallNotifikasjoner})` : ""}`}
            onClick={() => navigate("/arbeidsbenk/notifikasjoner")}
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
