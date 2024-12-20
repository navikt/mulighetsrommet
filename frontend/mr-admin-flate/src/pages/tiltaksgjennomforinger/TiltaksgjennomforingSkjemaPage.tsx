import { Header } from "@/components/detaljside/Header";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { SkjemaContainer } from "@/components/skjema/SkjemaContainer";
import { SkjemaContent } from "@/components/skjema/SkjemaContent";
import { defaultTiltaksgjennomforingData } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaConst";
import { TiltaksgjennomforingSkjemaContainer } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaContainer";
import { ErrorMeldinger } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaErrors";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { avtaleHarRegioner, inneholderUrl } from "@/utils/Utils";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";
import { Alert, Heading } from "@navikt/ds-react";
import { useLoaderData, useLocation, useMatch, useNavigate, useParams } from "react-router";
import { tiltaksgjennomforingLoader } from "./tiltaksgjennomforingLoaders";

export function TiltaksgjennomforingSkjemaPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { avtaleId } = useParams();
  const { tiltaksgjennomforing, avtale, ansatt } =
    useLoaderData<typeof tiltaksgjennomforingLoader>();
  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
  );

  const redigeringsModus = tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError = !avtale || !avtaleHarRegioner(avtale);

  let content = null;
  if (isError) {
    content = <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>;
  } else if (avtale && ansatt) {
    content = (
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
    );
  }

  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Forside", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId
      ? {
          tittel: "Avtaledetaljer",
          lenke: `/avtaler/${avtaleId}`,
        }
      : undefined,
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    redigeringsModus
      ? {
          tittel: "Tiltaksgjennomføringdetaljer",
          lenke: avtaleId
            ? `/avtaler/${avtaleId}/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`
            : `/tiltaksgjennomforinger/${tiltaksgjennomforing?.id}`,
        }
      : undefined,
    {
      tittel: redigeringsModus ? "Rediger gjennomføring" : "Ny tiltaksgjennomføring",
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
          {redigeringsModus ? "Rediger gjennomføring" : "Opprett ny tiltaksgjennomføring"}
        </Heading>
        {tiltaksgjennomforing ? (
          <GjennomforingStatusMedAarsakTag status={tiltaksgjennomforing.status} />
        ) : null}
      </Header>
      <ContainerLayout>
        <SkjemaContainer>
          <SkjemaContent>{content}</SkjemaContent>
        </SkjemaContainer>
      </ContainerLayout>
    </main>
  );
}
