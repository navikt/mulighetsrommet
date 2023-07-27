import { Tiltakstypestatus } from "mulighetsrommet-api-client";
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
    <div className={styles.avtaleskjema}>
      <Header>{redigeringsModus ? "Rediger avtale" : "Opprett avtale"}</Header>
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
            avtale={avtale}
            redigeringsModus={redigeringsModus!}
          />
        )}
      </div>
    </div>
  );
};

export default AvtaleSkjemaPage;
