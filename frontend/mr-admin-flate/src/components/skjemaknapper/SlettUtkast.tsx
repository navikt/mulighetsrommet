import { Alert, Button, Heading } from "@navikt/ds-react";
import { ApiError, Utkast } from "mulighetsrommet-api-client";
import { useState } from "react";
import styles from "./AvbrytAvtale.module.scss";
import SletteModal from "../modal/SletteModal";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { useNavigate } from "react-router-dom";

interface Props {
  handleDelete: () => void;
  utkast: Utkast;
}

export function SlettUtkast({ handleDelete, utkast }: Props) {
  const mutation = useDeleteUtkast();
  const [error, setError] = useState("");
  const [utkastIdForSletting, setUtkastIdForSletting] = useState<null | string>(
    null,
  );
  const { refetch } = useMineUtkast(utkast.type);
  const navigate = useNavigate();

  const slettUtkast = () => {
    if (!utkast?.id) throw new Error("Fant ingen avtaleId");

    setUtkastIdForSletting(utkast.id);
    mutation.mutate(utkast.id, {
      onSuccess: async () => {
        setUtkastIdForSletting(null);
        handleDelete();
        navigate(-1);
        await refetch();
      },
      onError: () => {
        const error = mutation.error as ApiError;
        setError(error.body);
      },
    });
  };

  return (
    <>
      <div className={styles.warning_container}>
        <Button
          type="button"
          size="small"
          variant="danger"
          onClick={() => setUtkastIdForSletting(utkast.id)}
        >
          Slett utkast
        </Button>

        {error ? (
          <Alert variant="warning">
            <Heading spacing size="small" level="3">
              Klarte ikke slette avtale
            </Heading>
            {error}
          </Alert>
        ) : null}
      </div>

      <SletteModal
        modalOpen={!!utkastIdForSletting}
        onClose={() => setUtkastIdForSletting(null)}
        mutation={mutation}
        handleDelete={slettUtkast}
        headerText="Ønsker du å slette utkastet?"
        headerSubText="De siste endringene blir ikke lagret."
        headerTextError="Kan ikke slette utkastet."
      />
    </>
  );
}
