import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { defaultTiltaksgjennomforingData } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaConst";
import { TiltaksgjennomforingSkjemaContainer } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaContainer";
import { ErrorMeldinger } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaErrors";
import { avtaleHarRegioner, inneholderUrl } from "@/utils/Utils";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";
import { Alert, Box, Heading } from "@navikt/ds-react";
import { useLoaderData, useLocation, useMatch, useNavigate, useParams } from "react-router";
import { tiltaksgjennomforingSkjemaLoader } from "./tiltaksgjennomforingLoaders";
import { ContentBox } from "@/layouts/ContentBox";

export function TiltaksgjennomforingSkjemaPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { avtaleId } = useParams();
  const { tiltaksgjennomforing, avtale, ansatt } =
    useLoaderData<typeof tiltaksgjennomforingSkjemaLoader>();
  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
  );

  const redigeringsModus = tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id);

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
            ? `/avtaler/${avtaleId}/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`
            : `/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Rediger gjennomføring" : "Ny gjennomføring",
      lenke: redigeringsModus
        ? `/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}/skjema`
        : "/tiltaksgjennomforinger/skjema",
    },
  ];

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <TiltaksgjennomforingIkon />
        <Heading size="large" level="2">
          {redigeringsModus ? "Rediger gjennomføring" : "Opprett ny gjennomføring"}
        </Heading>
        {tiltaksgjennomforing ? (
          <GjennomforingStatusMedAarsakTag status={tiltaksgjennomforing.status} />
        ) : null}
      </Header>
      <ContentBox>
        <Box padding="4" background="bg-default">
          {isError && <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>}
          {avtale && ansatt && (
            <TiltaksgjennomforingSkjemaContainer
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
              tiltaksgjennomforing={tiltaksgjennomforing}
              defaultValues={defaultTiltaksgjennomforingData(
                ansatt,
                avtale,
                location.state?.dupliserTiltaksgjennomforing ?? tiltaksgjennomforing,
              )}
            />
          )}
        </Box>
      </ContentBox>
    </main>
  );
}
