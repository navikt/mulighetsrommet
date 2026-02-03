import { Header } from "@/components/detaljside/Header";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormContainer } from "@/components/gjennomforing/GjennomforingFormContainer";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { Box, Heading } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useLocation, useNavigate } from "react-router";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { QueryKeys } from "@/api/QueryKeys";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";

function useGjennomforingFormData() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const tiltakstype = useTiltakstype(avtale.tiltakstype.id);
  const { data: ansatt } = useHentAnsatt();
  return { tiltakstype, avtale, ansatt };
}

export function OpprettGjennomforingFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { tiltakstype, avtale, ansatt } = useGjennomforingFormData();
  const queryClient = useQueryClient();

  const navigerTilbake = () => {
    navigate(-1);
  };

  const navigerTilGjennomforing = async (id: string) => {
    await queryClient.invalidateQueries({
      queryKey: QueryKeys.gjennomforing(id),
      type: "all",
    });
    navigate(`/gjennomforinger/${id}`);
  };

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: "/gjennomforinger",
    },
    {
      tittel: "Ny gjennomføring",
    },
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          Opprett ny gjennomføring
        </Heading>
      </Header>
      <ContentBox>
        <Box padding="4" background="bg-default">
          <GjennomforingFormContainer
            onClose={navigerTilbake}
            onSuccess={navigerTilGjennomforing}
            tiltakstype={tiltakstype}
            avtale={avtale}
            gjennomforing={null}
            deltakere={null}
            defaultValues={defaultGjennomforingData(
              ansatt,
              tiltakstype,
              avtale,
              location.state?.dupliserGjennomforing,
            )}
          />
        </Box>
      </ContentBox>
    </>
  );
}
