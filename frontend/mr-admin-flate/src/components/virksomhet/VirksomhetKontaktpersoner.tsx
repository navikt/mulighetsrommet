import { PlusIcon, XMarkIcon } from "@navikt/aksel-icons";
import { Button, Loader, TextField } from "@navikt/ds-react";
import classNames from "classnames";
import { useEffect, useState } from "react";
import { v4 as uuidv4 } from "uuid";
import { useVirksomhetKontaktpersoner } from "../../api/virksomhet/useVirksomhetKontaktpersoner";
import styles from "./VirksomhetKontaktpersoner.module.scss";
import { usePutVirksomhetKontaktperson } from "../../api/virksomhet/usePutVirksomhetKontaktperson";
import { SokeSelect } from "../skjema/SokeSelect";
import { VirksomhetKontaktperson } from "mulighetsrommet-api-client";

interface VirksomhetKontaktpersonerProps {
  orgnr: string;
  setValue: (e: any) => void;
}

export const VirksomhetKontaktpersoner = (
  props: VirksomhetKontaktpersonerProps
) => {
  const { orgnr, setValue, ...rest } = props;
  const mutation = usePutVirksomhetKontaktperson(orgnr);
  const { data: kontaktpersoner, isLoading: isLoadingKontaktpersoner, refetch } = useVirksomhetKontaktpersoner(orgnr);

  useEffect(() => {
    if (mutation.isSuccess) {
      setValue(mutation.data.id);
      setLeggTil(false)
      setNavn(undefined);
      setTelefon(undefined);
      setEpost(undefined);
      refetch();
      mutation.reset();
    }
  }, [mutation])

  const [leggTil, setLeggTil] = useState<boolean>(false);

  const [navn, setNavn] = useState<string | undefined>();
  const [navnError, setNavnError] = useState<string | undefined>();
  const [epost, setEpost] = useState<string | undefined>();
  const [telefon, setTelefon] = useState<string | undefined>();

  if (!kontaktpersoner || isLoadingKontaktpersoner) {
    return <Loader />
  }
  const opprettKontaktperson = () => {
    if (!navn) {
      setNavnError("Navn er påkrevd")
      return;
    }
    mutation.mutate({
      id: uuidv4(),
      navn,
      telefon,
      epost,
    });
  };

  const personLabel = (person: VirksomhetKontaktperson) => {
    let label = person.navn;
    if (person.epost) {
      label += `/${person.epost}`
    }
    if (person.telefon) {
      label += `/${person.telefon}`
    }
    return label;
  }

  return (
    <div className={styles.kontaktperson_container}>
      <label className={styles.kontaktperson_label} >
        <b>Kontaktperson hos leverandøren</b>
      </label>
      <SokeSelect
        size="small"
        placeholder="Søk etter kontaktpersoner"
        label={"Lagrede kontaktpersoner"}
        {...rest}
        options={kontaktpersoner.map((person) => ({
          value: person.id,
          label: `${personLabel(person)}`,
        }))}
      />
      <Button
        className={classNames(
          styles.kontaktperson_button,
          styles.kontaktperson_fjern_button
        )}
        type="button"
        onClick={() => { setLeggTil(!leggTil) }}
      >
        {leggTil ? <XMarkIcon /> : <><PlusIcon /> Legg til kontaktperson</>}
      </Button>
      {leggTil &&
        <div>
          <TextField
            size="small"
            label={"Navn"}
            error={navnError}
            onChange={(e) => {
              setNavn(e.target.value);
              setNavnError(undefined);
            }}
          />
          <div className={styles.kontaktperson_inputs}>
            <TextField
              size="small"
              label="Telefon"
              onChange={(e) => setTelefon(e.target.value)}
            />
            <TextField
              size="small"
              label="Epost"
              onChange={(e) => setEpost(e.target.value)}
            />
          </div>
          <div className={styles.button_container}>
            <Button
              size="small"
              variant="secondary"
              className={styles.button}
              type="button"
              onClick={opprettKontaktperson}
            >
              Lagre kontaktperson
            </Button>
          </div>
        </div>
      }
    </div>
  );
}