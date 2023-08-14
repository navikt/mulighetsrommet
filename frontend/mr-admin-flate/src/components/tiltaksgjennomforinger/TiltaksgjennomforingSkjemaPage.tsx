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
import { useAvtale } from "../../api/avtaler/useAvtale";
import { TiltaksgjennomforingStatus } from "../statuselementer/TiltaksgjennomforingStatus";
import { ErrorMeldinger } from "./TiltaksgjennomforingSkjemaErrors";
import { Alert } from "@navikt/ds-react";

const TiltaksgjennomforingSkjemaPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const {
    data: tiltaksgjennomforing,
    isLoading: tiltaksgjennomforingLoading,
  } = useTiltaksgjennomforing(
    searchParams.get("tiltaksgjennomforingId") || undefined,
  );
  const { data: avtale } = useAvtale(
    searchParams.get("avtaleId") || tiltaksgjennomforing?.avtaleId,
  );

  const { data: utkast, isLoading: utkastLoading } = useUtkast(
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

  const isError =
    !avtale ||
    (avtale?.sluttDato && new Date(avtale.sluttDato) < new Date()) ||
    !avtale?.navRegion ||
    isErrorAnsatt ||
    isErrorEnheter;

  if (utkastLoading || tiltaksgjennomforingLoading) {
    return (
      <Laster
        size="xlarge"
        tekst={
          utkastLoading ? "Laster utkast..." : "Laster tiltaksgjennomføring..."
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
        {redigeringsModus ? (
          utkastModus ? (
            "Rediger utkast"
          ) : (
            <div className={styles.rediger_header_status}>
              Rediger tiltaksgjennomføring
              <TiltaksgjennomforingStatus
                tiltaksgjennomforing={tiltaksgjennomforing!}
              />
            </div>
          )
        ) : (
          "Opprett ny tiltaksgjennomføring"
        )}
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
                onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
                avtale={avtale}
                tiltaksgjennomforing={
                  (utkast?.utkastData as Tiltaksgjennomforing) ||
                  tiltaksgjennomforing
                }
                utkastModus={utkastModus!}
              />
            )}
          </div>
        </div>
      </ContainerLayoutDetaljer>
    </main>
  );
};

export default TiltaksgjennomforingSkjemaPage;
