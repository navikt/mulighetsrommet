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
import { useNavEnheter } from "@/api/enhet/useNavEnheter";

function useGjennomforingFormData() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const { data: ansatt } = useHentAnsatt();
  const { data: enheter } = useNavEnheter();

  return { avtale, ansatt, enheter };
}

export function NewGjennomforingFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { avtale, ansatt, enheter } = useGjennomforingFormData();
  const queryClient = useQueryClient();

  const navigerTilbake = () => {
    navigate(-1);
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
            onClose={() => {
              navigerTilbake();
            }}
            onSuccess={async (id) => {
              await queryClient.invalidateQueries({
                queryKey: QueryKeys.gjennomforing(id),
                type: "all",
              });
              navigate(`/gjennomforinger/${id}`);
            }}
            avtale={avtale}
            gjennomforing={null}
            deltakere={null}
            defaultValues={defaultGjennomforingData(
              ansatt,
              avtale,
              location.state?.dupliserGjennomforing,
            )}
            enheter={enheter}
          />
        </Box>
      </ContentBox>
    </>
  );
}
