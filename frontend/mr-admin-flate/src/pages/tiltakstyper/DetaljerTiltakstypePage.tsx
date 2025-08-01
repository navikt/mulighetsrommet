import { Header } from "@/components/detaljside/Header";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeStatusTag } from "@/components/statuselementer/TiltakstypeStatusTag";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { Heading, Tabs } from "@navikt/ds-react";
import { Outlet, useLocation, useMatch } from "react-router";
import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";

export function DetaljerTiltakstypePage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: tiltakstype } = useTiltakstypeById();

  const matchAvtaler = useMatch("/tiltakstyper/:tiltakstypeId/avtaler");
  const brodsmuler: (Brodsmule | undefined)[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: matchAvtaler ? `/tiltakstyper/${tiltakstype.id}` : undefined },
    matchAvtaler ? { tittel: "Avtaler" } : undefined,
  ];

  return (
    <>
      <title>{`Tiltakstype | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltakstypeIkon />
        <Heading size="large" level="2">
          {tiltakstype.navn}
        </Heading>
        <TiltakstypeStatusTag status={tiltakstype.status} />
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
    </>
  );
}
