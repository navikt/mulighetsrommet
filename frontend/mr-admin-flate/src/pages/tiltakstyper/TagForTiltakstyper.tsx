import { Alert } from "@navikt/ds-react";
import { ApiError, Tiltakstype } from "mulighetsrommet-api-client";
import { useReducer } from "react";
import { SingleValue } from "react-select";
import CreatableSelect from "react-select/creatable";
import { mulighetsrommetClient } from "../../api/clients";
import { useAlleTagsForTiltakstyper } from "../../api/tiltakstyper/useAlleTagsForTiltakstyper";
import FilterTag from "../../components/knapper/FilterTag";
import styles from "./TagForTiltakstyper.module.scss";
import { initialTagState, reducer } from "./TagReducer";

interface Option {
  readonly label: string;
  readonly value: string;
}

interface Props {
  tiltakstype: Tiltakstype;
  refetchTiltakstype: () => void;
}

const TagTekster = {
  alleredeValgt: (isDisabled: boolean, tag: string) =>
    isDisabled ? `${tag} er allerede valgt` : tag,
  placeholder: (tags: string[]) =>
    tags.length > 0 ? "Velg tags" : "Opprett ny tag",
  ingenTagsEksisterer: () => "Det finnes ingen tags foreløpig",
  opprettTag: (input: string) => `Opprett ny tag: "${input}"`,
};

export function TagForTiltakstyper({ tiltakstype, refetchTiltakstype }: Props) {
  const { data: tags = [], refetch: refetchAlleTags } =
    useAlleTagsForTiltakstyper();
  const [state, dispatch] = useReducer(reducer, initialTagState);

  const refetchData = async () => {
    await refetchTiltakstype();
    await refetchAlleTags();
  };

  const createTag = async (inputValue: string) => {
    if (!tiltakstype) return;

    dispatch({ type: "Opprett tag" });
    const eksisterendeTags = tiltakstype?.tags ?? [];
    const requestBody = Array.from([
      ...eksisterendeTags,
      inputValue.toLowerCase(),
    ]);
    try {
      await mulighetsrommetClient.tiltakstyper.lagreTagsForTiltakstype({
        id: tiltakstype.id,
        requestBody,
      });
      dispatch({ type: "Tag opprettet" });
      throw new Error("");
      // await refetchData();
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 401) {
          dispatch({
            type: "Sett feil",
            payload: "Du har ikke tilgang til å lagre tags",
          });
          return;
        }
      }
      dispatch({ type: "Sett feil", payload: "Klarte ikke lagre tags" });
    }
  };

  const handleChange = async (value: SingleValue<Option>) => {
    if (value?.value) {
      await createTag(value.value);
    }
  };

  const clearTag = async (tagSomSkalSlettes: string) => {
    if (!tiltakstype) return;

    const requestBody =
      tiltakstype?.tags?.filter((tag) => tag !== tagSomSkalSlettes) ?? [];
    await mulighetsrommetClient.tiltakstyper.lagreTagsForTiltakstype({
      id: tiltakstype.id,
      requestBody: Array.from(requestBody),
    });
    await refetchData();
  };

  const options = Array.from(tags ?? []).map((tag) => {
    const isDisabled = !!tiltakstype?.tags?.includes(tag);
    return {
      value: tag,
      label: TagTekster.alleredeValgt(isDisabled, tag),
      isDisabled,
    };
  });

  return (
    <>
      <div className={styles.tags}>
        <FilterTag
          options={
            tiltakstype?.tags?.map((tag) => ({ id: tag, tittel: tag })) ?? []
          }
          handleClick={clearTag}
        />
      </div>
      <div className={styles.create_tag}>
        <CreatableSelect
          placeholder={TagTekster.placeholder(tags)}
          isDisabled={state.isLoading}
          isLoading={state.isLoading}
          noOptionsMessage={TagTekster.ingenTagsEksisterer}
          onChange={handleChange}
          onCreateOption={createTag}
          formatCreateLabel={TagTekster.opprettTag}
          options={options}
        />
        {state.error ? (
          <Alert className={styles.mt_1} variant="error">
            {state.error}
          </Alert>
        ) : null}
      </div>
    </>
  );
}
