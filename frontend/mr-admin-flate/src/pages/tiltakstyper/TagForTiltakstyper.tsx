import { ApiError, Tiltakstype } from "mulighetsrommet-api-client";
import { useState } from "react";
import { SingleValue } from "react-select";
import { Alert } from "@navikt/ds-react";
import CreatableSelect from "react-select/creatable";
import { mulighetsrommetClient } from "../../api/clients";
import { useAlleTagsForTiltakstyper } from "../../api/tiltakstyper/useAlleTagsForTiltakstyper";
import FilterTag from "../../components/knapper/FilterTag";
import styles from "./TagForTiltakstyper.module.scss";

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
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const refetchData = async () => {
    await refetchTiltakstype();
    await refetchAlleTags();
  };

  const handleCreate = async (inputValue: string) => {
    if (!tiltakstype) return;

    setError("");
    setIsLoading(true);
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
      setIsLoading(false);
      await refetchData();
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 401) {
          setError("Du har ikke tilgang til å lagre tags");
        }
        setError("Du har ikke tilgang til å lagre tags");
      }
      setIsLoading(false);
    }
  };

  const handleChange = async (value: SingleValue<Option>) => {
    if (value?.value) {
      await handleCreate(value.value);
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
          isDisabled={isLoading}
          isLoading={isLoading}
          noOptionsMessage={TagTekster.ingenTagsEksisterer}
          onChange={handleChange}
          onCreateOption={handleCreate}
          formatCreateLabel={TagTekster.opprettTag}
          options={options}
        />
        {error ? (
          <Alert className={styles.mt_1} variant="error">
            {error}
          </Alert>
        ) : null}
      </div>
    </>
  );
}
