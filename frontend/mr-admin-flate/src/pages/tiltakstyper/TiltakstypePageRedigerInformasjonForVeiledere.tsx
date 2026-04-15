import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { Header } from "@/components/detaljside/Header";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeInformasjonForVeiledereForm } from "@/components/tiltakstype/TiltakstypeInformasjonForVeiledereForm";
import { ContentBox } from "@/layouts/ContentBox";
import { Box, Heading } from "@navikt/ds-react";
import { useNavigate } from "react-router";

export function TiltakstypePageRedigerInformasjonForVeiledere() {
  const { data: tiltakstype } = useTiltakstypeById();
  const navigate = useNavigate();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: `/tiltakstyper/${tiltakstype.id}` },
    { tittel: "Rediger tiltakstype" },
  ];

  async function handleSuccess() {
    navigate(`/tiltakstyper/${tiltakstype.id}/redaksjonelt-innhold`);
  }

  return (
    <>
      <title>{`Rediger tiltakstype | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltakstypeIkon />
        <Heading size="large" level="2">
          Rediger tiltakstype
        </Heading>
      </Header>
      <ContentBox>
        <Box padding="space-16" background="default">
          <TiltakstypeInformasjonForVeiledereForm
            tiltakstype={tiltakstype}
            onSuccess={handleSuccess}
            onCancel={() => navigate(-1)}
          />
        </Box>
      </ContentBox>
    </>
  );
}
