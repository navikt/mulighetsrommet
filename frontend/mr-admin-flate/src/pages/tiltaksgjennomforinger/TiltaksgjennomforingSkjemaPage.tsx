import { Alert } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useTiltaksgjennomforingById } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforingById";
import { useUtkast } from "../../api/utkast/useUtkast";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { avtaleHarRegioner, inneholderUrl } from "../../utils/Utils";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import styles from "../../components/skjema/Skjema.module.scss";
import {
  InferredTiltaksgjennomforingSchema,
  TiltaksgjennomforingSchema,
} from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingSchema";
import { TiltaksgjennomforingSkjemaContainer } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaContainer";
import { ErrorMeldinger } from "../../components/tiltaksgjennomforinger/TiltaksgjennomforingSkjemaErrors";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";

export type TiltaksgjennomforingUtkastData = Partial<InferredTiltaksgjennomforingSchema> & {
  id: string;
};

const TiltaksgjennomforingSkjemaPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const { data: tiltaksgjennomforing, isLoading: tiltaksgjennomforingLoading } =
    useTiltaksgjennomforingById();
  const { data: utkast, isLoading: utkastLoading } = useUtkast(
    TiltaksgjennomforingSchema,
    searchParams.get("utkastId") || undefined,
  );
  const { data: ansatt, isLoading: isLoadingAnsatt } = useHentAnsatt();
  const { data: avtale, isLoading: avtaleIsLoading } = useAvtale(
    tiltaksgjennomforing?.avtaleId ?? utkast?.utkastData.avtaleId,
  );

  const utkastModus = utkast && inneholderUrl(utkast?.id);
  const redigeringsModus =
    utkastModus || (tiltaksgjennomforing && inneholderUrl(tiltaksgjennomforing?.id));

  const navigerTilbake = () => {
    navigate(-1);
  };

  const refetchUtkast = () => queryClient.refetchQueries({ queryKey: ["utkast"] });

  const isError = !avtale || !avtaleHarRegioner(avtale);

  if (avtaleIsLoading || utkastLoading || tiltaksgjennomforingLoading || isLoadingAnsatt) {
    return (
      <Laster
        size="xlarge"
        tekst={utkastLoading ? "Laster utkast..." : "Laster tiltaksgjennomføring..."}
      />
    );
  }

  let content = null;
  if (isError) {
    content = <Alert variant="error">{ErrorMeldinger(avtale)}</Alert>;
  } else if (avtale && ansatt) {
    content = (
      <TiltaksgjennomforingSkjemaContainer
        onClose={() => {
          refetchUtkast();
          navigerTilbake();
        }}
        onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
        avtale={avtale}
        ansatt={ansatt}
        tiltaksgjennomforing={tiltaksgjennomforing}
        tiltaksgjennomforingUtkast={
          { ...utkast?.utkastData, id: utkast?.id } as TiltaksgjennomforingUtkastData
        }
      />
    );
  }

  return (
    <main>
      <Header>
        {redigeringsModus
          ? utkastModus
            ? "Rediger utkast"
            : "Rediger tiltaksgjennomføring"
          : "Opprett ny tiltaksgjennomføring"}
      </Header>
      <ContainerLayout>
        <div className={styles.skjema}>
          <div className={styles.skjema_content}>{content}</div>
        </div>
      </ContainerLayout>
    </main>
  );
};

export default TiltaksgjennomforingSkjemaPage;
