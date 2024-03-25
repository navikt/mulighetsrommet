import { Button, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import styles from "./ArrangorKontaktpersonSkjema.module.scss";
import { useUpsertArrangorKontaktperson } from "../../api/arrangor/useUpsertArrangorKontaktperson";
import { validEmail } from "../../utils/Utils";
import { ArrangorKontaktperson as ArrangorKontaktperson } from "mulighetsrommet-api-client";
import { useDeleteArrangorKontaktperson } from "../../api/arrangor/useDeleteArrangorKontaktperson";
import { resolveErrorMessage } from "../../api/errors";
import { useHandleApiUpsertResponse } from "../../api/effects";

type ArrangorKontaktpersonErrors = Partial<Record<keyof ArrangorKontaktperson, string>>;

interface State {
  navn: string;
  epost: string;
  telefon: string;
  beskrivelse: string;
  errors: ArrangorKontaktpersonErrors;
}

interface VirksomhetKontaktpersonerProps {
  arrangorId: string;
  person?: ArrangorKontaktperson;
  onSubmit: () => void;
}

export const ArrangorKontaktpersonSkjema = (props: VirksomhetKontaktpersonerProps) => {
  const { arrangorId, person, onSubmit } = props;
  const putMutation = useUpsertArrangorKontaktperson(arrangorId);
  const deleteMutation = useDeleteArrangorKontaktperson();

  const [state, setState] = useState<State>({
    navn: person?.navn ?? "",
    telefon: person?.telefon ?? "",
    beskrivelse: person?.beskrivelse ?? "",
    epost: person?.epost ?? "",
    errors: {},
  });

  useEffect(() => {
    if (deleteMutation.isSuccess) {
      deleteMutation.reset();
      onSubmit();
      return;
    }
  }, [deleteMutation]);

  function deleteKontaktperson() {
    if (person) {
      deleteMutation.mutate({ arrangorId, kontaktpersonId: person.id });
    }
  }

  useHandleApiUpsertResponse(
    putMutation,
    () => {
      putMutation.reset();
      onSubmit();
    },
    (validation) => {
      const errors = validation.errors.reduce((errors: Record<string, string>, error) => {
        return { ...errors, [error.name]: error.message };
      }, {});
      setState({ ...state, errors });
    },
  );

  function opprettEllerLagreKontaktperson() {
    setState({
      ...state,
      errors: {
        navn: !state.navn ? "Navn må være satt" : undefined,
        epost: !validEmail(state.epost) ? "Epost må være gyldig" : undefined,
      },
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
        error={state.errors.navn}
        autoFocus
        onChange={(e) => {
          setState({
            ...state,
            navn: e.target.value,
            errors: { ...state.errors, navn: undefined },
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
            error={state.errors.epost}
            onChange={(e) => {
              setState({
                ...state,
                epost: e.target.value,
                errors: { ...state.errors, epost: undefined },
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
