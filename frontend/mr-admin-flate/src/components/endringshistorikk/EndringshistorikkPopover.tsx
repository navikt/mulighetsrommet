import React, { ReactElement, useRef, useState } from "react";
import { ClockDashedIcon } from "@navikt/aksel-icons";
import { Button, Loader, Popover } from "@navikt/ds-react";
import styles from "./ViewEndringshistorikk.module.scss";
import { InlineErrorBoundary } from "mulighetsrommet-frontend-common";

export interface EndringshistorikkPopoverProps {
  children: ReactElement;
}

export function EndringshistorikkPopover({ children }: EndringshistorikkPopoverProps) {
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <Button
        ref={buttonRef}
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        variant="tertiary-neutral"
        type="button"
        size="small"
        title="Trykk for å se endringshistorikk"
        className={styles.endringshistorikk_knapp}
      >
        <ClockDashedIcon height={25} width={25} title="Endringshistorikk" />
      </Button>

      <Popover
        open={open}
        onClose={() => setOpen(false)}
        anchorEl={buttonRef.current}
        placement="bottom-end"
      >
        <Popover.Content style={{ maxHeight: "350px", overflowY: "auto" }}>
          <React.Suspense fallback={<Loader title="Laster endringshistorikk" />}>
            <InlineErrorBoundary>{open ? children : null}</InlineErrorBoundary>
          </React.Suspense>
        </Popover.Content>
      </Popover>
    </>
  );
}
