import { useQueryClient } from "@tanstack/react-query";
import { Avtale, Tiltakstypestatus } from "mulighetsrommet-api-client";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useNavEnheter } from "../../api/enhet/useNavEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { inneholderUrl } from "../../utils/Utils";
import { Header } from "../detaljside/Header";
import { Laster } from "../laster/Laster";
import { AvtaleSkjemaContainer } from "./AvtaleSkjemaContainer";
import { useUtkast } from "../../api/utkast/useUtkast";
import styles from "../skjema/Skjema.module.scss";

const AvtaleSkjemaPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [searchParams] = useSearchParams();
  const { data: avtale, isLoading: avtaleLoading } = useAvtale();
  const { data: utkast, isLoading: utkastLoading } = useUtkast(
    searchParams.get("utkastId") || undefined,
  );
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } = useTiltakstyper(
    { status: Tiltakstypestatus.AKTIV },
    1,
  );
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useNavEnheter();

  const utkastModus = utkast && inneholderUrl(utkast?.id);
  const redigeringsModus = utkastModus || (avtale && inneholderUrl(avtale?.id));

  const navigerTilbake = () => {
    navigate(-1);
  };

  if (utkastLoading || avtaleLoading) {
    return <Laster size="xlarge" tekst={utkastLoading ? "Laster utkast..." : "Laster avtale..."} />;
  }

  return (
    <main>
      <Header dataTestId={redigeringsModus ? "rediger-avtale-header" : "opprett-avtale-header"}>
        {redigeringsModus
          ? utkastModus
            ? "Rediger utkast"
            : "Rediger avtale"
          : "Opprett ny avtale"}
      </Header>
      <ContainerLayout>
        <div className={styles.skjema}>
          {isLoadingAnsatt || isLoadingTiltakstyper || isLoadingEnheter ? <Laster /> : null}
          <div className={styles.skjema_content}>
            {!tiltakstyper?.data || !ansatt || !enheter ? null : (
              <AvtaleSkjemaContainer
                onClose={() => {
                  queryClient.refetchQueries({
                    queryKey: ["utkast"],
                  });
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
      </ContainerLayout>
    </main>
  );
};

export default AvtaleSkjemaPage;
