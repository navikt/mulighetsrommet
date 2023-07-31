import { useQueryClient } from "@tanstack/react-query";
import { Tiltakstypestatus } from "mulighetsrommet-api-client";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { ContainerLayoutOversikt } from "../../layouts/ContainerLayout";
import { inneholderUrl } from "../../utils/Utils";
import { Header } from "../detaljside/Header";
import { Laster } from "../laster/Laster";
import styles from "./AvtaleSkjema.module.scss";
import { AvtaleSkjemaContainer } from "./AvtaleSkjemaContainer";
import { MainContainer } from "../../layouts/MainContainer";

const AvtaleSkjemaPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [searchParams] = useSearchParams();
  const { data: avtale } = useAvtale(searchParams.get("avtaleId") || undefined);
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } =
    useTiltakstyper({ status: Tiltakstypestatus.AKTIV }, 1);
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useAlleEnheter();

  const redigeringsModus = avtale && inneholderUrl(avtale?.id);

  const navigerTilbake = () => {
    navigate(-1);
  };

  return (
    <MainContainer>
      <div className={styles.avtaleskjema}>
        <Header
          dataTestId={
            redigeringsModus ? "rediger-avtale-header" : "opprett-avtale-header"
          }
        >
          {redigeringsModus ? "Rediger avtale" : "Opprett avtale"}
        </Header>
        {isLoadingAnsatt || isLoadingTiltakstyper || isLoadingEnheter ? (
          <Laster />
        ) : null}
        <ContainerLayoutOversikt>
          <div className={styles.avtaleskjema_content}>
            {!tiltakstyper?.data || !ansatt || !enheter ? null : (
              <AvtaleSkjemaContainer
                onClose={() => {
                  queryClient.refetchQueries({ queryKey: ["utkast"] });
                  navigerTilbake();
                }}
                onSuccess={(id) => navigate(`/avtaler/${id}`)}
                tiltakstyper={tiltakstyper.data}
                ansatt={ansatt}
                enheter={enheter}
                avtale={avtale}
                redigeringsModus={redigeringsModus!}
              />
            )}
          </div>
        </ContainerLayoutOversikt>
      </div>
    </MainContainer>
  );
};

export default AvtaleSkjemaPage;
