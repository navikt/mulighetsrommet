import { Avtale, Tiltakstypestatus } from "mulighetsrommet-api-client";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { Laster } from "../laster/Laster";
import { AvtaleSkjemaContainer } from "./AvtaleSkjemaContainer";
import styles from "./AvtaleSkjema.module.scss";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Header } from "../detaljside/Header";
import { inneholderUrl } from "../../utils/Utils";
import { MainContainer } from "../../layouts/MainContainer";
import { useUtkast } from "../../api/utkast/useUtkast";

const AvtaleSkjemaPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [searchParams] = useSearchParams();
  const { data: avtale, isFetching: avtaleFetching } = useAvtale(
    searchParams.get("avtaleId") || undefined,
  );
  const { data: utkast, isFetching: utkastFetching } = useUtkast(
    searchParams.get("utkastId") || undefined,
  );
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } =
    useTiltakstyper({ status: Tiltakstypestatus.AKTIV }, 1);
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useAlleEnheter();

  const utkastModus = utkast && inneholderUrl(utkast?.id);
  const redigeringsModus = utkastModus || (avtale && inneholderUrl(avtale?.id));

  const navigerTilbake = () => {
    navigate(-1);
  };

  if (utkastFetching || avtaleFetching) {
    return (
      <Laster
        size="xlarge"
        tekst={utkastFetching ? "Laster utkast..." : "Laster avtale..."}
      />
    );
  }

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
              avtale={(utkast?.utkastData as Avtale) || avtale}
              redigeringsModus={redigeringsModus!}
            />
          )}
        </div>
      </div>
    </MainContainer>
  );
};

export default AvtaleSkjemaPage;
