import { useTiltakstypeById } from "@/api/tiltakstyper/useTiltakstypeById";
import { Header } from "@/components/detaljside/Header";
import { TiltakstypeIkon } from "@/components/ikoner/TiltakstypeIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltakstypeDeltakerRegistreringForm } from "@/components/tiltakstype/TiltakstypeDeltakerRegistreringForm";
import { ContentBox } from "@/layouts/ContentBox";
import { Box, Heading } from "@navikt/ds-react";
import { useNavigate } from "react-router";

export function TiltakstypePageRedigerDeltakerRegistrering() {
  const { data: tiltakstype } = useTiltakstypeById();
  const navigate = useNavigate();

  const brodsmuler: Brodsmule[] = [
    { tittel: "Tiltakstyper", lenke: "/tiltakstyper" },
    { tittel: "Tiltakstype", lenke: `/tiltakstyper/${tiltakstype.id}` },
    { tittel: "Rediger informasjon for deltakere" },
  ];

  async function handleSuccess() {
    navigate(`/tiltakstyper/${tiltakstype.id}/deltaker-registrering`);
  }

  return (
    <>
      <title>{`Rediger informasjon for deltakere | ${tiltakstype.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltakstypeIkon />
        <Heading size="large" level="2">
          Rediger informasjon for deltakere
        </Heading>
      </Header>
      <ContentBox>
        <Box padding="space-16" background="default">
          <TiltakstypeDeltakerRegistreringForm
            tiltakstype={tiltakstype}
            onSuccess={handleSuccess}
            onCancel={() => navigate(-1)}
          />
        </Box>
      </ContentBox>
    </>
  );
}
