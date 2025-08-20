import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { usePotentialAvtale } from "@/api/avtaler/useAvtale";
import { useNavEnheter } from "@/api/enhet/useNavEnheter";
import { useAdminGjennomforingById } from "@/api/gjennomforing/useAdminGjennomforingById";
import { QueryKeys } from "@/api/QueryKeys";
import { Header } from "@/components/detaljside/Header";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormContainer } from "@/components/gjennomforing/GjennomforingFormContainer";
import { ErrorMeldinger } from "@/components/gjennomforing/GjennomforingFormErrors";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { avtaleHarRegioner, inneholderUrl } from "@/utils/Utils";
import { Alert, Box, Heading } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useLocation, useNavigate } from "react-router";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { GjennomforingStatusMedAarsakTag } from "@/components/statuselementer/GjennomforingStatusMedAarsakTag";

function useGjennomforingFormData() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId);
  const { data: avtale } = usePotentialAvtale(gjennomforing.avtaleId);
  const { data: ansatt } = useHentAnsatt();
  return { gjennomforing, avtale, ansatt };
}

export function GjennomforingFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { gjennomforing, avtale, ansatt } = useGjennomforingFormData();
  const { data: enheter } = useNavEnheter();
  const queryClient = useQueryClient();

  const redigeringsModus = inneholderUrl(gjennomforing.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError = !avtale || !avtaleHarRegioner(avtale);

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: "/gjennomforinger",
    },
    redigeringsModus
      ? {
          tittel: "Gjennomføring",
          lenke: `/gjennomforinger/${gjennomforing.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Rediger gjennomføring" : "Ny gjennomføring",
    },
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          {redigeringsModus ? "Rediger gjennomføring" : "Opprett ny gjennomføring"}
        </Heading>
        <GjennomforingStatusMedAarsakTag status={gjennomforing.status} />
      </Header>
      <ContentBox>
        <Box padding="4" background="bg-default">
          {isError && <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>}
          {avtale && (
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
              gjennomforing={gjennomforing}
              defaultValues={defaultGjennomforingData(
                ansatt,
                avtale,
                location.state?.dupliserGjennomforing ?? gjennomforing,
              )}
              enheter={enheter}
            />
          )}
        </Box>
      </ContentBox>
    </>
  );
}
