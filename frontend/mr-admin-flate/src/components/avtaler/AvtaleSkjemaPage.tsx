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
  const { data: avtale, isLoading: avtaleLoading } = useAvtale();
  const { data: utkast, isLoading: utkastLoading } = useUtkast(
    searchParams.get("utkastId") || undefined,
  );
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } =
    useTiltakstyper({ status: Tiltakstypestatus.AKTIV }, 1);
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: enheter, isLoading: isLoadingEnheter } = useAlleEnheter();

  const utkastmodus = utkast && inneholderUrl(utkast?.id);
  const redigeringsmodus = utkastmodus || (avtale && inneholderUrl(avtale?.id));

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

  const redigerAvtaleHeader = () => {
    return (
      <div className={styles.avtaleheader}>
        Rediger avtale{" "}
        {avtale!.avtalestatus === "Aktiv" ? (
          <AvtalestatusTag avtale={avtale!} />
        ) : null}
      </div>
    );
  };

  return (
    <main>
      <Header
        dataTestId={
          redigeringsmodus ? "rediger-avtale-header" : "opprett-avtale-header"
        }
      >
        {redigeringsmodus
          ? utkastmodus
            ? "Rediger utkast"
            : redigerAvtaleHeader()
          : "Opprett ny avtale"}
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
                utkast={utkast!}
                avtale={(utkast?.utkastData as Avtale) || avtale}
                redigeringsmodus={redigeringsmodus!}
                utkastmodus={utkastmodus!}
              />
            )}
          </div>
        </div>
      </ContainerLayoutDetaljer>
    </main>
  );
};

export default AvtaleSkjemaPage;
