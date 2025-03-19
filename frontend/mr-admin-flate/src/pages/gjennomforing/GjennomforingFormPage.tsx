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
import { useLocation, useNavigate, useParams } from "react-router";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { usePotentialAvtale } from "../../api/avtaler/useAvtale";
import { useAdminGjennomforingById } from "../../api/gjennomforing/useAdminGjennomforingById";
import { QueryKeys } from "../../api/QueryKeys";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";

function useGjennomforingFormData() {
  const { gjennomforingId } = useParams();
  const { data: gjennomforing } = useAdminGjennomforingById(gjennomforingId!);
  const { data: avtale } = usePotentialAvtale(gjennomforing?.avtaleId);
  const { data: ansatt } = useHentAnsatt();

  return { gjennomforing, avtale, ansatt };
}

export function GjennomforingFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { gjennomforing, avtale, ansatt } = useGjennomforingFormData();
  const queryClient = useQueryClient();

  const redigeringsModus = gjennomforing && inneholderUrl(gjennomforing?.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError = !avtale || !avtaleHarRegioner(avtale);

  if (!avtale) {
    return null;
  }

  const brodsmuler: Array<Brodsmule | undefined> = [
    {
      tittel: "Gjennomføringer",
      lenke: "/gjennomforinger",
    },
    redigeringsModus
      ? {
          tittel: "Gjennomføring",
          lenke: `/gjennomforinger/${gjennomforing?.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Rediger gjennomføring" : "Ny gjennomføring",
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          {redigeringsModus ? "Rediger gjennomføring" : "Opprett ny gjennomføring"}
        </Heading>
        {gjennomforing ? (
          <GjennomforingStatusMedAarsakTag
            status={gjennomforing.status.status}
            avbrutt={gjennomforing.status.avbrutt}
          />
        ) : null}
      </Header>
      <ContentBox>
        <Box padding="4" background="bg-default">
          {isError && <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>}
          {avtale && ansatt && (
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
            />
          )}
        </Box>
      </ContentBox>
    </main>
  );
}
