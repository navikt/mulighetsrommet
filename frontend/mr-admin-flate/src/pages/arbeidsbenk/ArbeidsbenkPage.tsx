import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Tiltakskode, Toggles } from "@mr/api-client-v2";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { useSuspenseQuery } from "@tanstack/react-query";
import { Outlet, useLocation, useNavigate } from "react-router";
import { useFeatureToggle } from "../../api/features/useFeatureToggle";
import { ulesteNotifikasjonerQuery } from "./notifikasjoner/notifikasjonerQueries";

export function ArbeidsbenkPage() {
  useTitle("Arbeidsbenk");
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
      <HeaderBanner
        heading="Arbeidsbenk"
        harUndermeny
        ikon={<BellDotFillIcon title="Arbeidsbenk" fontSize={32} />}
      />
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
          <div className="max-w-[1280px] w-full m-auto">
            <Outlet />
          </div>
        </ContentBox>
      </Tabs>
    </main>
  );
}
