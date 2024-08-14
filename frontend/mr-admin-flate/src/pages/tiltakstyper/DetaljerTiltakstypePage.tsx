import { Alert, Heading, Tabs } from "@navikt/ds-react";
import { useTitle } from "@mr/frontend-common";
import { Link, Outlet, useLocation, useMatch } from "react-router-dom";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { Header } from "@/components/detaljside/Header";
import { Laster } from "@/components/laster/Laster";
import { TiltakstypestatusTag } from "@/components/statuselementer/TiltakstypestatusTag";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";

function useTiltakstypeBrodsmuler(tiltakstypeId?: string): Array<Brodsmule | undefined> {
  const match = useMatch("/tiltakstyper/:tiltakstypeId/avtaler");
  return [
    { tittel: "Forside", lenke: "/" },
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstypedetaljer", lenke: `/tiltakstyper/${tiltakstypeId}` },
    match
      ? { tittel: "Tiltaktypens avtaler", lenke: `/tiltakstyper/${tiltakstypeId}/avtaler` }
      : undefined,
  ];
}

export function DetaljerTiltakstypePage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: tiltakstype, isLoading } = useTiltakstypeById();
  useTitle(`Tiltakstyper ${tiltakstype?.navn ? `- ${tiltakstype.navn}` : ""}`);
  const brodsmuler = useTiltakstypeBrodsmuler(tiltakstype?.id);

  if (!tiltakstype && isLoading) {
    return <Laster tekst="Laster tiltakstype" />;
  }

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
        <Tabs.List className={commonStyles.list}>
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
        <ContainerLayout>
          <div id="panel">
            <Outlet />
          </div>
        </ContainerLayout>
      </Tabs>
    </main>
  );
}
