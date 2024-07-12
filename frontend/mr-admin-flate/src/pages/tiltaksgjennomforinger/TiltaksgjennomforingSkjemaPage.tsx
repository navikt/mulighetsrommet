import { Alert, Heading } from "@navikt/ds-react";
import { useLocation, useMatch, useNavigate, useParams } from "react-router-dom";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "@/api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { avtaleHarRegioner, inneholderUrl } from "@/utils/Utils";
import { Header } from "@/components/detaljside/Header";
import { Laster } from "@/components/laster/Laster";
import { TiltaksgjennomforingSkjemaContainer } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaContainer";
import { ErrorMeldinger } from "@/components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaErrors";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { TiltaksgjennomforingStatusTag } from "mulighetsrommet-frontend-common";
import { SkjemaContainer } from "@/components/skjema/SkjemaContainer";
import { SkjemaContent } from "@/components/skjema/SkjemaContent";

const TiltaksgjennomforingSkjemaPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { avtaleId } = useParams();
  const { data: tiltaksgjennomforing, isLoading: tiltaksgjennomforingLoading } =
    useTiltaksgjennomforingById();
  const { data: avtale, isLoading: avtaleIsLoading } = useAvtale(tiltaksgjennomforing?.avtaleId);
  const { data: ansatt, isPending: isPendingAnsatt } = useHentAnsatt();
  const erPaaGjennomforingerForAvtale = useMatch(
    "/avtaler/:avtaleId/tiltaksgjennomforinger/:tiltaksgjennomforingId/skjema",
  );

  const redigeringsModus = tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError = !avtale || !avtaleHarRegioner(avtale);

  if (avtaleIsLoading || tiltaksgjennomforingLoading || isPendingAnsatt) {
    return <Laster size="xlarge" tekst={"Laster tiltaksgjennomføring..."} />;
  }

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
        ansatt={ansatt}
        tiltaksgjennomforing={
          location.state?.tiltaksgjennomforing
            ? location.state.tiltaksgjennomforing
            : tiltaksgjennomforing
        }
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
      tittel: redigeringsModus ? "Rediger tiltaksgjennomføring" : "Ny tiltaksgjennomføring",
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
          {redigeringsModus ? "Rediger tiltaksgjennomføring" : "Opprett ny tiltaksgjennomføring"}
        </Heading>
        {tiltaksgjennomforing ? (
          <TiltaksgjennomforingStatusTag status={tiltaksgjennomforing.status} showAvbruttAarsak />
        ) : null}
      </Header>
      <ContainerLayout>
        <SkjemaContainer>
          <SkjemaContent>{content}</SkjemaContent>
        </SkjemaContainer>
      </ContainerLayout>
    </main>
  );
};

export default TiltaksgjennomforingSkjemaPage;
