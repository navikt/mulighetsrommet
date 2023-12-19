import { Alert, Heading } from "@navikt/ds-react";
import { useNavigate } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { avtaleHarRegioner, inneholderUrl } from "../../utils/Utils";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import styles from "../../components/skjema/Skjema.module.scss";
import { TiltaksgjennomforingSkjemaContainer } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaContainer";
import { ErrorMeldinger } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaErrors";
import { TiltaksgjennomforingstatusTag } from "../../components/statuselementer/TiltaksgjennomforingstatusTag";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";

const TiltaksgjennomforingSkjemaPage = () => {
  const navigate = useNavigate();
  const { data: tiltaksgjennomforing, isLoading: tiltaksgjennomforingLoading } =
    useTiltaksgjennomforingById();
  const { data: avtale, isLoading: avtaleIsLoading } = useAvtale(tiltaksgjennomforing?.avtaleId);
  const { data: ansatt, isPending: isPendingAnsatt } = useHentAnsatt();

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
        onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
        avtale={avtale}
        ansatt={ansatt}
        tiltaksgjennomforing={tiltaksgjennomforing}
      />
    );
  }

  return (
    <main>
      <Header>
        <Heading size="large" level="2">
          {redigeringsModus ? "Rediger tiltaksgjennomføring" : "Opprett ny tiltaksgjennomføring"}
        </Heading>
        {tiltaksgjennomforing ? (
          <TiltaksgjennomforingstatusTag tiltaksgjennomforing={tiltaksgjennomforing} />
        ) : null}
      </Header>
      <ContainerLayout>
        <div className={styles.skjema}>
          <div className={styles.skjema_content}>{content}</div>
        </div>
      </ContainerLayout>
    </main>
  );
};

export default TiltaksgjennomforingSkjemaPage;
