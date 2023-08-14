import { useQueryClient } from "@tanstack/react-query";
import { Avtale, Tiltakstypestatus } from "mulighetsrommet-api-client";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import { inneholderUrl } from "../../utils/Utils";
import { Header } from "../detaljside/Header";
import { Laster } from "../laster/Laster";
import { AvtaleSkjemaContainer } from "./AvtaleSkjemaContainer";
import { useUtkast } from "../../api/utkast/useUtkast";
import styles from "../skjema/Skjema.module.scss";
import { AvtalestatusTag } from "../statuselementer/AvtalestatusTag";

const AvtaleSkjemaPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [searchParams] = useSearchParams();
  const { data: avtale, isLoading: avtaleLoading } = useAvtale(
    searchParams.get("avtaleId") || undefined,
  );
  const { data: utkast, isLoading: utkastLoading } = useUtkast(
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

  if (utkastLoading || avtaleLoading) {
    return (
      <Laster
        size="xlarge"
        tekst={utkastLoading ? "Laster utkast..." : "Laster avtale..."}
      />
    );
  }

  return (
    <main>
      <Header
        dataTestId={
          redigeringsModus ? "rediger-avtale-header" : "opprett-avtale-header"
        }
      >
        {redigeringsModus ? (
          utkastModus ? (
            "Rediger utkast"
          ) : (
            <div className={styles.rediger_header_status}>
              Rediger avtale
              <AvtalestatusTag avtale={avtale!} />
            </div>
          )
        ) : (
          "Opprett ny avtale"
        )}
      </Header>
      <ContainerLayoutDetaljer>
        <div className={styles.skjema}>
          {isLoadingAnsatt || isLoadingTiltakstyper || isLoadingEnheter ? (
            <Laster />
          ) : null}
          <div className={styles.skjema_content}>
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
                utkastModus={utkastModus!}
              />
            )}
          </div>
        </div>
      </ContainerLayoutDetaljer>
    </main>
  );
};

export default AvtaleSkjemaPage;
