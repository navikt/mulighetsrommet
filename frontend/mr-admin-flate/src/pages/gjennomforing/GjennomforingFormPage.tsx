import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormContainer } from "@/components/gjennomforing/GjennomforingFormContainer";
import { ErrorMeldinger } from "@/components/gjennomforing/GjennomforingFormErrors";
import { avtaleHarRegioner, inneholderUrl } from "@/utils/Utils";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";
import { Alert, Box, Heading } from "@navikt/ds-react";
import { useLoaderData, useLocation, useNavigate } from "react-router";
import { gjennomforingFormLoader } from "./gjennomforingLoaders";
import { ContentBox } from "@/layouts/ContentBox";

export function GjennomforingFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { gjennomforing, avtale, ansatt } = useLoaderData<typeof gjennomforingFormLoader>();

  const redigeringsModus = gjennomforing && inneholderUrl(gjennomforing?.id);

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
        {gjennomforing ? <GjennomforingStatusMedAarsakTag status={gjennomforing.status} /> : null}
      </Header>
      <ContentBox>
        <Box padding="4" background="bg-default">
          {isError && <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>}
          {avtale && ansatt && (
            <GjennomforingFormContainer
              onClose={() => {
                navigerTilbake();
              }}
              onSuccess={(id) => navigate(`/gjennomforinger/${id}`)}
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
