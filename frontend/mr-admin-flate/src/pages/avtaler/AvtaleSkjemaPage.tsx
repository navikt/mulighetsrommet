import { Tiltakstypestatus } from "mulighetsrommet-api-client";
import { useLocation, useNavigate } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { inneholderUrl } from "../../utils/Utils";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import styles from "../../components/skjema/Skjema.module.scss";
import { AvtaleSkjemaContainer } from "../../components/avtaler/AvtaleSkjemaContainer";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { AvtalestatusTag } from "../../components/statuselementer/AvtalestatusTag";
import { Heading } from "@navikt/ds-react";

const AvtaleSkjemaPage = () => {
  const navigate = useNavigate();

  const { data: avtale, isLoading: avtaleLoading } = useAvtale();
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } = useTiltakstyper(
    { status: Tiltakstypestatus.AKTIV },
    1,
  );
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useNavEnheter();
  const location = useLocation();

  const redigeringsModus = avtale && inneholderUrl(avtale?.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  if (avtaleLoading) {
    return <Laster size="xlarge" tekst={"Laster avtale..."} />;
  }

  return (
    <main>
      <Header>
        <Heading size="large" level="2">
          {redigeringsModus ? "Rediger avtale" : "Opprett ny avtale"}
        </Heading>
        {avtale ? <AvtalestatusTag avtale={avtale} /> : null}
      </Header>

      <ContainerLayout>
        <div className={styles.skjema}>
          {isLoadingAnsatt || isLoadingTiltakstyper || isLoadingEnheter ? <Laster /> : null}
          <div className={styles.skjema_content}>
            {!tiltakstyper?.data || !ansatt || !enheter ? null : (
              <AvtaleSkjemaContainer
                onClose={() => {
                  navigerTilbake();
                }}
                onSuccess={(id) => navigate(`/avtaler/${id}`)}
                tiltakstyper={tiltakstyper.data}
                ansatt={ansatt}
                enheter={enheter}
                avtale={location.state?.avtale ? location.state.avtale : avtale}
                redigeringsModus={redigeringsModus!}
              />
            )}
          </div>
        </div>
      </ContainerLayout>
    </main>
  );
};

export default AvtaleSkjemaPage;
