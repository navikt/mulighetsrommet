import { Heading, Select } from "@navikt/ds-react";
import { PAGE_SIZE } from "../../constants";
import styles from "./PagineringOversikt.module.scss";

interface Props {
  page: number;
  antall: number;
  maksAntall?: number;
  type: string;
  size?: number;
  setSize?: (value: number) => void;
}

const antallSize = [15, 50, 100, 250, 500, 1000];

export function PagineringsOversikt({
  page,
  antall,
  maksAntall = 0,
  type,
  size,
  setSize,
}: Props) {
  if (antall === 0) return null;

  return (
    <Heading
      level="1"
      size="xsmall"
      data-testid="antall-tiltak"
      className={styles.container}
    >
      <span>
        Viser {(page - 1) * PAGE_SIZE + 1}-{antall + (page - 1) * PAGE_SIZE} av{" "}
        {maksAntall} {type}{" "}
      </span>
      {setSize ? (
        <Select
          size="small"
          label="Velg antall"
          hideLabel
          name="size"
          value={size}
          onChange={(e) => setSize(Number.parseInt(e.currentTarget.value))}
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
