import { Button } from "@navikt/ds-react";
import { ApiError, Avtalestatus } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";
import { useAvtale } from "../../api/avtaler/useAvtale";
import AvbrytAvtaleModal from "../modal/AvbrytAvtaleModal";

interface Props {
  onAvbryt: () => void;
}

export function AvbrytAvtaleKnapp({ onAvbryt }: Props) {
  const { data: avtale } = useAvtale();
  const mutation = useAvbrytAvtale();
  const [error, setError] = useState<ApiError | null>(null);
  const [avbrytModal, setAvbrytModal] = useState(false);

  useEffect(() => {
    if (mutation.isSuccess) {
      onAvbryt();
    }

    if (mutation.isError) {
      const error = mutation.error as ApiError;
      setError(error.body);
    }
  }, [mutation]);

  if (avtale?.avtalestatus === Avtalestatus.AVSLUTTET) {
    // Trenger ikke avbryt en avtale som allerede er avsluttet
    return null;
  }

  return (
    <>
      <Button
        type="button"
        variant="danger"
        onClick={() => setAvbrytModal(true)}
      >
        Avbryt avtalen
      </Button>

      <AvbrytAvtaleModal
        modalOpen={avbrytModal}
        onClose={() => setAvbrytModal(false)}
        avtale={avtale}
        error={error}
      />
    </>
  );
}
