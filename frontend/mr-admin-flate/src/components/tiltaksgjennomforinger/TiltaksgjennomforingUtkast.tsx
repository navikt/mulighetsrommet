import { Alert } from "@navikt/ds-react";
import {
  ApiError,
  Tiltaksgjennomforing,
  Utkast,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { Laster } from "../laster/Laster";
import { OpprettTiltaksgjennomforingModal } from "../modal/OpprettTiltaksgjennomforingModal";
import { UtkastKort } from "../utkast/Utkastkort";
import styles from "./TiltaksgjennomforingUtkast.module.scss";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";

export function TiltaksgjennomforingUtkast() {
  const {
    data = [],
    isLoading,
    error,
    refetch,
  } = useMineUtkast(Utkast.type.TILTAKSGJENNOMFORING);
  const [utkastForRedigering, setUtkastForRedigering] = useState<Utkast | null>(
    null,
  );
  const { data: avtale } = useAvtale();
  const navigate = useNavigate();
  const mutation = useDeleteUtkast();

  if (error as ApiError) {
    const apiError = error as ApiError;
    return (
      <Alert variant="error">
        Det var problemer ved henting av utkast. {apiError.message}
      </Alert>
    );
  }

  if (data.length === 0 && !avtale && isLoading) {
    return <Laster tekst="Henter utkast..." />;
  }

  return (
    <div className={styles.container}>
      {data.length === 0 ? (
        <Alert variant="info">Du har ingen utkast</Alert>
      ) : null}
      <ul className={styles.liste}>
        {data?.map((utkast) => {
          return (
            <li key={utkast.id}>
              <UtkastKort utkast={utkast} mutation={mutation} />
            </li>
          );
        })}
      </ul>
      {utkastForRedigering ? (
        <OpprettTiltaksgjennomforingModal
          modalOpen={!!utkastForRedigering}
          avtale={avtale}
          tiltaksgjennomforing={
            utkastForRedigering?.utkastData as Tiltaksgjennomforing
          }
          onClose={async () => {
            refetch();
            setUtkastForRedigering(null);
          }}
          onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
        />
      ) : null}
    </div>
  );
}
