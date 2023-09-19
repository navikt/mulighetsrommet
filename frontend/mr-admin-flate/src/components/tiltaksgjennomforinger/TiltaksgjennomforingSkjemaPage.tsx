import { useQueryClient } from "@tanstack/react-query";
import { Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { useNavigate, useSearchParams } from "react-router-dom";
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
  const { data: tiltaksgjennomforing, isLoading: tiltaksgjennomforingLoading } =
    useTiltaksgjennomforing();
  const { data: utkast, isLoading: utkastLoading } = useUtkast(
    searchParams.get("utkastId") || undefined,
  );
  const { data: avtale, isLoading: avtaleIsLoading } = useAvtale(
    tiltaksgjennomforing?.avtaleId ?? utkast?.utkastData?.avtaleId,
  );

  const utkastModus = utkast && inneholderUrl(utkast?.id);
  const redigeringsModus =
    utkastModus ||
    (tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id));

  const navigerTilbake = () => {
    navigate(-1);
  };

  const isError =
    !avtale ||
    (avtale?.sluttDato && new Date(avtale.sluttDato) < new Date()) ||
    !avtale?.navRegion;

  if (avtaleIsLoading || utkastLoading || tiltaksgjennomforingLoading) {
    return (
      <Laster
        size="xlarge"
        tekst={
          utkastLoading ? "Laster utkast..." : "Laster tiltaksgjennomføring..."
        }
      />
    );
  }

  let content = null;
  if (isError ) {
    content = (
      <Alert variant="error">{ErrorMeldinger(avtale, redigeringsModus)}</Alert>
    );
  } else if (avtale) {
    content = (
      <TiltaksgjennomforingSkjemaContainer
        onClose={() => {
          queryClient.refetchQueries({ queryKey: ["utkast"] });
          navigerTilbake();
        }}
        onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
        avtale={avtale}
        tiltaksgjennomforing={
          (utkast?.utkastData as Tiltaksgjennomforing) || tiltaksgjennomforing
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
          : "Opprett ny tiltaksgjennomføring"}
      </Header>
      <ContainerLayoutDetaljer>
        <div className={styles.skjema}>
          <div className={styles.skjema_content}>{content}</div>
        </div>
      </ContainerLayoutDetaljer>
    </main>
  );
};

export default TiltaksgjennomforingSkjemaPage;
