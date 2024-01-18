import { PlusIcon } from "@navikt/aksel-icons";
import { Button, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import styles from "./VirksomhetKontaktpersoner.module.scss";
import { usePutVirksomhetKontaktperson } from "../../api/virksomhet/usePutVirksomhetKontaktperson";
import { useFormContext } from "react-hook-form";
import { Laster } from "../laster/Laster";
import { validEmail } from "../../utils/Utils";
import { DeleteVirksomhetKontaktpersonModal } from "./DeleteVirksomhetKontaktpersonModal";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";

interface State {
  leggTil: boolean;
  rediger: boolean;
  selectedId?: string;
  navn?: string;
  epost?: string;
  telefon?: string;
  beskrivelse?: string;
  navnError?: string;
  epostError?: string;
}

interface VirksomhetKontaktpersonerProps {
  orgnr: string;
  formValueName: string;
  title: string;
}

export const VirksomhetKontaktpersoner = (props: VirksomhetKontaktpersonerProps) => {
  const { orgnr, formValueName, title } = props;
  const { register, watch, setValue } = useFormContext();
  const putMutation = usePutVirksomhetKontaktperson(orgnr);
  const {
    data: kontaktpersoner,
    isLoading: isLoadingKontaktpersoner,
    refetch,
  } = useVirksomhetKontaktpersoner(orgnr);

  const [deleteModalOpen, setDeleteModalOpen] = useState(false);

  const [state, setState] = useState<State>({
    leggTil: false,
    rediger: false,
    selectedId: watch(formValueName),
    navn: undefined,
    telefon: undefined,
    beskrivelse: undefined,
    epost: undefined,
    navnError: undefined,
    epostError: undefined,
  });

  useEffect(() => {
    setState({
      ...state,
      selectedId: watch(formValueName),
    });
  }, [watch(formValueName)]);

  useEffect(() => {
    if (putMutation.isSuccess) {
      setValue(formValueName, putMutation.data.id);
      setState({
        ...state,
        leggTil: false,
        rediger: false,
        selectedId: putMutation.data.id,
      });
      refetch();
      putMutation.reset();
    }
  }, [putMutation]);

  if (!kontaktpersoner || isLoadingKontaktpersoner) {
    return <Laster />;
  }

  const opprettEllerLagreKontaktperson = () => {
    setState({
      ...state,
      navnError: !state.navn ? "Navn må være satt" : undefined,
      epostError: !validEmail(state.epost) ? "Epost må være en gyldig epost adresse" : undefined,
    });
    if (!state.navn || !state.epost || !validEmail(state.epost)) {
      return;
    }

    putMutation.mutate({
      id: state.leggTil ? uuidv4() : state.selectedId!!,
      navn: state.navn,
      telefon: state.telefon || null,
      beskrivelse: state.beskrivelse || null,
      epost: state.epost,
    });
  };

  const valgtPerson = kontaktpersoner.find((person) => person.id === state.selectedId);

  return (
    <>
      <ControlledSokeSelect
        size="small"
        placeholder="Søk etter kontaktpersoner"
        onClearValue={() => setValue(formValueName, null)}
        label={title}
        {...register(formValueName)}
        onChange={(e) =>
          setState({
            ...state,
            selectedId: e.target.value as string,
          })
        }
        options={kontaktpersoner.map((person) => ({
          value: person.id,
          label: person.navn,
        }))}
      />
      {state.selectedId && !state.rediger && (
        <div className={styles.kontaktperson_info_container}>
          <label>{`Navn: ${valgtPerson?.navn ?? "Navn eksisterer ikke"}`}</label>
          <label>{`Telefon: ${valgtPerson?.telefon || "Telefonnummer eksisterer ikke"}`}</label>
          <label>{`E-post: ${valgtPerson?.epost ?? "E-post eksisterer ikke"}`}</label>
          {valgtPerson?.beskrivelse && <label>{`Beskrivelse: ${valgtPerson?.beskrivelse}`}</label>}
        </div>
      )}
      {!state.leggTil && !state.rediger && (
        <div className={styles.button_container}>
          {state.selectedId && (
            <Button
              className={styles.kontaktperson_button}
              type="button"
              size="small"
              variant="tertiary"
              onClick={() =>
                setState({
                  ...state,
                  navn: valgtPerson?.navn,
                  epost: valgtPerson?.epost,
                  telefon: valgtPerson?.telefon ?? undefined,
                  beskrivelse: valgtPerson?.beskrivelse ?? undefined,
                  rediger: true,
                })
              }
            >
              Rediger
            </Button>
          )}
          <Button
            className={styles.kontaktperson_button}
            size="small"
            type="button"
            onClick={() =>
              setState({
                ...state,
                leggTil: !state.leggTil,
                navn: undefined,
                epost: undefined,
                telefon: undefined,
                beskrivelse: undefined,
              })
            }
          >
            <PlusIcon aria-label="Opprett ny kontaktperson" /> eller opprett ny kontaktperson
          </Button>
        </div>
      )}
      {(state.leggTil || state.rediger) && (
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
            <div className={styles.button_container_left}>
              <Button size="small" type="button" onClick={opprettEllerLagreKontaktperson}>
                {state.leggTil ? "Opprett kontaktperson" : "Lagre"}
              </Button>
              {state.rediger && (
                <Button
                  size="small"
                  type="button"
                  variant="danger"
                  onClick={() => setDeleteModalOpen(true)}
                >
                  Slett
                </Button>
              )}
            </div>
            <Button
              size="small"
              variant="secondary"
              type="button"
              onClick={() =>
                setState({
                  ...state,
                  leggTil: false,
                  rediger: false,
                })
              }
            >
              Avbryt
            </Button>
          </div>
        </div>
      )}
      <DeleteVirksomhetKontaktpersonModal
        kontaktpersonId={state.selectedId}
        modalOpen={deleteModalOpen}
        onClose={() => {
          setDeleteModalOpen(false);
          setValue(formValueName, null);
          refetch();
          setState({
            ...state,
            selectedId: undefined,
            rediger: false,
          });
        }}
      />
    </>
  );
};
