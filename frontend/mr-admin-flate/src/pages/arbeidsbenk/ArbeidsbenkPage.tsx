import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router";
import { arbeidsbenkLoader } from "@/pages/arbeidsbenk/arbeidsbenkLoader";
import { ContentBox } from "@/layouts/ContentBox";
import { LoaderData } from "@/types/loader";

export function ArbeidsbenkPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { antallNotifikasjoner, enableArbeidsbenk } =
    useLoaderData<LoaderData<typeof arbeidsbenkLoader>>();

  useTitle("Arbeidsbenk");

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
