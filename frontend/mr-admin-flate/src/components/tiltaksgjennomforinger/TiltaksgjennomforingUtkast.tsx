import { Alert, Button } from "@navikt/ds-react";
import {
  ApiError,
  Tiltaksgjennomforing,
  Utkast,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { formaterDatoTid } from "../../utils/Utils";
import { Laster } from "../laster/Laster";
import { OpprettTiltaksgjennomforingModal } from "../modal/OpprettTiltaksgjennomforingModal";
import { DocPencilIcon } from "@navikt/aksel-icons";
import styles from "./TiltaksgjennomforingUtkast.module.scss";

export function TiltaksgjennomforingUtkast() {
  // TODO Hent utkast basert på om man har valgt "mine" (default) eller alles utkast.

  const avtaleId = useGetAvtaleIdFromUrl();
  const {
    data = [],
    isLoading,
    error,
    refetch,
  } = useMineUtkast(Utkast.type.TILTAKSGJENNOMFORING);
  const [utkastForRedigering, setUtkastForRedigering] = useState<Utkast | null>(
    null
  );
  const { data: avtale } = useAvtale(avtaleId);
  const navigate = useNavigate();

  if (error as ApiError) {
    const apiError = error as ApiError;
    return (
      <Alert variant="error">
        Det var problemer ved henting av utkast. {apiError.message}
      </Alert>
    );
  }

  if (!data && !avtale && isLoading) {
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
              <UtkastKort
                utkast={utkast}
                onEdit={() => setUtkastForRedigering(utkast)}
              />
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

interface UtkastKortProps {
  utkast: Utkast;
  onEdit: () => void;
}

function UtkastKort({ utkast, onEdit }: UtkastKortProps) {
  const slettMutation = useDeleteUtkast();
  const { refetch } = useMineUtkast(Utkast.type.TILTAKSGJENNOMFORING);

  async function slettUtkast() {
    slettMutation.mutate(utkast.id, { onSuccess: async () => await refetch() });
  }

  // TODO Parse data
  const data: any = utkast.utkastData;
  return (
    <div className={styles.utkast}>
      <div>
        <small className={styles.tekst_med_ikon}>
          <DocPencilIcon /> Utkast
        </small>
        <small>Oppdatert: {formaterDatoTid(utkast.updatedAt!)}</small>
        <h2 className={styles.truncate} title={data?.navn}>
          {data?.navn || "Utkast uten tittel"}
        </h2>
      </div>
      <div className={styles.knappe_container}>
        <small>Opprettet: {formaterDatoTid(utkast.createdAt!)}</small>
        <div className={styles.knapper}>
          <Button
            data-testid="rediger-utkast-knapp"
            size="small"
            variant="primary"
            onClick={onEdit}
          >
            Rediger utkast
          </Button>
          <Button
            data-testid="slett-utkast-knapp"
            size="small"
            variant="danger"
            onClick={slettUtkast}
          >
            Slett utkast
          </Button>
        </div>
      </div>
    </div>
  );
}
