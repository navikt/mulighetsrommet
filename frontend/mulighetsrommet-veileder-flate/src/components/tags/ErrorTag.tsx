import { BodyShort, Tag } from "@navikt/ds-react";
import { kebabCase } from "../../utils/Utils";
import styles from "./ErrorTag.module.scss";
import { XMarkOctagonFillIcon } from "@navikt/aksel-icons";
import React from "react";

interface Props {
  innhold: string;
  title: string;
  dataTestId: string;
}

export const ErrorTag = ({ innhold, title, dataTestId }: Props) => {
  return (
    <Tag variant="error" size="small" data-testid={`${kebabCase(dataTestId)}`} title={title}>
      <XMarkOctagonFillIcon className={styles.svg_error} />
      <BodyShort size="small" className={styles.innhold}>
        {innhold}
      </BodyShort>
    </Tag>
  );
};
