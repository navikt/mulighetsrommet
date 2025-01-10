import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { defaultTiltaksgjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormContainer } from "@/components/gjennomforing/GjennomforingFormContainer";
import { ErrorMeldinger } from "@/components/gjennomforing/GjennomforingFormErrors";
import { avtaleHarRegioner, inneholderUrl } from "@/utils/Utils";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";
import { Alert, Box, Heading } from "@navikt/ds-react";
import { useLoaderData, useLocation, useMatch, useNavigate, useParams } from "react-router";
import { gjennomforingFormLoader } from "./gjennomforingLoaders";
import { ContentBox } from "@/layouts/ContentBox";

export function GjennomforingFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { avtaleId } = useParams();
  const { gjennomforing, avtale, ansatt } = useLoaderData<typeof gjennomforingFormLoader>();
  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
  );

  const redigeringsModus = gjennomforing && inneholderUrl(gjennomforing?.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError = !avtale || !avtaleHarRegioner(avtale);

  const brodsmuler: Array<Brodsmule | undefined> = [
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Gjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtale",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    redigeringsModus
      ? {
          tittel: "Gjennomføring",
          lenke: avtaleId
            ? `/avtaler/${avtaleId}/tiltaksgjennomforinger/${gjennomforing?.id}`
            : `/tiltaksgjennomforinger/${gjennomforing?.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Rediger gjennomføring" : "Ny gjennomføring",
      lenke: redigeringsModus
        ? `/tiltaksgjennomforinger/${gjennomforing?.id}/skjema`
        : "/tiltaksgjennomforinger/skjema",
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
              onSuccess={(id) =>
                navigate(
                  avtaleId
                    ? `/avtaler/${avtaleId}/tiltaksgjennomforinger/${id}`
                    : `/tiltaksgjennomforinger/${id}`,
                )
              }
              avtale={avtale}
              gjennomforing={gjennomforing}
              defaultValues={defaultTiltaksgjennomforingData(
                ansatt,
                avtale,
                location.state?.dupliserTiltaksgjennomforing ?? gjennomforing,
              )}
            />
          )}
        </Box>
      </ContentBox>
    </main>
  );
}
