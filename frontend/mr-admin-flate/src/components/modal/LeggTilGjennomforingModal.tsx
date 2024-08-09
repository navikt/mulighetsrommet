import { Alert, Button, Heading, HelpText, Modal, Search } from "@navikt/ds-react";
import { useSetAvtaleForGjennomforing } from "@/api/tiltaksgjennomforing/useSetAvtaleForGjennomforing";
import { useState } from "react";
import { Avtale, Tiltaksgjennomforing } from "mulighetsrommet-api-client";
import { TiltaksgjennomforingerListe } from "../tiltaksgjennomforinger/TiltaksgjennomforingerListe";
import { Link } from "react-router-dom";
import { RedaksjoneltInnholdModalContainer } from "@/components/modal/RedaksjoneltInnholdModalContainer";
import { RedaksjoneltInnholdModalBody } from "@/components/modal/RedaksjoneltInnholdModalBody";

interface Props {
  avtale: Avtale;
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

  const handleLeggTil = (tiltaksgjennomforing: Tiltaksgjennomforing, avtaleId?: string) => {
    mutate(
      {
        gjennomforingId: tiltaksgjennomforing.id,
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
    <RedaksjoneltInnholdModalContainer modalOpen={modalOpen} onClose={clickCancel}>
      <Modal.Header closeButton>
        <Heading size="medium">Legg til eller fjern gjennomføring fra avtalen</Heading>
      </Modal.Header>

      <RedaksjoneltInnholdModalBody>
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
          <TiltaksgjennomforingerListe
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
      </RedaksjoneltInnholdModalBody>
    </RedaksjoneltInnholdModalContainer>
  );
}
