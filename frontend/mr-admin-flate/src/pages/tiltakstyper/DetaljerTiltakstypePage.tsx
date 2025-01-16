import { Header } from "@/components/detaljside/Header";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypestatusTag } from "@/components/statuselementer/TiltakstypestatusTag";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { useTitle } from "@mr/frontend-common";
import { Alert, Heading, Tabs } from "@navikt/ds-react";
import { Link, Outlet, useLoaderData, useLocation, useMatch } from "react-router";
import { tiltakstypeLoader } from "./tiltakstyperLoaders";
import { ContentBox } from "@/layouts/ContentBox";

function useTiltakstypeBrodsmuler(tiltakstypeId?: string): Array<Brodsmule | undefined> {
  const match = useMatch("/tiltakstyper/:tiltakstypeId/avtaler");
  return [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: `/tiltakstyper/${tiltakstypeId}` },
    match ? { tittel: "Avtaler", lenke: `/tiltakstyper/${tiltakstypeId}/avtaler` } : undefined,
  ];
}

export function DetaljerTiltakstypePage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const tiltakstype = useLoaderData<typeof tiltakstypeLoader>();
  useTitle(`Tiltakstyper ${tiltakstype?.navn ? `- ${tiltakstype.navn}` : ""}`);
  const brodsmuler = useTiltakstypeBrodsmuler(tiltakstype?.id);

  if (!tiltakstype) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltakstype
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltakstypeIkon />
        <Heading size="large" level="2">
          {tiltakstype?.navn ?? "..."}
        </Heading>
        <TiltakstypestatusTag tiltakstype={tiltakstype} />
      </Header>

      <Tabs value={pathname.includes("avtaler") ? "avtaler" : "arenainfo"}>
        <Tabs.List className="p-[0 0.5rem] w-[1920px] flex items-start m-auto">
          <Tabs.Tab
            value="arenainfo"
            label="Arenainfo"
            onClick={() => navigateAndReplaceUrl(`/tiltakstyper/${tiltakstype.id}`)}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="avtaler"
            label="Avtaler"
            onClick={() => navigateAndReplaceUrl(`/tiltakstyper/${tiltakstype.id}/avtaler`)}
            aria-controls="panel"
          />
        </Tabs.List>
        <ContentBox>
          <div id="panel">
            <Outlet />
          </div>
        </ContentBox>
      </Tabs>
    </main>
  );
}
