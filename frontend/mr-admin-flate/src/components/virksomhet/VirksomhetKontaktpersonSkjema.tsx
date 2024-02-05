import { Button, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import styles from "./VirksomhetKontaktpersonSkjema.module.scss";
import { usePutVirksomhetKontaktperson } from "../../api/virksomhet/usePutVirksomhetKontaktperson";
import { validEmail } from "../../utils/Utils";
import { VirksomhetKontaktperson } from "mulighetsrommet-api-client";
import { useDeleteVirksomhetKontaktperson } from "../../api/virksomhet/useDeleteVirksomhetKontaktperson";
import { resolveErrorMessage } from "../../api/errors";

interface State {
  navn: string;
  epost: string;
  telefon: string;
  beskrivelse: string;
  navnError?: string;
  epostError?: string;
}

interface VirksomhetKontaktpersonerProps {
  orgnr: string;
  person?: VirksomhetKontaktperson;
  onSubmit: () => void;
}

export const VirksomhetKontaktpersonSkjema = (props: VirksomhetKontaktpersonerProps) => {
  const { orgnr, person, onSubmit } = props;
  const putMutation = usePutVirksomhetKontaktperson(orgnr);
  const deleteMutation = useDeleteVirksomhetKontaktperson();

  const [state, setState] = useState<State>({
    navn: person?.navn ?? "",
    telefon: person?.telefon ?? "",
    beskrivelse: person?.beskrivelse ?? "",
    epost: person?.epost ?? "",
    navnError: undefined,
    epostError: undefined,
  });

  useEffect(() => {
    if (putMutation.isSuccess) {
      putMutation.reset();
      onSubmit();
    }
  }, [putMutation]);

  useEffect(() => {
    if (deleteMutation.isSuccess) {
      deleteMutation.reset();
      onSubmit();
      return;
    }
  }, [deleteMutation]);

  function deleteKontaktperson() {
    if (person) {
      deleteMutation.mutate(person.id);
    }
  }

  function opprettEllerLagreKontaktperson() {
    setState({
      ...state,
      navnError: !state.navn ? "Navn må være satt" : undefined,
      epostError: !validEmail(state.epost) ? "Epost må være gyldig" : undefined,
    });
    if (!state.navn || !state.epost || !validEmail(state.epost)) {
      return;
    }

    putMutation.mutate({
      id: person?.id ?? uuidv4(),
      navn: state.navn,
      telefon: state.telefon || null,
      beskrivelse: state.beskrivelse || null,
      epost: state.epost,
    });
  }

  return (
    <div className={styles.input_container}>
      <TextField
        size="small"
        label={"Navn"}
        value={state.navn}
        error={state.navnError}
        onChange={(e) => {
          setState({
            ...state,
            navn: e.target.value,
            navnError: undefined,
          });
        }}
      />
      <div className={styles.telefonepost_input}>
        <div className={styles.telefon_input}>
          <TextField
            size="small"
            label="Telefon"
            value={state.telefon}
            onChange={(e) => {
              setState({
                ...state,
                telefon: e.target.value,
              });
            }}
          />
        </div>
        <div className={styles.epost_input}>
          <TextField
            size="small"
            label="Epost"
            value={state.epost}
            error={state.epostError}
            onChange={(e) => {
              setState({
                ...state,
                epost: e.target.value,
                epostError: undefined,
              });
            }}
          />
        </div>
      </div>
      <TextField
        size="small"
        label={"Beskrivelse"}
        placeholder="Unngå personopplysninger"
        maxLength={67}
        value={state.beskrivelse}
        onChange={(e) => {
          setState({
            ...state,
            beskrivelse: e.target.value,
          });
        }}
      />
      <div className={styles.button_container}>
        <Button size="small" type="button" onClick={opprettEllerLagreKontaktperson}>
          {person ? "Lagre" : "Opprett"}
        </Button>
        {person && (
          <Button size="small" type="button" variant="danger" onClick={deleteKontaktperson}>
            Slett
          </Button>
        )}
      </div>
      {deleteMutation.isError && (
        <div className={styles.error_msg} style={{}}>
          <b>• {resolveErrorMessage(deleteMutation.error)}</b>
        </div>
      )}
    </div>
  );
};
