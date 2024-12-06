import { useDeleteArrangorKontaktperson } from "@/api/arrangor/useDeleteArrangorKontaktperson";
import { useUpsertArrangorKontaktperson } from "@/api/arrangor/useUpsertArrangorKontaktperson";
import { SkjemaInputContainer } from "@/components/skjema/SkjemaInputContainer";
import { validEmail } from "@/utils/Utils";
import { ApiError, ArrangorKontaktperson, ArrangorKontaktpersonAnsvar } from "@mr/api-client";
import { resolveErrorMessage } from "@mr/frontend-common/components/error-handling/errors";
import { isValidationError } from "@mr/frontend-common/utils/utils";
import { Button, HGrid, TextField, UNSAFE_Combobox } from "@navikt/ds-react";
import { useState } from "react";
import { v4 as uuidv4 } from "uuid";
import styles from "./ArrangorKontaktpersonSkjema.module.scss";
import { navnForAnsvar } from "./ArrangorKontaktpersonUtils";

type ArrangorKontaktpersonErrors = Partial<Record<keyof ArrangorKontaktperson, string>>;

interface State {
  navn: string;
  epost: string;
  telefon: string;
  beskrivelse: string;
  ansvarligFor: ArrangorKontaktpersonAnsvar[];
  errors: ArrangorKontaktpersonErrors;
}

interface VirksomhetKontaktpersonerProps {
  arrangorId: string;
  person?: ArrangorKontaktperson;
  onSubmit: () => void;
  onOpprettSuccess: (kontaktperson: ArrangorKontaktperson) => void;
}

export function ArrangorKontaktpersonSkjema({
  arrangorId,
  person,
  onSubmit,
  onOpprettSuccess,
}: VirksomhetKontaktpersonerProps) {
  const putMutation = useUpsertArrangorKontaktperson(arrangorId);
  const deleteMutation = useDeleteArrangorKontaktperson();

  const [state, setState] = useState<State>({
    navn: person?.navn ?? "",
    telefon: person?.telefon ?? "",
    beskrivelse: person?.beskrivelse ?? "",
    epost: person?.epost ?? "",
    ansvarligFor: person?.ansvarligFor ?? [],
    errors: {},
  });

  function deleteKontaktperson() {
    if (person) {
      deleteMutation.mutate(
        { arrangorId, kontaktpersonId: person.id },
        {
          onSuccess: () => {
            deleteMutation.reset();
            onSubmit();
          },
        },
      );
    }
  }

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

    putMutation.mutate(
      {
        id: person?.id ?? uuidv4(),
        navn: state.navn,
        telefon: state.telefon || null,
        beskrivelse: state.beskrivelse || null,
        epost: state.epost,
        ansvarligFor: state.ansvarligFor,
      },
      {
        onSuccess: (kontaktperson) => {
          onOpprettSuccess(kontaktperson);
          putMutation.reset();
          onSubmit();
        },
        onError: (error: ApiError) => {
          if (isValidationError(error)) {
            const errors = error.errors.reduce((errors: Record<string, string>, error) => {
              return { ...errors, [error.name]: error.message };
            }, {});
            setState({ ...state, errors });
          }
        },
      },
    );
  }

  return (
    <SkjemaInputContainer>
      <HGrid gap="2" columns={1}>
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
        <HGrid columns={2} gap="2">
          <div>
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
          <div>
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
        </HGrid>
        <UNSAFE_Combobox
          label="Hva er kontaktpersonen ansvarlig for?"
          size="small"
          isMultiSelect
          error={state.errors.ansvarligFor}
          selectedOptions={state.ansvarligFor.map((ansvar) => ({
            label: navnForAnsvar(ansvar),
            value: ansvar,
          }))}
          options={[
            { value: ArrangorKontaktpersonAnsvar.AVTALE, label: "Avtale" },
            {
              value: ArrangorKontaktpersonAnsvar.TILTAKSGJENNOMFORING,
              label: "Tiltaksgjennomføring",
            },
            { value: ArrangorKontaktpersonAnsvar.OKONOMI, label: "Økonomi" },
          ]}
          onToggleSelected={(option, isSelected) => {
            setState({
              ...state,
              ansvarligFor: isSelected
                ? [...state.ansvarligFor, option as ArrangorKontaktpersonAnsvar]
                : state.ansvarligFor.filter((o) => o !== option),
            });
          }}
        />
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
        <HGrid columns={2} gap="1">
          <Button size="small" type="button" onClick={opprettEllerLagreKontaktperson}>
            {person ? "Lagre" : "Opprett"}
          </Button>
          {person && (
            <Button size="small" type="button" variant="danger" onClick={deleteKontaktperson}>
              Slett
            </Button>
          )}
        </HGrid>
        {deleteMutation.isError && (
          <div className={styles.error_msg}>
            <b>• {resolveErrorMessage(deleteMutation.error)}</b>
          </div>
        )}
      </HGrid>
    </SkjemaInputContainer>
  );
}
