import React, { ReactElement, useRef, useState } from "react";
import { ClockDashedIcon } from "@navikt/aksel-icons";
import { Button, Loader, Popover } from "@navikt/ds-react";
import { ErrorBoundary } from "react-error-boundary";
import { ErrorFallback } from "../../main";

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
      >
        <ClockDashedIcon height={25} width={25} title="Endringshistorikk" />
      </Button>

      <Popover
        open={open}
        onClose={() => setOpen(false)}
        anchorEl={buttonRef.current}
        placement="bottom-end"
      >
        <Popover.Content>
          <React.Suspense fallback={<Loader title="Laster endringshistorikk" />}>
            <ErrorBoundary FallbackComponent={ErrorFallback}>
              {open ? children : null}
            </ErrorBoundary>
          </React.Suspense>
        </Popover.Content>
      </Popover>
    </>
  );
}
