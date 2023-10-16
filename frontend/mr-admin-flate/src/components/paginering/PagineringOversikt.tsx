import { Heading, Select } from "@navikt/ds-react";
import styles from "./PagineringOversikt.module.scss";

interface Props {
  page: number;
  antall: number;
  maksAntall?: number;
  type: string;
  antallVises?: number;
  setAntallVises?: (value: number) => void;
}

const antallSize = [15, 50, 100, 250, 500, 1000];

export function PagineringsOversikt({
  page,
  antall,
  maksAntall = 0,
  type,
  antallVises = antall,
  setAntallVises,
}: Props) {
  return (
    <Heading level="1" size="xsmall" data-testid="antall-tiltak" className={styles.container}>
      {antall < 1 ? (
        <span>Viser 0 av 0 {type} </span>
      ) : (
        <span>
          Viser {(page - 1) * antallVises + 1}-{antall + (page - 1) * antallVises} av {maksAntall}{" "}
          {type}{" "}
        </span>
      )}

      {setAntallVises ? (
        <Select
          size="small"
          label="Velg antall"
          hideLabel
          name="size"
          value={antallVises}
          onChange={(e) => setAntallVises(Number.parseInt(e.currentTarget.value))}
        >
          {antallSize.map((ant) => (
            <option key={ant} value={ant}>
              {ant}
            </option>
          ))}
        </Select>
      ) : null}
    </Heading>
  );
}
