import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { Header } from "@/components/detaljside/Header";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeRedaksjoneltInnholdForm } from "@/components/tiltakstype/TiltakstypeRedaksjoneltInnholdForm";
import { ContentBox } from "@/layouts/ContentBox";
import { Box, Heading } from "@navikt/ds-react";
import { useNavigate } from "react-router";

export function RedigerTiltakstypeRedaksjoneltInnholdPage() {
  const { data: tiltakstype } = useTiltakstypeById();
  const navigate = useNavigate();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: `/tiltakstyper/${tiltakstype.id}` },
    { tittel: "Rediger redaksjonelt innhold" },
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
          Rediger redaksjonelt innhold
        </Heading>
      </Header>
      <ContentBox>
        <Box padding="space-16" background="default">
          <TiltakstypeRedaksjoneltInnholdForm
            tiltakstype={tiltakstype}
            onSuccess={handleSuccess}
            onCancel={() => navigate(-1)}
          />
        </Box>
      </ContentBox>
    </>
  );
}
