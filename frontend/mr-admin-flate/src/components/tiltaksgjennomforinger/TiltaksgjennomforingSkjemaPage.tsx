import { useQueryClient } from "@tanstack/react-query";
import {
  Tiltaksgjennomforing,
  Tiltakstypestatus,
} from "mulighetsrommet-api-client";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { ContainerLayoutDetaljer } from "../../layouts/ContainerLayout";
import { inneholderUrl } from "../../utils/Utils";
import { Header } from "../detaljside/Header";
import { Laster } from "../laster/Laster";
import { useUtkast } from "../../api/utkast/useUtkast";
import { TiltaksgjennomforingSkjemaContainer } from "./TiltaksgjennomforingSkjemaContainer";
import React from "react";
import { useTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforing";
import styles from "../skjema/Skjema.module.scss";
import { Alert } from "@navikt/ds-react";
import { ErrorMeldinger } from "./TiltaksgjennomforingSkjemaErrors";
import { useAvtale } from "../../api/avtaler/useAvtale";

const TiltaksgjennomforingSkjemaPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const {
    data: tiltaksgjennomforing,
    isFetching: tiltaksgjennomforingFetching,
  } = useTiltaksgjennomforing(
    searchParams.get("tiltaksgjennomforingId") || undefined,
  );
  const { data: avtale } = useAvtale(
    searchParams.get("avtaleId") || tiltaksgjennomforing?.avtaleId,
  );
  const { data: utkast, isFetching: utkastFetching } = useUtkast(
    searchParams.get("utkastId") || undefined,
  );
  const { data: tiltakstyper, isLoading: isLoadingTiltakstyper } =
    useTiltakstyper({ status: Tiltakstypestatus.AKTIV }, 1);
  const {
    data: ansatt,
    isLoading: isLoadingAnsatt,
    isError: isErrorAnsatt,
  } = useHentAnsatt();
  const {
    data: enheter,
    isLoading: isLoadingEnheter,
    isError: isErrorEnheter,
  } = useAlleEnheter();

  const utkastModus = utkast && inneholderUrl(utkast?.id);
  const redigeringsModus =
    utkastModus ||
    (tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id));

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError =
    !avtale ||
    (avtale && avtale?.sluttDato && new Date(avtale.sluttDato) < new Date()) ||
    !avtale?.navRegion ||
    isErrorAnsatt ||
    isErrorEnheter;

  if (utkastFetching || tiltaksgjennomforingFetching) {
    return (
      <Laster
        size="xlarge"
        tekst={
          utkastFetching ? "Laster utkast..." : "Laster tiltaksgjennomføring..."
        }
      />
    );
  }

  return (
    <main>
      <Header
        dataTestId={
          redigeringsModus
            ? "rediger-tiltaksgjennomforing-header"
            : "opprett-tiltaksgjennomforing-header"
        }
      >
        {redigeringsModus
          ? utkastModus
            ? "Rediger utkast"
            : "Rediger tiltaksgjennomføring"
          : "Opprett ny tiltaksgjennomforing"}
      </Header>
      <ContainerLayoutDetaljer>
        <div className={styles.skjema}>
          {isLoadingAnsatt || isLoadingTiltakstyper || isLoadingEnheter ? (
            <Laster />
          ) : null}
          <div className={styles.skjema_content}>
            {isError && redigeringsModus ? (
              <Alert variant="error">
                {ErrorMeldinger(
                  avtale!,
                  redigeringsModus,
                  isErrorAnsatt,
                  isErrorEnheter,
                )}
              </Alert>
            ) : (!tiltakstyper?.data || !ansatt || !enheter) &&
              !isError ? null : (
              <TiltaksgjennomforingSkjemaContainer
                onClose={() => {
                  queryClient.refetchQueries({ queryKey: ["utkast"] });
                  navigerTilbake();
                }}
                onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
                avtale={avtale}
                tiltaksgjennomforing={
                  (utkast?.utkastData as Tiltaksgjennomforing) ||
                  tiltaksgjennomforing
                }
              />
            )}
          </div>
        </div>
      </ContainerLayoutDetaljer>
    </main>
  );
};

export default TiltaksgjennomforingSkjemaPage;
