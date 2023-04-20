import { TextField } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { inferredSchema } from "../avtaler/opprett/OpprettAvtaleContainer";
import { CheckmarkIcon } from "@navikt/aksel-icons";
import { useEffect, useReducer } from "react";
import { mulighetsrommetClient } from "../../api/clients";
import { capitalizeEveryWord } from "../../utils/Utils";
import { reducer, initialState } from "../avtaler/opprett/virksomhetReducer";
import { Laster } from "../laster/Laster";
import { Avtale } from "mulighetsrommet-api-client";
import styles from "./VirksomhetInput.module.scss";

interface Props {
  avtale?: Avtale;
}

export function VirksomhetInput({ avtale }: Props) {
  const {
    register,
    formState: { errors },
    setError,
    clearErrors,
  } = useFormContext<inferredSchema>();

  useEffect(() => {
    if (avtale?.leverandor?.organisasjonsnummer?.length === 9) {
      sjekkOrgnr(avtale.leverandor.organisasjonsnummer);
    }
  }, [avtale?.leverandor]);

  const [virksomhetState, virksomhetDispatcher] = useReducer(
    reducer,
    initialState
  );

  const sjekkOrgnr = async (orgnr: string) => {
    virksomhetDispatcher({ type: "Reset" });
    if (orgnr.trim().length === 9) {
      virksomhetDispatcher({ type: "Hent data" });
      try {
        const response =
          await mulighetsrommetClient.hentVirksomhet.hentVirksomhetMedOrgnr({
            orgnr: orgnr.trim(),
          });

        virksomhetDispatcher({ type: "Data hentet", payload: response });
        clearErrors("leverandor");
      } catch (error) {
        virksomhetDispatcher({ type: "Reset" });
        setError("leverandor", {
          message: `Ingen leverandør med orgnummer ${orgnr}`,
        });
      }
    }
  };

  return (
    <div>
      <TextField
        error={errors.leverandor?.message}
        label={"Leverandør"}
        {...register("leverandor", {
          onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
            sjekkOrgnr(e.currentTarget.value),
        })}
        data-testid="leverandor-input"
      />
      <div className={styles.virksomhet}>
        {virksomhetState.status === "fetching" ? (
          <Laster tekst="Henter virksomhet" sentrert={false} />
        ) : null}
        {virksomhetState.status === "fetched" ? (
          <span
            data-testid="leverandor-validert-navn"
            className={styles.icon_text_align}
            aria-label={`Fant virksomhet for orgnr: ${virksomhetState.data?.organisasjonsnummer} med navn ${virksomhetState.data?.navn}`}
          >
            <CheckmarkIcon color="green" title="Fant virksomhet" />{" "}
            {capitalizeEveryWord(virksomhetState.data?.navn)}
          </span>
        ) : null}
      </div>
    </div>
  );
}
