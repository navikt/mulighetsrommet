import { Alert, Button, Heading, HelpText, Modal, Search } from "@navikt/ds-react";
import { useSetAvtaleForGjennomforing } from "@/api/gjennomforing/useSetAvtaleForGjennomforing";
import { useState } from "react";
import { AvtaleDto, GjennomforingDto } from "@mr/api-client";
import { GjennomforingList } from "../gjennomforing/GjennomforingList";
import { Link } from "react-router";

interface Props {
  avtale: AvtaleDto;
  modalOpen: boolean;
  onClose: () => void;
}

export function LeggTilGjennomforingModal({ avtale, modalOpen, onClose }: Props) {
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");

  const { mutate, isPending } = useSetAvtaleForGjennomforing();

  const clickCancel = () => {
    setSearch("");
    onClose();
  };

  const handleLeggTil = (gjennomforing: GjennomforingDto, avtaleId?: string) => {
    mutate(
      {
        gjennomforingId: gjennomforing.id,
        avtaleId,
      },
      {
        onError: () => {
          setError(`Klarte ikke koble gjennomføring til avtale`);
        },
      },
    );
  };

  return (
    <Modal
      open={modalOpen}
      onClose={clickCancel}
      style={{ maxHeight: "70rem" }}
      aria-label="modal"
      width="50rem"
    >
      <Modal.Header closeButton>
        <Heading size="medium">Legg til eller fjern gjennomføring fra avtalen</Heading>
      </Modal.Header>
      <Modal.Body style={{ display: "flex", flexDirection: "column", gap: "2rem" }}>
        <Search
          label="Søk på navn eller tiltaksnummer"
          variant="simple"
          hideLabel={false}
          autoFocus
          onChange={(search) => setSearch(search)}
          value={search}
        />

        {error ? <Alert variant="error">{error}</Alert> : null}

        {!search ? null : (
          <GjennomforingList
            filter={{
              search,
              tiltakstyper: [avtale.tiltakstype.id],
            }}
            action={(gjennomforing) =>
              !gjennomforing.avtaleId ? (
                <Button
                  size="small"
                  variant="tertiary"
                  disabled={isPending}
                  onClick={() => handleLeggTil(gjennomforing, avtale.id)}
                >
                  Legg til
                </Button>
              ) : gjennomforing.avtaleId === avtale.id ? (
                <Button
                  size="small"
                  variant="tertiary"
                  disabled={isPending}
                  onClick={() => handleLeggTil(gjennomforing, undefined)}
                >
                  Fjern
                </Button>
              ) : (
                <div style={{ margin: "0 auto" }}>
                  <HelpText title="Hvorfor har du ikke legg til eller fjern-knapp?">
                    Denne tiltaksgjennomføringen er allerede koblet til en annen avtale.
                    <div>
                      <Link to={`/avtaler/${gjennomforing.avtaleId}`}>Gå til avtalen</Link>
                    </div>
                  </HelpText>
                </div>
              )
            }
          />
        )}
      </Modal.Body>
    </Modal>
  );
}
