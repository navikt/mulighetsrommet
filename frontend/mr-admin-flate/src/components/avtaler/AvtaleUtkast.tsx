import { Alert } from "@navikt/ds-react";
import { ApiError, Avtale, Utkast } from "mulighetsrommet-api-client";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { Laster } from "../laster/Laster";
import { UtkastKort } from "../utkast/Utkastkort";
import styles from "./AvtaleUtkast.module.scss";
import OpprettAvtaleModal from "./OpprettAvtaleModal";

export function AvtaleUtkast() {
  const {
    data = [],
    isLoading,
    error,
    refetch,
  } = useMineUtkast(Utkast.type.AVTALE);
  const [utkastForRedigering, setUtkastForRedigering] = useState<Utkast | null>(
    null
  );
  const navigate = useNavigate();

  if (error as ApiError) {
    const apiError = error as ApiError;
    return (
      <Alert variant="error">
        Det var problemer ved henting av utkast. {apiError.message}
      </Alert>
    );
  }

  if (!data && isLoading) {
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
        <OpprettAvtaleModal
          modalOpen={!!utkastForRedigering}
          avtale={utkastForRedigering?.utkastData as Avtale}
          onClose={async () => {
            refetch();
            setUtkastForRedigering(null);
          }}
          onSuccess={(id) => navigate(`/avtaler/${id}`)}
        />
      ) : null}
    </div>
  );
}
