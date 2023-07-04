import { Alert, Button } from "@navikt/ds-react";
import {
  ApiError,
  Tiltaksgjennomforing,
  Utkast,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useAlleUtkast } from "../../api/utkast/useAlleUtkast";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import { Laster } from "../laster/Laster";
import { OpprettTiltaksgjennomforingModal } from "../modal/OpprettTiltaksgjennomforingModal";
import { formaterDatoTid } from "../../utils/Utils";

export function TiltaksgjennomforingUtkast() {
  // TODO Hent utkast basert på om man har valgt "mine" (default) eller alles utkast.

  const avtaleId = useGetAvtaleIdFromUrl();
  const { data: brukerData } = useHentAnsatt();
  const {
    data = [],
    isLoading,
    error,
    refetch,
  } = useAlleUtkast(avtaleId, brukerData?.navIdent);
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
    <div>
      <ul>
        {data.length === 0 ? <p>Det eksisterer ingen utkast</p> : null}

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
  const avtaleId = useGetAvtaleIdFromUrl();
  const { data: brukerData } = useHentAnsatt();
  const slettMutation = useDeleteUtkast();
  const { refetch } = useAlleUtkast(avtaleId, brukerData?.navIdent);

  async function slettUtkast() {
    slettMutation.mutate(utkast.id, { onSuccess: async () => await refetch() });
  }

  // TODO Parse data
  const data: any = utkast.utkastData;
  return (
    <div>
      <h2>{data?.navn || "Utkast uten tittel"}</h2>
      <p>Opprettet: {formaterDatoTid(utkast.createdAt!)}</p>
      <p>Oppdatert: {formaterDatoTid(utkast.updatedAt!)}</p>
      <div style={{ display: "flex", gap: "0.2rem" }}>
        <Button size="small" variant="primary" onClick={onEdit}>
          Rediger utkast
        </Button>
        <Button size="small" variant="danger" onClick={slettUtkast}>
          Slett utkast
        </Button>
      </div>
    </div>
  );
}
