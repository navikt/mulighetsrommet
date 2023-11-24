import { Alert } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useTiltaksgjennomforing } from "../../api/tiltaksgjennomforing/useTiltaksgjennomforing";
import { useUtkast } from "../../api/utkast/useUtkast";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { avtaleHarRegioner, inneholderUrl } from "../../utils/Utils";
import { Header } from "../detaljside/Header";
import { Laster } from "../laster/Laster";
import styles from "../skjema/Skjema.module.scss";
import {
  InferredTiltaksgjennomforingSchema,
  TiltaksgjennomforingSchema,
} from "./TiltaksgjennomforingSchema";
import { TiltaksgjennomforingSkjemaContainer } from "./TiltaksgjennomforingSkjemaContainer";
import { ErrorMeldinger } from "./TiltaksgjennomforingSkjemaErrors";

export type TiltaksgjennomforingUtkastData = Partial<InferredTiltaksgjennomforingSchema> & {
  id: string;
};

const TiltaksgjennomforingSkjemaPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const { data: tiltaksgjennomforing, isLoading: tiltaksgjennomforingLoading } =
    useTiltaksgjennomforing();
  const { data: utkast, isLoading: utkastLoading } = useUtkast(
    TiltaksgjennomforingSchema,
    searchParams.get("utkastId") || undefined,
  );
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

  if (avtaleIsLoading || utkastLoading || tiltaksgjennomforingLoading) {
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
  } else if (avtale) {
    content = (
      <TiltaksgjennomforingSkjemaContainer
        onClose={() => {
          refetchUtkast();
          navigerTilbake();
        }}
        onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
        avtale={avtale}
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
