import { Alert } from "@navikt/ds-react";
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

const TiltaksgjennomforingSkjemaPage = () => {
  const navigate = useNavigate();
  const { data: tiltaksgjennomforing, isLoading: tiltaksgjennomforingLoading } =
    useTiltaksgjennomforingById();
  const { data: avtale, isLoading: avtaleIsLoading } = useAvtale(tiltaksgjennomforing?.avtaleId);

  const redigeringsModus = tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError = !avtale || !avtaleHarRegioner(avtale);

  if (avtaleIsLoading || tiltaksgjennomforingLoading) {
    return <Laster size="xlarge" tekst={"Laster tiltaksgjennomføring..."} />;
  }

  let content = null;
  if (isError) {
    content = <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>;
  } else if (avtale) {
    content = (
      <TiltaksgjennomforingSkjemaContainer
        onClose={() => {
          navigerTilbake();
        }}
        onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
        avtale={avtale}
        tiltaksgjennomforing={tiltaksgjennomforing}
      />
    );
  }

  return (
    <main>
      <Header>
        {redigeringsModus ? "Rediger tiltaksgjennomføring" : "Opprett ny tiltaksgjennomføring"}
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
