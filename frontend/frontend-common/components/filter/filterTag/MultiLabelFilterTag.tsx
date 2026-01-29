import { Tag } from "@navikt/ds-react";
import Ikonknapp from "../../ikonknapp/Ikonknapp";
import { XMarkIcon } from "@navikt/aksel-icons";
import styles from "./FilterTag.module.scss";
import { MouseEvent } from "react";

interface Props {
  labels: string[];
  onClose?: (e: MouseEvent) => void;
}

export function MultiLabelFilterTag({ labels, onClose }: Props) {
  if (labels.length === 0) {
    return null;
  }

  return (
    <Tag size="small" variant="info" className={styles.filtertag} title={labels.join(", ")}>
      {tagLabel(labels)}
      {onClose ? (
        <Ikonknapp
          className={styles.overstyrt_ikon_knapp}
          handleClick={onClose}
          ariaLabel="Lukke"
          icon={<XMarkIcon className={styles.ikon} aria-label="Lukke" />}
        />
      ) : null}
    </Tag>
  );
}

function tagLabel(labels: string[]) {
  const firstLabel = labels[0];
  if (labels.length > 1) {
    return `${firstLabel} +${labels.length - 1}`;
  }
  return firstLabel;
}
