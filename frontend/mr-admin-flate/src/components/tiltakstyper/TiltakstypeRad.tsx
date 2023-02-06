import { BodyShort, Tag } from "@navikt/ds-react";
import { Tiltakstype } from "mulighetsrommet-api-client";
import { formaterDato, kalkulerStatusForTiltakstype } from "../../utils/Utils";
import FilterTag from "../knapper/FilterTag";
import styles from "../listeelementer/Listeelementer.module.scss";
import { ListeRad } from "../listeelementer/ListeRad";

interface Props {
  tiltakstype: Tiltakstype;
}

export function TiltakstypeRad({ tiltakstype }: Props) {
  const status = kalkulerStatusForTiltakstype(tiltakstype);
  return (
    <ListeRad
      linkTo={`/tiltakstyper/${tiltakstype.id}`}
      classname={styles.listerad_tiltakstype}
    >
      <div>
        <BodyShort size="medium">{tiltakstype.navn}</BodyShort>
        <div>
          <FilterTag
            options={
              tiltakstype.tags?.map((tag) => ({ id: tag, tittel: tag })) ?? []
            }
            skjulIkon
          />
        </div>
      </div>
      <BodyShort size="medium">
        <Tag
          variant={
            status === "Aktiv"
              ? "success"
              : status === "Planlagt"
              ? "info"
              : "neutral"
          }
        >
          {status}
        </Tag>
      </BodyShort>
      <BodyShort
        size="small"
        title={`Startdato ${formaterDato(tiltakstype.fraDato)}`}
      >
        {formaterDato(tiltakstype.fraDato)}
      </BodyShort>
      <BodyShort
        size="small"
        title={`Sluttdato ${formaterDato(tiltakstype.tilDato)}`}
      >
        {formaterDato(tiltakstype.tilDato)}
      </BodyShort>
    </ListeRad>
  );
}
