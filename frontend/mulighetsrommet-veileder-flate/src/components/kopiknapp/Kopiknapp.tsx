import { FilesFillIcon, FilesIcon } from "@navikt/aksel-icons";
import { Button, Tooltip } from "@navikt/ds-react";
import React, { useEffect, useState } from "react";
import styles from "./Kopiknapp.module.scss";

interface KopiknappProps {
  kopitekst: string;
  dataTestId?: string;
}

const Kopiknapp = ({ kopitekst, dataTestId }: KopiknappProps) => {
  const [showTooltip, setShowTooltip] = useState(false);
  const [hover, setHover] = useState(false);

  const copyToClipboard = (kopitekst: string, e: React.MouseEvent) => {
    e.stopPropagation();
    return navigator.clipboard.writeText(kopitekst).finally(() => {
      setShowTooltip(true);
    });
  };

  useEffect(() => {
    let timeOutId = 0;
    if (showTooltip) {
      timeOutId = window.setTimeout(() => setShowTooltip(false), 1200);
    }
    return () => clearTimeout(timeOutId);
  }, [showTooltip]);

  return (
    <Tooltip
      placement="top"
      content="Kopiert"
      className={styles.kopiknapp_tooltip}
      open={showTooltip}
      role="tooltip"
    >
      <Button
        size="xsmall"
        variant="tertiary"
        className={styles.kopiknapp}
        onMouseEnter={() => setHover(true)}
        onMouseLeave={() => setHover(false)}
        onClick={(e) => {
          copyToClipboard(kopitekst, e);
        }}
        data-testid={dataTestId}
      >
        {hover ? <FilesFillIcon aria-label="Kopier" /> : <FilesIcon aria-label="Kopier" />}
      </Button>
    </Tooltip>
  );
};

export default Kopiknapp;
